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
   (println "  lein polylith x    where x is:")
   (println)
   (println "    deps                 Prints all dependencies.")
   (println "    gen-deps             Generate dependency files")
   (println "    build-jenkins c s    c = curr-build-no")
   (println "                         s = last-success-build-no"))
  ([project subtask & args]
   (case subtask
     "gen-deps" (cmd/gen-deps)
     "build-jenkins" (cmd/build-jenkins project args)
     "deps" (cmd/deps)
     (cmd/task-not-found subtask))))
