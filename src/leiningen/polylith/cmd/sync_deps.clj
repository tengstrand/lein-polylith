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
    (if index (nth content (inc index))
              [])))

(defn index-of-lib [libs [lib]]
  (ffirst
    (filter #(= lib (-> % second first))
            (map-indexed vector libs))))

(defn updated-dev-lib [libs [lib interfaces-ns]]
  (if (= (first lib) interfaces-ns)
    libs
    (let [index (index-of-lib libs lib)]
      (if index
        libs
        (conj libs lib)))))

(defn updated-entity-lib [entity-libs dev-lib]
  (if-let [index (index-of-lib entity-libs dev-lib)]
    (assoc entity-libs index dev-lib)
    entity-libs))

(defn updated-dev-libs [dev-libs entity-libs interface-ns]
  (vec (sort-by first (reduce updated-dev-lib dev-libs
                              (map #(vector % interface-ns) entity-libs)))))

(defn updated-entity-libs [entity-libs dev-libs]
  (vec (sort-by first (reduce updated-entity-lib entity-libs dev-libs))))

(defn ->entity [file index]
  (shared/entity-src-dir-name (first (str/split (subs (str file) index) #"/"))))

(defn deps-index [content]
  (ffirst
    (filter #(= :dependencies (second %))
            (map-indexed vector content))))

(defn ->interfaces-ns [top-dir]
  (let [top-ns (str/replace top-dir "/" ".")
        namespace (if (str/blank? top-ns) nil top-ns)]
    (symbol namespace "interfaces")))

(defn entity-libs [dev-libs [ws-path entity components interface-ns]]
  (let [type (if (contains? (set components) entity) "components" "bases")
        libs (libraries (str ws-path "/" type "/" entity "/project.clj"))]
    (updated-dev-libs dev-libs libs interface-ns)))

(defn entities-libs [ws-path dev-libs entities components interface-ns]
  (reduce entity-libs dev-libs (map #(vector ws-path % components interface-ns) entities)))

(defn update-environment! [ws-path top-dir dev-project-path dev-libs environment all-components all-bases]
  (let [project-path (str ws-path "/" dev-project-path)
        path (str ws-path "/environments/" environment "/src/" top-dir)
        index (inc (count path))
        entities (set (map #(->entity % index) (file/source-files path)))
        components (filterv all-components entities)
        interfaces-ns (->interfaces-ns top-dir)
        libs (entities-libs ws-path dev-libs entities components interfaces-ns)
        content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))
        new-content (seq (assoc content index libs))]
    (when (not= content new-content)
      (println (str "  updated: " dev-project-path))
      (file/write-to-file project-path dev-project-path new-content))))

(defn update-environments [ws-path top-dir dev-project-path]
  (let [dev-libs (libraries (str ws-path "/" dev-project-path))
        components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)]
    (doseq [environment (shared/all-environments ws-path)]
      (update-environment! ws-path top-dir dev-project-path dev-libs environment components bases))))

(defn updated-content [project-path updated-libs]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))]
    (seq (assoc content index updated-libs))))

(defn sync-entities! [ws-path dev-project-path entities-name entities]
  (let [dev-libs (libraries dev-project-path)]
    (doseq [entity entities]
      (let [project-path (str entities-name "/" entity "/project.clj")
            full-project-path (str ws-path "/" project-path)
            entity-libs (libraries full-project-path)
            updated-libs (updated-entity-libs entity-libs dev-libs)]
        (when-not (= entity-libs updated-libs)
          (println (str "  updated: " project-path))
          (file/write-to-file full-project-path project-path
                              (updated-content full-project-path updated-libs)))))))

(defn updated-system-content [libs project-path]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))]
    (seq (assoc content index libs))))

(defn update-systems! [ws-path top-dir]
  (let [components (shared/all-components ws-path)]
    (doseq [system (shared/all-systems ws-path)]
      (let [system-path (str ws-path "/systems/" system)
            project-path (str system-path "/project.clj")
            path (str "systems/" system "/project.clj")
            src-path (str system-path "/src/" top-dir)
            entities (file/directory-names src-path)
            interfaces-ns (->interfaces-ns top-dir)
            libs (entities-libs ws-path [] entities components interfaces-ns)
            sys-libs (sort-by first (libraries project-path))
            content (seq (updated-system-content libs project-path))]
        (when (not= libs sys-libs)
          (println (str "  updated: " path))
          (file/write-to-file (str ws-path "/" path) path content))))))

(defn execute [ws-path top-dir]
  (let [dev-project-path "environments/development/project.clj"
        project-path (str ws-path "/" dev-project-path)]
    (update-environments ws-path top-dir dev-project-path)
    (sync-entities! ws-path project-path "components" (shared/all-components ws-path))
    (sync-entities! ws-path project-path "bases" (shared/all-bases ws-path))
    (update-systems! ws-path top-dir)))
