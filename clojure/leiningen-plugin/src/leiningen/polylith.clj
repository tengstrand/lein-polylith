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
   (let [ws-dir (file/parent-path (:root project))
         settings (:polylith project)
         ignore-tests (:ignore-tests settings [])
         top-ns (:top-ns settings)
         top-dir (:top-dir settings "")
         dev-dirs (:development-dirs settings ["development"])]
     (if (nil? ws-dir)
       (cond
         (= "help" subtask) (cmd/help args)
         (and (= "create" subtask)
              (= "w" (first args))) (cmd/create ws-dir top-dir top-ns dev-dirs args)
         :else (help/not-executed-from-development))
       (case subtask
         "changes" (cmd/changes ws-dir args)
         "create" (cmd/create ws-dir top-dir top-ns dev-dirs args)
         "delete" (cmd/delete ws-dir dev-dirs args)
         "deps" (cmd/deps ws-dir args)
         "diff" (cmd/diff ws-dir args)
         "help" (cmd/help args)
         "info" (cmd/info ws-dir args)
         "settings" (cmd/settings ws-dir settings)
         "test" (cmd/test-cmd ws-dir ignore-tests args)
         (cmd/task-not-found subtask))))))
