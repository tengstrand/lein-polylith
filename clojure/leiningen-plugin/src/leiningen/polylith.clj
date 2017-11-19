(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]))

(defn ^:no-project-needed polylith
  {:help-arglists '([cmd/create-dependency-files!
                     cmd/print-dependencies])
   :subtasks [#'cmd/create-dependency-files!
              #'cmd/create-dependency-files!]}
  ([project]
   (cmd/help))
  ([project subtask & args]
   (case subtask
     "gen" (cmd/create-dependency-files!)
     "print" (cmd/print-dependencies)
     (cmd/task-not-found subtask))))
