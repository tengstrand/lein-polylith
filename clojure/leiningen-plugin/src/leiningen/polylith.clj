(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]
            [leiningen.polylith.file :as file]))

(defn ^:no-project-needed polylith
  {:help-arglists '([cmd/deps
                     cmd/changed
                     cmd/help
                     cmd/info
                     cmd/settings])
   :subtasks [#'cmd/deps
              #'cmd/changed
              #'cmd/help
              #'cmd/info
              #'cmd/settings]}
  ([project]
   (cmd/help))
  ([project subtask & args]
   (let [polylith-settings (:polylith project)
         root-dir (or (:root-dir polylith-settings) (file/parent-path))]
     (case subtask
       "deps" (cmd/deps root-dir)
       "changed" (cmd/changed root-dir args)
       "help" (cmd/help)
       "info" (cmd/info root-dir args)
       "settings" (cmd/settings polylith-settings)
       (cmd/task-not-found subtask)))))
