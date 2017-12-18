(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]
            [leiningen.polylith.file :as file]))

(defn polylith
  "Helps you write component based systems"
  ([project]
   (cmd/help []))
  ([project subtask & args]
   (let [settings (:polylith project)
         root-dir (or (:root-dir settings) (file/parent-path))
         ignore-tests (or (:ignore-tests settings) [])]
     (case subtask
       "changes" (cmd/changes root-dir args)
       "delete" (cmd/delete root-dir args)
       "deps" (cmd/deps root-dir)
       "diff" (cmd/diff root-dir args)
       "help" (cmd/help args)
       "info" (cmd/info root-dir args)
       "new" (cmd/new-cmd root-dir args)
       "settings" (cmd/settings root-dir settings)
       "tests" (cmd/tests root-dir ignore-tests args)
       (cmd/task-not-found subtask)))))
