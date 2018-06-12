(ns leiningen.polylith.cmd.sync-deps
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn libraries [path]
  (let [content (first (file/read-file path))
        index (ffirst
                (filter #(= :dependencies (second %))
                        (map-indexed vector content)))]
    (nth content (inc index))))

(defn ->entity [file index]
  (shared/entity-src-dir-name (first (str/split (subs (str file) index) #"/"))))

(defn ->add-lib [m [lib version]]
  (if (contains? m lib)
    m
    (assoc m lib version)))

(defn deps-index [content]
  (ffirst
    (filter #(= :dependencies (second %))
            (map-indexed vector content))))

(defn libs-except-interfaces [ws-path top-dir type entities]
  (let [top-ns (str/replace top-dir "/" ".")
        namespace (if (str/blank? top-ns) nil top-ns)
        interfaces (symbol namespace "interfaces")
        libs (map #(libraries (str ws-path "/" type "/" % "/project.clj")) entities)]
    (filterv #(not= interfaces (first %)) (map vec (partition 2 (flatten libs))))))

(defn update-environment! [ws-path top-dir dev-project-path dev-libs environment all-components all-bases]
  (let [project-path (str ws-path "/" dev-project-path)
        path (str ws-path "/environments/" environment "/src/" top-dir)
        index (inc (count path))
        entities (set (map #(->entity % index) (file/source-files path)))
        components (filterv all-components entities)
        bases (filterv all-bases entities)
        base-libs (libs-except-interfaces ws-path top-dir "bases" bases)
        component-libs (libs-except-interfaces ws-path top-dir "components" components)
        libs (concat base-libs component-libs)
        updated-libs (vec (seq (reduce ->add-lib (into (sorted-map) dev-libs) libs)))
        content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))
        new-content (seq (assoc content index updated-libs))]
    (when (not= content new-content)
      (println (str "  updated: " dev-project-path))
      (file/write-to-file project-path dev-project-path new-content))))

(defn update-environments [ws-path top-dir dev-project-path]
  (let [dev-libs (into {} (libraries (str ws-path "/" dev-project-path)))
        components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)]
    (doseq [environment (shared/all-environments ws-path)]
      (update-environment! ws-path top-dir dev-project-path dev-libs environment components bases))))

(defn merge-libs [dev-libs project-path]
  (let [comp-libs (libraries project-path)
        dev-key (fn [m [k v]]
                  (if (contains? dev-libs k)
                    (into m [[k (dev-libs k)]])
                    (into m [[k v]])))]
    (reduce dev-key {} comp-libs)))

(defn lib-versions-has-changed? [dev-project-path project-path]
  (let [dev-libs (into {} (libraries dev-project-path))
        libs (into {} (libraries project-path))
        shared-libs (set/intersection (set (map first libs))
                                      (set (map first dev-libs)))]
    (not
      (empty?
        (set/difference
          (set (filter #(contains? shared-libs (first %)) dev-libs))
          (set (filter #(contains? shared-libs (first %)) libs)))))))

(defn updated-content [dev-project-path project-path]
  (let [dev-libs (into {} (libraries dev-project-path))
        updated-libs (vec (seq (merge-libs dev-libs project-path)))
        content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))]
    (seq (assoc content index updated-libs))))

(defn sync-entities! [ws-path dev-project-path entities-name entities]
  (doseq [entity entities]
    (let [path (str entities-name "/" entity "/project.clj")
          target-path (str ws-path "/" path)]
      (when (lib-versions-has-changed? dev-project-path target-path)
        (println (str "  updated: " path))
        (file/write-to-file target-path path (updated-content dev-project-path target-path))))))

(defn entity-libs [ws-path type entities]
  (into (sorted-map)
        (filter #(not= "interfaces" (-> % first name))
                (mapcat #(libraries (str ws-path "/" type "/" % "/project.clj"))
                        entities))))

(defn updated-system-content [libs project-path]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))]
    (seq (assoc content index libs))))

(defn update-systems! [ws-path top-dir]
  (let [bases (shared/all-bases ws-path)
        components (shared/all-components ws-path)]
    (doseq [system (shared/all-systems ws-path)]
      (let [system-path (str ws-path "/systems/" system)
            project-path (str system-path "/project.clj")
            path (str "systems/" system "/project.clj")
            src-path (str system-path "/src/" top-dir)
            entities (file/directory-names src-path)
            base-libs (entity-libs ws-path "bases" (filter bases entities))
            comp-libs (entity-libs ws-path "components" (filter components entities))
            new-libs (sort (set (concat base-libs comp-libs)))
            sys-libs (sort (libraries project-path))
            content (seq (updated-system-content new-libs project-path))]
        (when (not= new-libs sys-libs)
          (println (str "  updated: " path))
          (file/write-to-file (str ws-path "/" path) path content))))))

(defn execute [ws-path top-dir]
  (let [dev-project-path "environments/development/project.clj"
        project-path (str ws-path "/" dev-project-path)]
    (update-environments ws-path top-dir dev-project-path)
    (sync-entities! ws-path project-path "components" (shared/all-components ws-path))
    (sync-entities! ws-path project-path "bases" (shared/all-bases ws-path))
    (update-systems! ws-path top-dir)))
