(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]
            [leiningen.polylith.file :as file]))

(defn ^:no-project-needed polylith
  {:help-arglists '([cmd/changes
                     cmd/deps
                     cmd/diff
                     cmd/help
                     cmd/info
                     cmd/settings
                     cmd/tests])
   :subtasks [#'cmd/changes
              #'cmd/deps
              #'cmd/diff
              #'cmd/help
              #'cmd/info
              #'cmd/settings
              #'cmd/tests]}
  ([project]
   (cmd/help))
  ([project subtask & args]
   (let [settings (:polylith project)
         root-dir (or (:root-dir settings) (file/parent-path))
         ignore-tests (or (:ignore-tests settings) [])]
     (case subtask
       "changes" (cmd/changes root-dir args)
       "deps" (cmd/deps root-dir)
       "diff" (cmd/diff root-dir args)
       "help" (cmd/help)
       "info" (cmd/info root-dir args)
       "settings" (cmd/settings root-dir settings)
       "tests" (cmd/tests root-dir ignore-tests args)
       (cmd/task-not-found subtask)))))
