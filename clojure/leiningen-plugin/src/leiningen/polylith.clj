(ns leiningen.polylith
  (:require [leiningen.polylith.cmd :as cmd]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]
            [leiningen.polylith.help :as help]))

(defn ^:no-project-needed polylith
  "Helps you write component based systems"
  ([project]
   (cmd/help []))
  ([project subtask & args]
   (let [ws-path (file/parent-path (:root project))
         settings (:polylith project)
         ignore-tests (:ignore-tests settings [])
         top-ns (:top-ns settings)
         top-dir (:top-dir settings "")
         dev-dirs (:development-dirs settings ["development"])]
     (if (nil? ws-path)
       (cond
         (= "help" subtask) (cmd/help args)
         (and (= "create" subtask)
              (= "w" (first args))) (cmd/create ws-path top-dir top-ns dev-dirs args)
         :else (help/not-executed-from-development))
       (case subtask
         "changes" (cmd/changes ws-path args)
         "create" (cmd/create ws-path top-dir top-ns dev-dirs args)
         "delete" (cmd/delete ws-path top-dir top-ns dev-dirs args)
         "deps" (cmd/deps ws-path args)
         "diff" (cmd/diff ws-path args)
         "help" (cmd/help args)
         "info" (cmd/info ws-path args)
         "settings" (cmd/settings ws-path settings)
         "test" (cmd/test-cmd ws-path ignore-tests args)
         (cmd/task-not-found subtask))))))
