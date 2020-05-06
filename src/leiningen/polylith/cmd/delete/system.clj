(ns leiningen.polylith.cmd.delete.system
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn delete-from-environment [system base system-ns-dir base-ns-dir dev-path]
  (file/delete-dir (str dev-path "/docs/" base "-readme.md"))
  (file/delete-dir (str dev-path "/docs/" system "-readme.md"))
  (file/delete-dir (str dev-path "/project-files/bases/" base "-project.clj"))
  (file/delete-dir (str dev-path "/project-files/systems/" system "-project.clj"))
  (file/delete-dir (str dev-path "/resources/" base))
  (file/delete-dir (str dev-path "/src/" system-ns-dir))
  (file/delete-dir (str dev-path "/src/" base-ns-dir)))

(defn delete [[ws-path top-dir system base]]
  (let [base-dir (str ws-path "/bases/" (shared/full-dir-name top-dir base))
        system-dir (str ws-path "/systems/" system)
        dev-dirs (file/directory-names (str ws-path "/environments"))
        env-path (str ws-path "/environments/")
        base-ns-dir (shared/full-dir-name top-dir base)
        system-ns-dir (shared/full-dir-name top-dir system)]
    (file/delete-dir base-dir)
    (file/delete-dir system-dir)
    (doseq [dev-dir dev-dirs]
      (delete-from-environment system base system-ns-dir base-ns-dir (str env-path dev-dir)))))

;$ rm -rf bases/base1
;$ rm -rf systems/system1
;$ rm -rf environments/development/docs/base1-readme.md
;$ rm -rf environments/development/docs/system1-readme.md
;$ rm -rf environments/development/project-files/bases/base1-project.clj
;$ rm -rf environments/development/project-files/systems/system1-project.clj
;$ rm -rf environments/development/resources/base1
;$ rm -rf environments/development/src/com/example/base1
;$ rm -rf environments/development/test/com/example/base1

(+ 1 2)