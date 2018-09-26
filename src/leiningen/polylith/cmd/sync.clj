(ns leiningen.polylith.cmd.sync
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.sync.components :as components]
            [leiningen.polylith.cmd.sync.environments :as environments]
            [leiningen.polylith.cmd.sync.entities :as entities]
            [leiningen.polylith.cmd.sync.interfaces :as interfaces]
            [leiningen.polylith.cmd.sync.systems :as systems]))

(defn do-sync [ws-path top-dir project-path dev-project-path]
  (environments/update-environments ws-path top-dir dev-project-path)
  (entities/sync-entities! ws-path project-path "components" (shared/all-components ws-path))
  (entities/sync-entities! ws-path project-path "bases" (shared/all-bases ws-path))
  (systems/update-systems-libs! ws-path top-dir)
  (every? true?
    [(interfaces/sync-interfaces! ws-path top-dir)
     (components/add-missing-components-to-systems! ws-path top-dir)]))

(defn execute [ws-path top-dir]
  (let [dev-project-path "environments/development/project.clj"
        project-path (str ws-path "/" dev-project-path)]
    (do-sync ws-path top-dir project-path dev-project-path)))

(defn sync-all [ws-path top-dir action]
  (or (execute ws-path top-dir)
      (throw (Exception. ^String (str "Cannot " action ".")))))
