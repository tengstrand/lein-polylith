(ns leiningen.polylith.cmd.sync
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn index-of-lib [libs [lib]]
  (ffirst
    (filter #(= lib (-> % second first))
            (map-indexed vector libs))))

(defn updated-dev-lib [libs lib]
  (if (= "interfaces" (-> lib first name))
    libs
    (if (index-of-lib libs lib)
      libs
      (conj libs lib))))

(defn updated-entity-lib [entity-libs dev-lib]
  (if-let [index (index-of-lib entity-libs dev-lib)]
    (assoc entity-libs index dev-lib)
    entity-libs))

(defn updated-dev-libs [dev-libs entity-libs]
  (vec (sort-by first (reduce updated-dev-lib dev-libs entity-libs))))

(defn updated-entity-libs [entity-libs dev-libs]
  (vec (sort-by first (reduce updated-entity-lib entity-libs dev-libs))))

(defn ->entity [file index]
  (shared/entity-src-dir-name (first (str/split (subs (str file) index) #"/"))))

(defn deps-index [content]
  (ffirst
    (filter #(= :dependencies (second %))
            (map-indexed vector content))))

(defn entity-libs [dev-libs [ws-path entity components]]
  (let [type (if (contains? (set components) entity) "components" "bases")
        libs (shared/libraries (str ws-path "/" type "/" entity "/project.clj"))]
    (updated-dev-libs dev-libs libs)))

(defn entities-libs [ws-path dev-libs entities components]
  (reduce entity-libs dev-libs (map #(vector ws-path % components) entities)))

(defn update-environment! [ws-path top-dir dev-project-path dev-libs environment all-components all-bases]
  (let [project-path (str ws-path "/" dev-project-path)
        path (str ws-path "/environments/" environment "/src/" top-dir)
        index (if (empty? top-dir) (count path) (inc (count path)))
        entities (set (map #(->entity % index) (file/source-files path)))
        components (filterv all-components entities)
        libs (vec (sort-by #(-> % first str) (entities-libs ws-path dev-libs entities components)))
        content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))
        new-content (seq (assoc content index libs))]
    (when (not= content new-content)
      (println (str "  updated: " dev-project-path))
      (file/write-to-file project-path dev-project-path new-content))))

(defn update-environments [ws-path top-dir dev-project-path]
  (let [dev-libs (shared/libraries (str ws-path "/" dev-project-path))
        components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)]
    (doseq [environment (shared/all-environments ws-path)]
      (update-environment! ws-path top-dir dev-project-path dev-libs environment components bases))))

(defn updated-content [project-path updated-libs]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))]
    (seq (assoc content index updated-libs))))

(defn sync-entities! [ws-path dev-project-path entities-name entities]
  (let [dev-libs (shared/libraries dev-project-path)]
    (doseq [entity entities]
      (let [project-path (str entities-name "/" entity "/project.clj")
            full-project-path (str ws-path "/" project-path)
            entity-libs (shared/libraries full-project-path)
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
            libs (entities-libs ws-path [] entities components)
            sys-libs (sort-by first (shared/libraries project-path))
            content (seq (updated-system-content libs project-path))]
        (when (not= libs sys-libs)
          (println (str "  updated: " path))
          (file/write-to-file (str ws-path "/" path) path content))))))

(defn sync-cmd [ws-path top-dir project-path dev-project-path]
  (update-environments ws-path top-dir dev-project-path)
  (sync-entities! ws-path project-path "components" (shared/all-components ws-path))
  (sync-entities! ws-path project-path "bases" (shared/all-bases ws-path))
  (update-systems! ws-path top-dir))

(defn validate [arg]
  (condp = arg
    nil [false "Missing argument. Valid arguments are: 'all' or 'deps'."]
    "all" [true]
    "deps" [true]
    [false (str "Invalid argument '" arg "'. Valid arguments are: 'all' or 'deps'.")]))

(defn execute [ws-path top-dir [arg]]
  (let [[ok? message] (validate arg)
        dev-project-path "environments/development/project.clj"
        project-path (str ws-path "/" dev-project-path)]
    (if ok?
      (sync-cmd ws-path top-dir project-path dev-project-path)
      (println message))))
