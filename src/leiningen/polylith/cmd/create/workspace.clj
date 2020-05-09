(ns leiningen.polylith.cmd.create.workspace
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.version :as v]
            [clojure.java.io :as io]))

(defn readme-content [name]
  ["<img src=\"images/logo.png\" width=\"30%\" alt=\"Polylith\" id=\"logo\">"
   ""
   "Polylith documentation can be found here:"
   ""
   "- The [high-level documentation](https://polylith.gitbook.io/polylith)"
   "- The [Polylith Tool documentation](https://github.com/tengstrand/lein-polylith)"
   "- The [RealWorld example app documentation](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)"
   ""
   "You can also get in touch with the Polylith Team via our [forum](https://polylith.freeflarum.com) or on [Slack](https://clojurians.slack.com/archives/C013B7MQHJQ)."
   ""
   (str "<h1>" name "</h1>")
   ""
   "<p>Add your workspace documentation here...</p>"])

(defn create [path name ws-ns top-dir clojure-version skip-git?]
  (let [ws-path (str path "/" name)
        ws-name (if (str/blank? ws-ns) "" (str ws-ns "/"))
        local-time-content ["{:last-success 0}"]
        interface-content [(str "(defproject " ws-name "interfaces \"1.0\"")
                           (str "  :description \"Component interfaces\"")
                           (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version) "]")
                           (str "  :aot :all)")]
        ws-content [(str "(defproject " ws-name name " \"1.0\"")
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
                           ".polylith/time.edn"
                           ".polylith/git.edn"]
        dev-content [(str "(defproject " ws-name "development \"1.0\"")
                     (str "  :description \"The main development environment.\"")
                     (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version) "])")]]
        ;style-content (-> "templates/style.css" io/resource slurp)]
    (file/create-dir ws-path)
    (file/create-dir (str ws-path "/.polylith"))
    ;(file/create-dir (str ws-path "/doc"))
    ;(file/create-file (str ws-path "/doc/style.css") [style-content])
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
    (file/create-file (str ws-path "/.polylith/time.edn") local-time-content)
    (file/create-file (str ws-path "/interfaces/project.clj") interface-content)
    (file/create-file (str ws-path "/project.clj") ws-content)
    (file/create-file (str ws-path "/.gitignore") gitignore-content)
    (file/copy-resource-file! "readme.md" (str ws-path "/readme.md"))
    (file/create-file (str ws-path "/readme.md") (readme-content name))
    (file/create-dir (str ws-path "/images"))
    (file/copy-resource-file! "images/logo.png" (str ws-path "/images/logo.png"))
    (file/create-file (str ws-path "/environments/development/project.clj") dev-content)
    (file/create-symlink (str ws-path "/environments/development/project-files/interfaces-project.clj") "../../../interfaces/project.clj")
    (file/create-symlink (str ws-path "/environments/development/project-files/workspace-project.clj") "../../../project.clj")
    (file/create-symlink (str ws-path "/environments/development/interfaces") "../../interfaces/src")
    (when-not skip-git?
      (try
        (shared/sh "git" "init" :dir ws-path)
        (shared/sh "git" "add" "." :dir ws-path)
        (shared/sh "git" "commit" "-m" "Initial commit." :dir ws-path)
        (catch Exception _
          (println "Cannot create a git repository while creating workspace.
                    You can create a git repository manually for your workspace."))))))
