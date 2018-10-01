(ns leiningen.polylith.cmd.sync.systems
  (:require [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.sync.environments :as environments]
            [leiningen.polylith.cmd.sync.shared :as shared-sync]))

(defn updated-system-content [libs project-path]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (shared-sync/deps-index content))]
    (seq (assoc content index libs))))

(defn update-systems-libs! [ws-path top-dir]
  (let [components (shared/all-components ws-path)]
    (doseq [system (shared/all-systems ws-path)]
      (let [system-path (str ws-path "/systems/" system)
            project-path (str system-path "/project.clj")
            path (str "systems/" system "/project.clj")
            src-path (str system-path "/src/" top-dir)
            entities (file/directory-names src-path)
            libs (environments/entities-libs ws-path [] entities components)
            sys-libs (sort-by first (shared/libraries project-path))
            content (seq (updated-system-content libs project-path))]
        (when (not= libs sys-libs)
          (println (str "updated: " path))
          (file/write-to-file (str ws-path "/" path) path content))))))
