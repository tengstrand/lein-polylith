(ns leiningen.polylith.cmd.help
  (:require [leiningen.polylith.cmd.help.add :as add]
            [leiningen.polylith.cmd.help.build :as build]
            [leiningen.polylith.cmd.help.changes :as changes]
            [leiningen.polylith.cmd.help.compile :as compile]
            [leiningen.polylith.cmd.help.create :as create]
            [leiningen.polylith.cmd.help.delete :as delete]
            [leiningen.polylith.cmd.help.deps :as deps]
            [leiningen.polylith.cmd.help.diff :as diff]
            ;[leiningen.polylith.cmd.help.doc :as doc]
            [leiningen.polylith.cmd.help.info :as info]
            [leiningen.polylith.cmd.help.prompt :as prompt]
            [leiningen.polylith.cmd.help.remove :as remove]
            [leiningen.polylith.cmd.help.settings :as settings]
            [leiningen.polylith.cmd.help.success :as success]
            [leiningen.polylith.cmd.help.sync :as sync]
            [leiningen.polylith.cmd.help.test :as test-cmd]
            [leiningen.polylith.version :as v]
            [leiningen.polylith.cmd.shared :as shared]))

(defn help [prompt? ci?]
  (println (str "  Polylith " v/version " (" v/date ") - https://github.com/tengstrand/lein-polylith"))
  (println)
  (println "  lein polylith CMD [ARGS]  - where CMD [ARGS] are:")
  (println)
  (println "    add C S               Adds a component to a system.")
  (println "    build N [A] [S]       Builds changed systems and create artifacts.")
  (println "    changes E P [A]       Lists changed components, bases or systems.")
  (println "    compile P [A] [S]     Compiles changed components, bases and systems.")
  (println "    create X N [A]        Creates a component, system or workspace.")
  (println "    delete c N            Deletes a component.")
  (println "    deps [A]              Lists dependencies.")
  (println "    diff P [A] [F]        Lists all changes since a specific point in time.")
  ;(println "    doc [T]               Generates system documentation.")
  (println "    help [C]              Show this help or help for specified command.")
  (println "    info P [A]            Lists interfaces, components, bases, systems and environments.")
  (println "    prompt                Starts a prompt for current workspace.")
  (println "    remove C S            Removes a component from a system.")
  (println "    settings              Shows polylith settings.")
  (println "    success [B]           Sets last-success or given bookmark.")
  (println "    sync [F]              Syncs library dependencies and system components.")
  (println "    test P [A] [S]        Executes affected tests in components and bases.")
  (when prompt?
    (println)
    (println "  Type 'exit' or 'quit' to exit current 'prompt'."))
  (println)
  (println "  Examples:")
  (println "    lein polylith add mycomponent targetsystem")
  (println "    lein polylith build")
  (println "    lein polylith build -sync -compile -test -success")
  (if ci?
    (println "    lein polylith build 7d7fd132412aad0f8d3019edfccd1e9d92a5a8ae")
    (println "    lein polylith build 1523649477000"))
  (println "    lein polylith build mybookmark")
  (println "    lein polylith changes b")
  (if ci?
    (println "    lein polylith changes c 7d7fd132412aad0f8d3019edfccd1e9d92a5a8ae")
    (println "    lein polylith changes c 1523649477000"))
  (println "    lein polylith changes s mybookmark")
  (println "    lein polylith compile")
  (println "    lein polylith compile -sync")
  (if ci?
    (println "    lein polylith compile 7d7fd132412aad0f8d3019edfccd1e9d92a5a8ae")
    (println "    lein polylith compile 1523649477000"))
  (println "    lein polylith compile mybookmark")
  (println "    lein polylith create c mycomponent")
  (println "    lein polylith create c mycomponent myinterface")
  (println "    lein polylith create s mysystem mybase")
  (println "    lein polylith create w myworkspace -")
  (println "    lein polylith create w myworkspace com.my.company")
  (println "    lein polylith delete mycomponent")
  (println "    lein polylith deps")
  (println "    lein polylith deps +c")
  (println "    lein polylith deps +f")
  (println "    lein polylith deps development")
  (println "    lein polylith deps mycomponent +f")
  (println "    lein polylith deps myenvironment +c")
  (println "    lein polylith diff")
  (if ci?
    (println "    lein polylith diff 7d7fd132412aad0f8d3019edfccd1e9d92a5a8ae")
    (println "    lein polylith diff 1523649477000"))
  (println "    lein polylith diff mybookmark")
  (println "    lein polylith diff mybookmark +")
  ;(println "    lein polylith doc")
  (println "    lein polylith help")
  (println "    lein polylith help info")
  (println "    lein polylith info")
  (if ci?
    (println "    lein polylith info 7d7fd132412aad0f8d3019edfccd1e9d92a5a8ae")
    (println "    lein polylith info 1523649477000"))
  (println "    lein polylith info mybookmark")
  (println "    lein polylith prompt")
  (println "    lein polylith remove mycomponent mysystem")
  (println "    lein polylith settings")
  (println "    lein polylith success")
  (println "    lein polylith success mybookmark")
  (println "    lein polylith sync")
  (println "    lein polylith sync deps")
  (println "    lein polylith test")
  (println "    lein polylith test -compile")
  (if ci?
    (println "    lein polylith test 7d7fd132412aad0f8d3019edfccd1e9d92a5a8ae")
    (println "    lein polylith test 1523649477000"))
  (println "    lein polylith test mybookmark")
  (println "    lein polylith test -compile -success"))

(defn execute [[cmd] prompt?]
  (condp = cmd
    nil (help prompt? (shared/ci?))
    "add" (add/help)
    "build" (build/help)
    "changes" (changes/help)
    "compile" (compile/help)
    "create" (create/help)
    "delete" (delete/help)
    "deps" (deps/help)
    "diff" (diff/help)
    ;"doc" (doc/help)
    "info" (info/help)
    "prompt" (prompt/help)
    "remove" (remove/help)
    "settings" (settings/help)
    "success" (success/help)
    "sync" (sync/help)
    "test" (test-cmd/help)
    (println (str "Missing command '" cmd "'."))))
