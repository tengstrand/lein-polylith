(ns leiningen.polylith.cmd.create.workspace
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]
            [leiningen.polylith.version :as v]))

(defn create [path name ws-ns top-dir clojure-version]
  (let [ws-path (str path "/" name)
        ws-name (if (str/blank? ws-ns) "" (str ws-ns "/"))
        interface-content [(str "(defproject " ws-name "interfaces \"1.0\"")
                           (str "  :description \"Component interfaces\"")
                           (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version) "]")
                           (str "  :aot :all)")]
        ws-content [(str "(defproject " ws-name "development \"1.0\"")
                    (str "  :description \"The workspace\"")
                    (str "  :plugins [[polylith/lein-polylith \"" v/version "\"]]")
                    (str "  :polylith {:vcs \"git\"")
                    (str "             :build-tool \"leiningen\"")
                    (str "             :top-ns \"" ws-ns "\"")
                    (str "             :top-dir \"" top-dir "\"")
                    (str "             :ignored-tests []")
                    (str "             :clojure-version \"1.9.0\"")
                    (str "             :example-hash1 \"2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1\"")
                    (str "             :example-hash2 \"58cd8b3106c942f372a40616fe9155c9d2efd122\"})")]
        dev-content [(str "(defproject " ws-name "development \"1.0\"")
                     (str "  :description \"The main development environment\"")
                     (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version) "])")]]
    (file/create-dir ws-path)
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
    (shared/create-src-dirs! ws-path "/interfaces/src" [top-dir])
    (shared/create-src-dirs! ws-path "/environments/development/src" [top-dir])
    (shared/create-src-dirs! ws-path "/environments/development/test" [top-dir])
    (file/create-dir (str ws-path "/bases"))
    (file/create-file (str ws-path "/interfaces/project.clj") interface-content)
    (file/create-file (str ws-path "/project.clj") ws-content)
    (file/create-file (str ws-path "/environments/development/project.clj") dev-content)
    (file/create-symlink (str ws-path "/environments/development/project-files/interfaces-project.clj") "../../../interfaces/project.clj")
    (file/create-symlink (str ws-path "/environments/development/project-files/workspace-project.clj") "../../../project.clj")
    (file/create-symlink (str ws-path "/environments/development/interfaces") "../../interfaces/src")))
