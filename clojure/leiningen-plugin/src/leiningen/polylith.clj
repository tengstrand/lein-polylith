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
   (let [root-dir (file/parent-path (:root project))
         settings (:polylith project)
         ignore-tests (:ignore-tests settings [])
         top-ns (:top-ns settings)
         top-dir (:top-dir settings "")
         dev-dirs (:development-dirs settings ["development"])]
     (if (nil? root-dir)
       (cond
         (= "help" subtask) (cmd/help args)
         (and (= "create" subtask)
              (= "w" (first args))) (cmd/create root-dir top-ns dev-dirs args)
         :else (help/not-executed-from-development))
       (case subtask
         "changes" (cmd/changes root-dir args)
         "create" (cmd/create root-dir top-ns dev-dirs args)
         "delete" (cmd/delete root-dir dev-dirs args)
         "deps" (cmd/deps root-dir args)
         "diff" (cmd/diff root-dir args)
         "help" (cmd/help args)
         "info" (cmd/info root-dir args)
         "settings" (cmd/settings root-dir settings)
         "tests" (cmd/tests root-dir ignore-tests args)
         (cmd/task-not-found subtask))))))
