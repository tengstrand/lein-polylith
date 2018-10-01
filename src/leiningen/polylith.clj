(ns leiningen.polylith
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.add :as add]
            [leiningen.polylith.cmd.build :as build]
            [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.create :as create]
            [leiningen.polylith.cmd.delete :as delete]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.cmd.diff :as diff]
             ;[leiningen.polylith.cmd.doc :as doc]]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.prompt :as prompt]
            [leiningen.polylith.cmd.remove :as remove]
            [leiningen.polylith.cmd.settings :as settings]
            [leiningen.polylith.cmd.success :as success]
            [leiningen.polylith.cmd.sync :as sync]
            [leiningen.polylith.cmd.test :as test]))

(defn create-ws? [subtask args]
  (and (= "create" subtask)
       (or
         (= "w" (first args))
         (= "workspace" (first args)))))

(defn ^:no-project-needed polylith
  "Helps you develop component based systems"
  ([_]
   (help/execute [] false))
  ([project command & cmd-args]
   (let [ws-path (:root project)
         settings (:polylith project)
         top-ns (:top-namespace settings "")
         github-url (:github-url settings "")
         top-dir (str/replace top-ns #"\." "/")
         clojure-version (:clojure-version settings "1.9.0")
         args (vec (filter #(not= "-exit" %) cmd-args))
         exit? (not (contains? (set cmd-args) "-exit"))]
     (if (nil? settings)
       (cond
         (= "help" command) (help/execute args false)
         (create-ws? command args) (create/execute ws-path top-dir top-ns clojure-version args)
         :else (println (str "The command must be executed from the workspace root directory. Only 'help' and 'create w' commands can be executed outside of a workspace.")))
       (try
         (case command
           "add" (add/execute ws-path top-dir args)
           "build" (build/execute ws-path top-dir args)
           "changes" (changes/execute ws-path top-dir args)
           "compile" (compile/execute ws-path top-dir args)
           "create" (create/execute ws-path top-dir top-ns clojure-version args)
           "delete" (delete/execute ws-path top-dir args)
           "deps" (deps/execute ws-path top-dir args)
           "diff" (diff/execute ws-path args)
           ;"doc" (doc/execute ws-path top-dir github-url args)
           "help" (help/execute args false)
           "info" (info/execute ws-path top-dir args)
           "prompt" (prompt/execute ws-path top-dir top-ns clojure-version settings github-url args)
           "remove" (remove/execute ws-path top-dir args)
           "settings" (settings/execute ws-path settings)
           "success" (success/execute ws-path args)
           "sync" (sync/execute ws-path top-dir)
           "test" (test/execute ws-path top-dir args)
           (println (str "Command '" command "' not found. Type 'lein polylith' for help.")))
         (catch IllegalStateException _
           (when exit?
             (System/exit 1))))))))
