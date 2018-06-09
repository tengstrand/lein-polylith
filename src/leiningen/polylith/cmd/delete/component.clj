(ns leiningen.polylith.cmd.delete.component
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn delete-env-interface [ws-path top-dir env interface]
  (file/delete-dir (str ws-path "/environments/" env "/src/" (shared/full-dir-name top-dir interface))))

(defn delete-env-component [ws-path top-dir env component]
  (let [root-dir (str ws-path "/environments/" env)]
    (file/delete-file! (str root-dir "/src/" (shared/full-dir-name top-dir component)))
    (file/delete-file! (str root-dir "/test/" (shared/full-dir-name top-dir component)))
    (file/delete-file! (str root-dir "/docs/" component "-Readme.md"))
    (file/delete-file! (str root-dir "/project-files/components/" component "-project.clj"))
    (file/delete-file! (str root-dir "/resources/" component))))

(defn delete-system-component [ws-path top-dir system component]
  (file/delete-file! (str ws-path "/systems/" system "/src/" (shared/full-dir-name top-dir component)))
  (file/delete-file! (str ws-path "/systems/" system "/resources/" component)))

(defn delete [ws-path top-dir component]
  (let [components (shared/all-components ws-path)
        interface (shared/interface-of ws-path top-dir component)
        interfaces (filter #(= interface (shared/interface-of ws-path top-dir %)) components)
        single-interface-usage? (and (not= interface component)
                                     (= 1 (count interfaces)))
        systems (shared/all-systems ws-path)
        environments (shared/all-environments ws-path)]
    (file/delete-dir (str ws-path "/components/" component))
    (when single-interface-usage?
      (file/delete-dir (str ws-path "/interfaces/src/" (shared/full-dir-name top-dir interface))))
    (doseq [system systems]
      (delete-system-component ws-path top-dir system component))
    (doseq [env environments]
      (delete-env-component ws-path top-dir env component)
      (delete-env-interface ws-path top-dir env interface))))
