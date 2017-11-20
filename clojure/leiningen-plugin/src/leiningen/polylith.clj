(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]))

(defn ^:no-project-needed polylith
  {:help-arglists '([cmd/gen-deps
                     cmd/build-jenkins
                     cmd/deps])
   :subtasks [#'cmd/gen-deps
              #'cmd/build-jenkins
              #'cmd/deps]}
  ([project]
   (println "The Polylith architecture: https://github.com/tengstrand/polylith")
   (println)
   (println "  lein polylith gen-deps        Generate dependency files.")
   (println "  lein polylith build-jenkins   Jenkins stuff.")
   (println "  lein polylith deps            Prints all dependencies."))
  ([project subtask & args]
   (case subtask
     "gen-deps" (cmd/gen-deps)
     "build-jenkins" (cmd/build-jenkins project args)
     "deps" (cmd/deps)
     (cmd/task-not-found subtask))))
