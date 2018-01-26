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
            [leiningen.polylith.cmd.build :as build]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]
            [leiningen.polylith.cmd.help :as help]))

(def example-hash1 "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1")
(def example-hash2 "58cd8b3106c942f372a40616fe9155c9d2efd122")

(defn workspace? [subtask args]
  (and (= "create" subtask)
       (= "w" (or (first args) ""))))

(defn ^:no-project-needed polylith
  "Helps you write component based systems"
  ([project]
   (let [settings (:polylith project)
         sha1 (:example-hash1 settings example-hash1)
         sha2 (:example-hash2 settings example-hash2)]
     (help/execute sha1 sha2 [])))
  ([project subtask & args]
   (let [ws-path (:root project)
         settings (:polylith project)
         ignored-tests (:ignored-tests settings [])
         top-ns (:top-ns settings)
         top-dir (:top-dir settings)
         dev-dirs (:development-dirs settings ["development"])
         clojure-version (:clojure-version settings "1.9.0")
         sha1 (:example-hash1 settings example-hash1)
         sha2 (:example-hash2 settings example-hash2)]
     (if (nil? ws-path)
       (cond
         (= "help" subtask) (help/execute example-hash1 example-hash2 args)
         (workspace? subtask args) (create/execute ws-path top-dir top-ns dev-dirs clojure-version args)
         :else (println "Polylith must be executed from the 'development' directory."))
       (case subtask
         "changes" (changes/execute ws-path top-dir args)
         "create" (create/execute ws-path top-dir top-ns dev-dirs clojure-version args)
         "delete" (delete/execute ws-path top-dir top-ns dev-dirs args)
         "deps" (deps/execute ws-path args)
         "diff" (diff/execute ws-path args)
         "help" (help/execute sha1 sha2 args)
         "info" (info/execute ws-path top-dir args)
         "settings" (settings/execute ws-path settings)
         "test" (test/execute ws-path ignored-tests sha1 sha2 args)
         "build" (build/execute ws-path top-dir args)
         (println (str "Subtask '" subtask "' not found. Type 'lein polylith' for help.")))))))
