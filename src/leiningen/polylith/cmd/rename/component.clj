(ns leiningen.polylith.cmd.rename.component
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn rename-dev [ws-path top-dir dev-dir from to]
  (let [root (str ws-path "/environments/" dev-dir)
        to-dir (shared/full-dir-name top-dir to)
        relative-parent-path (shared/relative-parent-path to-dir)
        to-path (str "../../../components/" to)
        relative-component-path (str relative-parent-path "components/" to)
        from-dir (shared/full-dir-name top-dir from)
        to-dir (shared/full-dir-name top-dir to)]
    (file/create-symlink (str root "/src/" to-dir)
                         (str relative-component-path "/src/" to-dir))
    (file/create-symlink (str root "/test/" to-dir)
                         (str relative-component-path "/test/" to-dir))
    (file/delete-file! (str root "/src/" from-dir))
    (file/delete-file! (str root "/test/" from-dir))
    (file/delete-file! (str root "/docs/" from "-Readme.md"))
    (file/create-symlink (str root "/docs/" to "-Readme.md")
                         (str to-path "/Readme.md"))
    (file/delete-file! (str root "/project-files/components/" from "-project.clj"))
    (file/create-symlink (str root "/project-files/components/" to "-project.clj")
                         (str "../" to-path "/project.clj"))))

(defn rename [ws-path top-dir from to]
  (let [root-dir (str ws-path "/components")
        dir (if (str/blank? top-dir) "" (str "/" top-dir))
        src-dir (str root-dir "/" to "/src" dir)
        test-dir (str root-dir "/" to "/test" dir)
        dev-dirs (file/directory-names (str ws-path "/environments"))
        src-dir-from (shared/src-dir-name from)
        src-dir-to (shared/src-dir-name to)]

    (file/rename! root-dir from to)
    (file/rename! src-dir src-dir-from src-dir-to)
    (file/rename! test-dir src-dir-from src-dir-to)
    (file/rename! (str root-dir "/" to "/resources/") from to)

    (doseq [dev-dir dev-dirs]
      (rename-dev ws-path top-dir dev-dir from to))))
