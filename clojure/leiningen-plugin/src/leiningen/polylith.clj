(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]))

(defn- print-missing-root-dir []
  (println "Root directory must be set in the project.clj file, e.g.:")
  (println "  {:polylith {:root-dir \"/Users/joakimtengstrand/projects/myproject\""))

(defn ^:no-project-needed polylith
  {:help-arglists '([cmd/components
                     cmd/deps
                     cmd/gen-deps
                     cmd/git-changes
                     cmd/help
                     cmd/project-settings])
   :subtasks [#'cmd/components
              #'cmd/gen-deps
              #'cmd/git-changes
              #'cmd/help
              #'cmd/deps
              #'cmd/project-settings]}
  ([project]
   (cmd/help))
  ([project subtask & args]
   (let [settings (:polylith project)
         root-dir (:root-dir settings)]
     (if (nil? root-dir)
       (print-missing-root-dir)
       (case subtask
         "components" (cmd/components root-dir)
         "deps" (cmd/deps)
         "gen-deps" (cmd/gen-deps)
         "git-changes" (cmd/git-changes root-dir args)
         "help" (cmd/help)
         "settings" (cmd/project-settings settings)
         (cmd/task-not-found subtask))))))
