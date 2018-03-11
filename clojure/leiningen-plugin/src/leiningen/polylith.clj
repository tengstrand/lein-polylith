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

(defn create-ws? [subtask args]
  (and (= "create" subtask)
       (or
         (= "w" (first args))
         (= "workspace" (first args)))))

(defn ^:no-project-needed polylith
  "Helps you develop component based systems"
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
         clojure-version (:clojure-version settings "1.9.0")
         clojure-spec-version (:clojure-spec-version settings "0.1.143")
         sha1 (:example-hash1 settings example-hash1)
         sha2 (:example-hash2 settings example-hash2)]
     (if (nil? settings)
       (cond
         (= "help" subtask) (help/execute example-hash1 example-hash2 args)
         (create-ws? subtask args) (create/execute ws-path top-dir top-ns clojure-version clojure-spec-version args)
         :else (println (str "The command must be executed from the workspace root directory.")))
       (case subtask
         "changes" (changes/execute ws-path top-dir args)
         "create" (create/execute ws-path top-dir top-ns clojure-version clojure-spec-version args)
         "delete" (delete/execute ws-path top-dir top-ns args)
         "deps" (deps/execute ws-path args)
         "diff" (diff/execute ws-path args)
         "help" (help/execute sha1 sha2 args)
         "info" (info/execute ws-path top-dir args)
         "settings" (settings/execute ws-path settings)
         "test" (test/execute ws-path ignored-tests sha1 sha2 args)
         "build" (build/execute ws-path top-dir args)
         (println (str "Subtask '" subtask "' not found. Type 'lein polylith' for help.")))))))
