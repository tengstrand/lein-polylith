(ns leiningen.polylith.cmd.compile
  (:require [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.sync-deps :as sync-deps]))

(defn find-changes [ws-path top-dir args]
  (let [changed-components (changes/changes ws-path top-dir "c" args)
        changed-bases      (changes/changes ws-path top-dir "b" args)
        changed-systems    (changes/changes ws-path top-dir "s" args)]
    (println)
    (apply println "Changed components:" changed-components)
    (apply println "Changed bases:" changed-bases)
    (apply println "Changed systems:" changed-systems)
    (println)
    [changed-components changed-bases changed-systems]))

(defn compile-it [ws-path dir changes]
  (doseq [change changes]
    (println "Compiling" (str dir "/" change))
    (println (shared/sh "lein" "compile" :dir (str ws-path "/" dir "/" change)))))

(defn compile-changes [ws-path components bases systems]
  (println "Compiling interfaces")
  (println (shared/sh "lein" "install" :dir (str ws-path "/interfaces")))
  (compile-it ws-path "components" components)
  (compile-it ws-path "bases" bases)
  (compile-it ws-path "systems" systems))

(defn execute [ws-path top-dir args]
  (let [skip-sync-deps? (contains? (set args) "-sync-deps")
        cleaned-args    (filter #(not= "-sync-deps" %) args)
        [changed-components
         changed-bases
         changed-systems] (find-changes ws-path top-dir cleaned-args)]
    (if (info/has-circular-dependencies? ws-path top-dir)
      (do
        (println "Cannot compile: circular dependencies detected.\n")
        (info/execute ws-path top-dir cleaned-args)
        (throw (Exception. "Cannot compile: circular dependencies detected.")))
      (do
        (when-not skip-sync-deps? (sync-deps/execute ws-path top-dir))
        (compile-changes ws-path changed-components changed-bases changed-systems)))))
