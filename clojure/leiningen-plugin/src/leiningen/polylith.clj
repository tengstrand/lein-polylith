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
   (cmd/help))
  ([project subtask & args]
   (case subtask
     "gen-deps" (cmd/gen-deps)
     "build-jenkins" (cmd/build-jenkins project args)
     "deps" (cmd/deps)
     (cmd/task-not-found subtask))))
