(ns leiningen.polylith.cmd.prompt
  (:require [clojure.string :as str]
            [clojure.stacktrace :as stacktrace]
            [leiningen.polylith.cmd.add :as add]
            [leiningen.polylith.cmd.build :as build]
            [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.create :as create]
            [leiningen.polylith.cmd.delete :as delete]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.cmd.diff :as diff]
    ;[leiningen.polylith.cmd.doc :as doc]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.remove :as remove]
            [leiningen.polylith.cmd.settings :as settings]
            [leiningen.polylith.cmd.success :as success]
            [leiningen.polylith.cmd.sync :as sync]
            [leiningen.polylith.cmd.test :as test]
            [leiningen.polylith.file :as file]))

(defn prompt-cmd []
  (println "You can't start a new prompt from a running prompt."))

(def current-dir (atom ""))

(defn path [ws-path dir]
  (if (str/blank? @current-dir)
    (str ws-path "/" dir)
    (str ws-path "/" @current-dir "/" dir)))

(defn ->back [dir]
  (if-let [index (str/last-index-of dir "/")]
    (subs dir 0 index)
    ""))

(defn ->dir [dir]
  (if (str/ends-with? (or dir "") "/")
    (subs dir 0 (dec (count dir)))
    (or dir "")))

(defn execute-cmd [ws-path top-dir top-ns clojure-version settings github-url [command & args]]
  (try
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
      ;"doc" (doc/execute ws-path top-dir github-url args)
      "help" (help/execute args true)
      "info" (info/execute ws-path top-dir args)
      "prompt" (prompt-cmd)
      "remove" (remove/execute ws-path top-dir args)
      "settings" (settings/execute ws-path settings)
      "success" (success/execute ws-path args)
      "sync" (sync/execute ws-path top-dir)
      "test" (test/execute ws-path top-dir args)
      (println (str "Command '" command "' not found. Type 'help' for help.")))
    (catch IllegalStateException _)))
      ;; ignore

(defn execute [ws-path top-dir top-ns clojure-version settings github-url args]
  (let [ws (last (str/split ws-path #"/"))]
    (print (if (str/blank? @current-dir)
             (str ws "$> ")
             (str ws @current-dir"$> ")))
    (flush)
    (let [expr (read-line)]
      (when-not (or (= "exit" expr)
                    (= "quit" expr))
        (try
          (execute-cmd ws-path top-dir top-ns clojure-version settings github-url (str/split expr #" "))
          (catch IllegalStateException _)
            ;; ignore - thrown by the 'sync' command.
          (catch Exception e
            (stacktrace/print-stack-trace e)))
        (recur ws-path top-dir top-ns clojure-version settings github-url args)))))
