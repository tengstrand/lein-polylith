(ns leiningen.polylith.cmd.create.workspace
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]
            [leiningen.polylith.version :as v]))

(defn create [path name ws-ns top-dir clojure-version]
  (let [ws-path (str path "/" name)
        ws-name (if (str/blank? ws-ns) "" (str ws-ns "/"))
        local-time-content ["{:last-successful-build 0}"]
        interface-content [(str "(defproject " ws-name "interfaces \"1.0\"")
                           (str "  :description \"Component interfaces\"")
                           (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version) "]")
                           (str "  :aot :all)")]
        ws-content [(str "(defproject " ws-name "development \"1.0\"")
                    (str "  :description \"A Polylith workspace.\"")
                    (str "  :plugins [[polylith/lein-polylith \"" v/version "\"]]")
                    (str "  :polylith {:top-namespace \"" ws-ns "\"")
                    (str "             :clojure-version \"1.9.0\"})")]
        gitignore-content ["**/target"
                           "**/pom.xml"
                           "**/.idea"
                           "*.iml"
                           ".nrepl-port"
                           ".lein-env"
                           "crash.log"
                           ".polylith/time.local.edn"]
        dev-content [(str "(defproject " ws-name "development \"1.0\"")
                     (str "  :description \"The main development environment\"")
                     (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version) "])")]]
    (file/create-dir ws-path)
    (file/create-dir (str ws-path "/.polylith"))
    (file/create-dir (str ws-path "/interfaces"))
    (file/create-dir (str ws-path "/systems"))
    (file/create-dir (str ws-path "/components"))
    (file/create-dir (str ws-path "/environments"))
    (file/create-dir (str ws-path "/environments/development"))
    (file/create-dir (str ws-path "/environments/development/docs"))
    (file/create-dir (str ws-path "/environments/development/project-files"))
    (file/create-dir (str ws-path "/environments/development/project-files/bases"))
    (file/create-dir (str ws-path "/environments/development/project-files/components"))
    (file/create-dir (str ws-path "/environments/development/project-files/systems"))
    (file/create-dir (str ws-path "/environments/development/resources"))
    (file/create-file (str ws-path "/environments/development/resources/.keep") [""])
    (shared/create-src-dirs! ws-path "/interfaces/src" [top-dir])
    (shared/create-src-dirs! ws-path "/environments/development/src" [top-dir])
    (shared/create-src-dirs! ws-path "/environments/development/test" [top-dir])
    (file/create-dir (str ws-path "/bases"))
    (file/create-file (str ws-path "/.polylith/local.time") local-time-content)
    (file/create-file (str ws-path "/interfaces/project.clj") interface-content)
    (file/create-file (str ws-path "/project.clj") ws-content)
    (file/create-file (str ws-path "/.gitignore") gitignore-content)
    (file/copy-resource-file "Readme.md" (str ws-path "/Readme.md"))
    (file/copy-resource-file "logo.png" (str ws-path "/logo.png"))
    (file/create-file (str ws-path "/environments/development/project.clj") dev-content)
    (file/create-symlink (str ws-path "/environments/development/project-files/interfaces-project.clj") "../../../interfaces/project.clj")
    (file/create-symlink (str ws-path "/environments/development/project-files/workspace-project.clj") "../../../project.clj")
    (file/create-symlink (str ws-path "/environments/development/interfaces") "../../interfaces/src")))
