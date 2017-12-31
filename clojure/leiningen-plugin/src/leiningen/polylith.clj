(ns leiningen.polylith
  (:require [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.create :as create]
            [leiningen.polylith.cmd.delete :as delete]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.settings :as settings]
            [leiningen.polylith.cmd.test :as test]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]
            [leiningen.polylith.cmd.help :as help]))

(defn ^:no-project-needed polylith
  "Helps you write component based systems"
  ([project]
   (help/execute []))
  ([project subtask & args]
   (let [ws-path (file/parent-path (:root project))
         settings (:polylith project)
         ignore-tests (:ignore-tests settings [])
         top-ns (:top-ns settings)
         top-dir (:top-dir settings "")
         dev-dirs (:development-dirs settings ["development"])]
     (if (nil? ws-path)
       (cond
         (= "help" subtask) (help/execute args)
         (and (= "create" subtask)
              (= "w" (first args))) (create/execute ws-path top-dir top-ns dev-dirs args)
         :else (println "Polylith must be executed from the 'development' directory."))
       (case subtask
         "changes" (changes/execute ws-path args)
         "create" (create/execute ws-path top-dir top-ns dev-dirs args)
         "delete" (delete/execute ws-path top-dir top-ns dev-dirs args)
         "deps" (deps/execute ws-path args)
         "diff" (diff/execute ws-path args)
         "help" (help/execute args)
         "info" (info/execute ws-path args)
         "settings" (settings/execute ws-path settings)
         "test" (test/execute ws-path ignore-tests args)
         (do
           (println "Subtask" subtask "not found.")
           (help/help)))))))
