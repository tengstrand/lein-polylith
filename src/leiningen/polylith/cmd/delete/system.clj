(ns leiningen.polylith.cmd.delete.system
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.delete.base :as delete-base]))

(defn delete-from-environment [system ns-dir dev-path]
  (file/delete-dir (str dev-path "/docs/" system "-readme.md"))
  (file/delete-dir (str dev-path "/project-files/systems/" system "-project.clj"))
  (file/delete-dir (str dev-path "/src/" ns-dir))
  (file/delete-dir (str dev-path "/test/" ns-dir)))

(defn delete [ws-path top-dir system base]
  (let [system-dir (str ws-path "/systems/" system)
        dev-dirs (file/directory-names (str ws-path "/environments"))
        env-path (str ws-path "/environments/")
        ns-dir (shared/full-dir-name top-dir system)]
    (when base
      (delete-base/delete ws-path top-dir base))
    (file/delete-dir system-dir)
    (doseq [dev-dir dev-dirs]
      (delete-from-environment system ns-dir (str env-path dev-dir)))))
