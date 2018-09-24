(ns leiningen.polylith.cmd.sync.entities
  (:require [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.sync.shared :as shared-env]))

(defn updated-entity-lib [entity-libs dev-lib]
  (if-let [index (shared-env/index-of-lib entity-libs dev-lib)]
    (assoc entity-libs index dev-lib)
    entity-libs))

(defn updated-entity-libs [entity-libs dev-libs]
  (vec (sort-by first (reduce updated-entity-lib entity-libs dev-libs))))

(defn updated-content [project-path updated-libs]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (shared-env/deps-index content))]
    (seq (assoc content index updated-libs))))

(defn sync-entities! [ws-path dev-project-path entities-name entities]
  (let [dev-libs (shared/libraries dev-project-path)]
    (doseq [entity entities]
      (let [project-path (str entities-name "/" entity "/project.clj")
            full-project-path (str ws-path "/" project-path)
            entity-libs (shared/libraries full-project-path)
            updated-libs (updated-entity-libs entity-libs dev-libs)]
        (when-not (= entity-libs updated-libs)
          (println (str "updated: " project-path))
          (file/write-to-file full-project-path project-path
                              (updated-content full-project-path updated-libs)))))))
