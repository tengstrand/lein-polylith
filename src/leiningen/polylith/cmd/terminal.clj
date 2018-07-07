(ns leiningen.polylith.cmd.terminal
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.add :as add]
            [leiningen.polylith.cmd.build :as build]
            [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.create :as create]
            [leiningen.polylith.cmd.delete :as delete]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.remove :as remove]
            [leiningen.polylith.cmd.settings :as settings]
            [leiningen.polylith.cmd.success :as success]
            [leiningen.polylith.cmd.sync-deps :as sync]
            [leiningen.polylith.cmd.test :as test]))

(defn terminal-cmd []
  (println "You can't start a new terminal from a running terminal."))

(defn execute-cmd [ws-path top-dir top-ns clojure-version settings [command & args]]
  (case command
    "" (comment)
    "add" (add/execute ws-path top-dir args)
    "build" (build/execute ws-path top-dir args)
    "changes" (changes/execute ws-path top-dir args)
    "compile" (compile/execute ws-path top-dir args)
    "create" (create/execute ws-path top-dir top-ns clojure-version args)
    "delete" (delete/execute ws-path top-dir args)
    "deps" (deps/execute ws-path top-dir args)
    "diff" (diff/execute ws-path args)
    "help" (help/execute args)
    "info" (info/execute ws-path top-dir args)
    "remove" (remove/execute ws-path top-dir args)
    "settings" (settings/execute ws-path settings)
    "success" (success/execute ws-path args)
    "sync-deps" (sync/execute ws-path top-dir)
    "terminal" (terminal-cmd)
    "test" (test/execute ws-path top-dir args)
    (println (str "Command '" command "' not found. Type 'help' for help."))))

(defn execute [ws-path top-dir top-ns clojure-version settings args]
  (let [ws (last (str/split ws-path #"/"))]
    (print (str ws "$> "))
    (flush)
    (let [expr (read-line)]
      (when-not (or (= "exit" expr)
                    (= "quit" expr))
        (execute-cmd ws-path top-dir top-ns clojure-version settings (str/split expr #" "))
        (recur ws-path top-dir top-ns clojure-version settings args)))))
