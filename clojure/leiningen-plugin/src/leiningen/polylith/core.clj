(ns leiningen.polylith.core
  (:require [clojure.pprint :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.java.shell :as shell]
            [leiningen.polylith.file :as file]))

(defn str->component [name]
  (symbol (str/replace name #"_" "-")))

(defn ns-components [component-paths]
  (let [component (-> component-paths ffirst str->component)
        path->ns (fn [path] (-> path file/read-file first second))
        namespaces (map #(-> % second path->ns) component-paths)]
    (map #(vector % component) namespaces)))

(defn api-ns->component [ws-path]
  (into {}
        (reduce into []
                (map ns-components
                     (partition-by first (file/paths-in-dir (str ws-path "/apis/src")))))))

(defn- ->imports
  ([imports]
   (->imports imports []))
  ([imports result]
   (when (sequential? imports)
     (if (= :require (first imports))
       (conj result (rest imports))
       (filter (comp not nil?)
               (map ->imports imports))))))

(defn imports [content api->component]
  (let [requires (ffirst (->imports (first content)))
        ns-imports (map (juxt last first)
                        (filter #(= :as (second %)) requires))]
    (filter #(api->component (second %)) ns-imports)))

(defn component? [content alias->ns]
  (and (list? content)
       (-> content first sequential? not)
       (contains? alias->ns (some-> content first namespace symbol))))

(defn replace-ns [function alias->ns]
  (let [fn-name (name function)
        fn-ns-name (name (alias->ns (-> function namespace symbol)))]
    (symbol fn-ns-name fn-name)))

(defn file-dependencies
  ([filename api->component]
   (let [content (file/read-file filename)
         alias->ns (into {} (imports content api->component))
         functions (flatten (file-dependencies alias->ns content []))]
     (set (map #(replace-ns % alias->ns) functions))))
  ([alias->ns content result]
   (when (sequential? content)
     (if (component? content alias->ns)
       (conj result (first content))
       (filter (comp not nil?)
               (map #(file-dependencies alias->ns % result) content))))))

(defn component-dependencies [component-paths api->component]
  (let [component (-> component-paths ffirst symbol)
        files (map second component-paths)
        dependencies (sort (into #{} (mapcat #(file-dependencies % api->component) files)))]
    [component (vec dependencies)]))

(defn all-dependencies [ws-path]
  (let [development-dir (str ws-path "/development/src")
        api->component (api-ns->component ws-path)
        all-paths (partition-by first (file/paths-in-dir development-dir))]
    (into (sorted-map) (map #(component-dependencies % api->component) all-paths))))

(defn changed-dirs [dir file-paths]
  (let [f #(and (str/starts-with? % (str dir "/"))
                (> (count (str/split % #"/")) 2))]
    (vec (sort (set (map #(second (str/split % #"/"))
                         (filter f file-paths)))))))

(defn changed-system? [ws-path path changed-systems]
  (let [systems-path (str ws-path "/systems")
        system? (str/starts-with? path systems-path)
        changed? (and
                   system?
                   (let [system (second (str/split (subs path (count systems-path)) #"/"))]
                     (contains? (set changed-systems) system)))]
    {:system?  system?
     :changed? changed?}))

(defn changed-component? [ws-path path changed-components]
  (let [components-path (str ws-path "/components")
        component? (str/starts-with? path components-path)
        changed? (and
                   component?
                   (let [component (second (str/split (subs path (count components-path)) #"/"))]
                     (contains? (set changed-components) component)))]
    {:component? component?
     :changed?   changed?}))

(defn changed? [ws-path file changed-systems changed-components]
  (let [path (file/file-path->real-path file)
        changed-system (changed-system? ws-path path changed-systems)
        changed-component (changed-component? ws-path path changed-components)]
    {:name     (file/path->dir-name path)
     :type     (cond
                 (:system? changed-system) "-> system"
                 (:component? changed-component) "-> component"
                 :else "?")
     :changed? (cond
                 (:system? changed-system) (:changed? changed-system)
                 (:component? changed-component) (:changed? changed-component)
                 :else false)}))

(defn build-links [ws-path system changed-systems changed-components]
  (mapv #(changed? ws-path % changed-systems changed-components)
        (file/directories (str ws-path "/builds/" system "/src"))))

(defn build-info [ws-path builds changed-systems changed-components]
  (into {} (mapv (juxt identity #(build-links ws-path % changed-systems changed-components)) builds)))

(defn any-changes? [builds-info system]
  (or (some true? (map :changed? (builds-info system))) false))

(defn system-or-component-changed? [builds-info changed-builds]
  (let [system-changes (map (juxt identity #(any-changes? builds-info %)) (keys builds-info))]
    (map (juxt first #(or (last %) (contains? changed-builds (first %)))) system-changes)))

(defn diff [ws-path last-success-sha1 current-sha1]
  (let [diff (:out (shell/sh "git" "diff" "--name-only" last-success-sha1 current-sha1 :dir ws-path))]
    (str/split diff #"\n")))

(defn info
  ([ws-path]
   (info ws-path []))
  ([ws-path last-success-sha1 current-sha1]
   (info ws-path (diff ws-path last-success-sha1 current-sha1)))
  ([ws-path paths]
   (let [apis (set (file/directory-names (str ws-path "/apis/src")))
         components (set (file/directory-names (str ws-path "/components")))
         systems (set (file/directory-names (str ws-path "/systems")))
         builds (file/directory-names (str ws-path "/builds"))
         ;; make sure we only report changes that currently exist
         changed-apis (set (filter systems (set (changed-dirs "apis" paths))))
         changed-components (set (filter components (changed-dirs "components" paths)))
         changed-systems (set (filter systems (set (changed-dirs "systems" paths))))
         changed-builds-dir (set (filter systems (changed-dirs "builds" paths)))
         builds-info (build-info ws-path builds changed-systems changed-components)
         changed-builds (mapv first (filter second (system-or-component-changed? builds-info (set changed-builds-dir))))]
     {:apis               (-> apis sort vec)
      :builds             (-> builds sort vec)
      :components         (-> components sort vec)
      :systems            (-> systems sort vec)
      :diff               paths
      :changed-apis       changed-apis
      :changed-builds     changed-builds
      :changed-components changed-components
      :changed-systems    changed-systems
      :changed-builds-dir changed-builds-dir
      :builds-info        builds-info})))

(defn changes [ws-path cmd last-success-sha1 current-sha1]
  (let [{:keys [changed-apis
                changed-builds
                changed-systems
                changed-components]} (info ws-path last-success-sha1 current-sha1)]
    (condp = cmd
      "a" changed-apis
      "b" changed-builds
      "s" changed-systems
      "c" changed-components
      [])))

(defn delete [ws-path top-dir dev-dirs name]
  (let [top-name (if (zero? (count top-dir)) name (str top-dir "/" name))]
    (file/delete-dir (str ws-path "/apis/src/" top-name))
    (file/delete-dir (str ws-path "/components/" name))
    (doseq [dir dev-dirs]
      (file/delete-file (str ws-path "/" dir "/project-files/" name "-project.clj"))
      (file/delete-file (str ws-path "/" dir "/resources/" name))
      (file/delete-file (str ws-path "/" dir "/src/" top-name))
      (file/delete-file (str ws-path "/" dir "/test/" top-name))
      (file/delete-file (str ws-path "/" dir "/test-int/" top-name)))))

(defn path->ns [path]
  (second (first (file/read-file path))))

(defn system->tests [ws-path system test-dir dir]
  (let [paths (map second (file/paths-in-dir (str ws-path "/" dir "/" system "/" test-dir)))]
    (map path->ns paths)))

(defn tests-or-empty [tests? ws-path dir test-dir changed-systems]
  (if tests?
    (mapcat #(system->tests ws-path % test-dir dir) changed-systems)
    []))

(defn test-cmd
  ([ws-path [tests? integration-tests?]]
   (let [changed-systems (file/directory-names (str ws-path "/systems"))
         changed-components (file/directory-names (str ws-path "/components"))]
     (test-cmd ws-path [tests? integration-tests?] changed-systems changed-components)))
  ([ws-path [tests? integration-tests?] [last-success-sha1 current-sha1]]
   (let [{:keys [changed-systems
                 changed-components]} (info ws-path last-success-sha1 current-sha1)]
     (test-cmd ws-path [tests? integration-tests?] changed-systems changed-components)))
  ([ws-path [tests? integration-tests?] changed-systems changed-components]
   (let [system-tests (tests-or-empty tests? ws-path "systems" "test" changed-systems)
         system-itests (tests-or-empty integration-tests? ws-path "systems" "test-int" changed-systems)
         component-tests (tests-or-empty tests? ws-path "components" "test" changed-components)
         component-itests (tests-or-empty integration-tests? ws-path "components" "test-int" changed-components)]
     (vec (sort (map str (concat system-tests system-itests component-tests component-itests)))))))

(defn show-tests [tests single-line-statment?]
  (if single-line-statment?
    (if (empty? tests)
      (println "echo 'Nothing changed - no tests executed'")
      (println (str "lein test " (str/join " " tests))))
    (doseq [test tests]
      (println " " test))))

(defn run-tests [tests sing-line-statement?]
  (if (zero? (count tests))
    (println "Nothing to test")
    (do
      (println "Starts execution of" (count tests) "tests:")
      (show-tests tests sing-line-statement?)
      (apply shell/sh (concat ["lein" "test"] tests)))))
