(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]))

(defn ^:no-project-needed polylith
  {:help-arglists '([cmd/gen-deps
                     cmd/build-git
                     cmd/deps])
   :subtasks [#'cmd/gen-deps
              #'cmd/build-git
              #'cmd/deps]}
  ([project]
   (println "The Polylith architecture: https://github.com/tengstrand/polylith")
   (println)
   (println "  lein polylith a    where a is:")
   (println)
   (println "    deps           Prints all dependencies.")
   (println "    gen-deps       Generate dependency files")
   (println "    build-git"))
  ([project subtask & args]
   (case subtask
     "gen-deps" (cmd/gen-deps)
     "build-git" (cmd/build-git args)
     "deps" (cmd/deps)
     (cmd/task-not-found subtask))))
