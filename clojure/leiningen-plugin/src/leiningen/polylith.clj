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

(def example-sha1 "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1")
(def example-sha2 "58cd8b3106c942f372a40616fe9155c9d2efd122")

(defn ^:no-project-needed polylith
  "Helps you write component based systems"
  ([project]
   (let [settings (:polylith project)
         sha1 (:example-sha1 settings example-sha1)
         sha2 (:example-sha2 settings example-sha2)]
     (help/execute sha1 sha2 [])))
  ([project subtask & args]
   (let [ws-path (file/parent-path (:root project))
         settings (:polylith project)
         ignored-tests (:ignored-tests settings [])
         top-ns (:top-ns settings)
         top-dir (:top-dir settings)
         dev-dirs (:development-dirs settings ["development"])
         sha1 (:example-sha1 settings example-sha1)
         sha2 (:example-sha2 settings example-sha2)]
     (if (nil? ws-path)
       (cond
         (= "help" subtask) (help/execute example-sha1 example-sha2 args)
         (and (= "create" subtask)
              (= "w" (first args))) (create/execute ws-path top-dir top-ns dev-dirs args)
         :else (println "Polylith must be executed from the 'development' directory."))
       (case subtask
         "changes" (changes/execute ws-path args)
         "create" (create/execute ws-path top-dir top-ns dev-dirs args)
         "delete" (delete/execute ws-path top-dir top-ns dev-dirs args)
         "deps" (deps/execute ws-path args)
         "diff" (diff/execute ws-path args)
         "help" (help/execute sha1 sha2 args)
         "info" (info/execute ws-path args)
         "settings" (settings/execute ws-path settings)
         "test" (test/execute ws-path ignored-tests sha1 sha2 args)
         (do
           (println "Subtask" subtask "not found.")
           (help/help sha1 sha2)))))))
