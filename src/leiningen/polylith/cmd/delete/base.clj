(ns leiningen.polylith.cmd.delete.base
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn delete-from-environment [base ns-dir dev-path]
  (file/delete-dir (str dev-path "/docs/" base "-readme.md"))
  (file/delete-dir (str dev-path "/project-files/bases/" base "-project.clj"))
  (file/delete-dir (str dev-path "/resources/" base))
  (file/delete-dir (str dev-path "/src/" ns-dir))
  (file/delete-dir (str dev-path "/test/" ns-dir)))

(defn delete [ws-path top-dir base]
  (let [base-dir (str ws-path "/bases/" base)
        base-ns-dir (shared/full-dir-name top-dir base)
        dev-dirs (file/directory-names (str ws-path "/environments"))
        system-dirs (file/directory-names (str ws-path "/systems"))
        env-path (str ws-path "/environments/")
        ns-dir (shared/full-dir-name top-dir base)]
    (file/delete-dir base-dir)
    (doseq [sys-dir system-dirs]
      (file/delete-file! (str ws-path "/systems/" sys-dir "/src/" base-ns-dir))
      (file/delete-file! (str ws-path "/systems/" sys-dir "/resources/" base)))
    (doseq [dev-dir dev-dirs]
      (delete-from-environment base ns-dir (str env-path dev-dir)))))
