(ns leiningen.polylith.core
  (:require [clojure.pprint :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]
            [clojure.java.shell :as shell]))

(defn str->component [name]
  (symbol (str/replace name #"_" "-")))

(defn ns-components [component-paths]
  (let [component (-> component-paths ffirst str->component)
        path->ns (fn [path] (-> path file/read-file first second))
        namespaces (map #(-> % second path->ns) component-paths)]
    (map #(vector % component) namespaces)))

(defn api-ns->component []
  (into {}
        (reduce into []
                (map ns-components
                     (partition-by first (file/paths-in-dir "apis"))))))

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

(defn all-dependencies [root-dir]
  (let [api->component (api-ns->component)
        all-paths (partition-by first (file/paths-in-dir (str root-dir "/src")))]
    (into (sorted-map) (map #(component-dependencies % api->component) all-paths))))

(defn matching-dir? [path dir]
  (str/starts-with? path (str dir "/")))

(defn dirs [dir file-paths]
  (let [f #(and (str/starts-with? % (str dir "/"))
                (> (count (str/split % #"/")) 2))]
    (vec (sort (set (map #(second (str/split % #"/"))
                         (filter f file-paths)))))))

(defn components [root-dir]
  (file/directory-names (str root-dir "/components")))

(defn systems [root-dir]
  (file/directory-names (str root-dir "/systems")))

(defn bcomponents [root-dir system]
  (filterv #(not (= system %))
           (file/directory-names (str root-dir (str "/builds/systems/" system "/src/")))))

(defn changed-system? [root-dir path changed-systems]
  (let [systems-path (str root-dir "/systems")
        system? (str/starts-with? path systems-path)
        changed? (and
                   system?
                   (let [system (second (str/split (subs path (count systems-path)) #"/"))]
                     (contains? (set changed-systems) system)))]
    {:system? system?
     :changed? changed?}))

(defn changed-component? [root-dir path changed-components]
  (let [components-path (str root-dir "/components")
        component? (str/starts-with? path components-path)
        changed? (and
                   component?
                   (let [component (second (str/split (subs path (count components-path)) #"/"))]
                     (contains? (set changed-components) component)))]
    {:component? component?
     :changed? changed?}))

(defn changed? [root-dir file changed-systems changed-components]
  (let [path (file/file-path->real-path file)
        changed-system (changed-system? root-dir path changed-systems)
        changed-component (changed-component? root-dir path changed-components)]
    {:name (file/path->dir-name path)
     :type (cond
             (:system? changed-system) "-> system"
             (:component? changed-component) "-> component"
             :else "?")
     :changed? (cond
                 (:system? changed-system) (:changed? changed-system)
                 (:component? changed-component) (:changed? changed-component)
                 :else false)}))

(defn build-links [root-dir system changed-systems changed-components]
  (mapv #(changed? root-dir % changed-systems changed-components)
        (file/directories (str root-dir "/builds/" system "/src"))))

(defn build-info [root-dir builds changed-systems changed-components]
  (into {} (mapv (juxt identity #(build-links root-dir % changed-systems changed-components)) builds)))

(defn any-changes? [builds-info system]
  (or (some true? (map :changed? (builds-info system))) false))

(defn system-or-component-changed? [builds-info changed-builds]
  (let [system-changes (map (juxt identity #(any-changes? builds-info %)) (keys builds-info))]
    (map (juxt first #(or (last %) (contains? changed-builds (first %)))) system-changes)))

(defn info
  ([root-dir]
   (info root-dir []))
  ([root-dir last-success-sha1 current-sha1]
   (let [diff (:out (shell/sh "git" "diff" "--name-only" last-success-sha1 current-sha1 :dir root-dir))
         paths (str/split diff #"\n")]
     (info root-dir paths)))
  ([root-dir paths]
   (let [components (set (file/directory-names (str root-dir "/components")))
         systems (set (file/directory-names (str root-dir "/systems")))
         builds (file/directory-names (str root-dir "/builds"))
         ;; make sure we only report changes that currently exist
         changed-systems (set (filter systems (set (dirs "systems" paths))))
         changed-components (set (filter components (dirs "components" paths)))
         changed-builds-dir (set (filter systems (dirs "builds" paths)))
         builds-info (build-info root-dir builds changed-systems changed-components)
         changed-builds (mapv first (filter second (system-or-component-changed? builds-info (set changed-builds-dir))))]
     {:components (-> components sort vec)
      :systems (-> systems sort vec)
      :builds (-> builds sort vec)
      :diff paths
      :changed-components changed-components
      :changed-systems changed-systems
      :changed-builds-dir changed-builds-dir
      :changed-builds changed-builds
      :builds-info builds-info})))

(defn changed [root-dir cmd last-success-sha1 current-sha1]
  (let [{:keys [changed-builds
                changed-systems
                changed-components]} (info root-dir last-success-sha1 current-sha1)]
    (condp = cmd
      "b" changed-builds
      "s" changed-systems
      "c" changed-components
      [])))

(info "/Users/joakimtengstrand/IdeaProjects/project-unicorn"
      "d2930779686ecc893ca913762c364bb7f934c4e8"
      "07f0eb56768601bf199c52d0f2b4835b5902f247")

(info "/Users/joakimtengstrand/IdeaProjects/project-unicorn")
