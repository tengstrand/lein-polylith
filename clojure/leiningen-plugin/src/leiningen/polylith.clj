(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]))

(defn- print-missing-root-dir []
  (println "Root directory must be set in the project.clj file, e.g.:")
  (println "  {:polylith {:root-dir \"/Users/joakimtengstrand/projects/myproject\""))

(defn ^:no-project-needed polylith
  {:help-arglists '([cmd/bcomponents
                     cmd/bsystems
                     cmd/components
                     cmd/deps
                     cmd/gitdiff
                     cmd/help
                     cmd/info
                     cmd/project-settings
                     cmd/systems])
   :subtasks [#'cmd/bcomponents
              #'cmd/bsystems
              #'cmd/components
              #'cmd/deps
              #'cmd/gitdiff
              #'cmd/help
              #'cmd/info
              #'cmd/project-settings
              #'cmd/systems]}
  ([project]
   (cmd/help))
  ([project subtask & args]
   (let [settings (:polylith project)
         root-dir (:root-dir settings)]
     (if (nil? root-dir)
       (print-missing-root-dir)
       (case subtask
         "bsystems" (cmd/bsystems root-dir)
         "bcomponents" (cmd/bcomponents root-dir args)
         "components" (cmd/components root-dir)
         "deps" (cmd/deps root-dir)
         "gitdiff" (cmd/gitdiff root-dir args)
         "help" (cmd/help)
         "info" (cmd/info root-dir)
         "settings" (cmd/project-settings settings)
         "systems" (cmd/systems root-dir)
         (cmd/task-not-found subtask))))))
