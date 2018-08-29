(ns leiningen.polylith.cmd.compile
  (:require [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.sync :as sync]
            [leiningen.polylith.cmd.diff :as diff]))

(defn find-changes [ws-path top-dir args]
  (let [paths              (diff/changed-file-paths ws-path args)
        changed-components (info/changed-components ws-path paths)
        changed-bases      (info/changed-bases ws-path paths)
        changed-systems    (info/changed-systems ws-path top-dir paths)]
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
  (compile-it ws-path "components" (sort components))
  (compile-it ws-path "bases" (sort bases))
  (compile-it ws-path "systems" (sort systems)))

(defn execute [ws-path top-dir args]
  (let [skip-circular-deps? (contains? (set args) "-circular-deps")
        skip-sync? (contains? (set args) "-sync")
        cleaned-args (filter #(and (not= "-sync" %)
                                   (not= "-circular-deps" %))
                             args)
        [changed-components
         changed-bases
         changed-systems] (find-changes ws-path top-dir cleaned-args)]
    (if (and (not skip-circular-deps?)
             (info/has-circular-dependencies? ws-path top-dir))
      (do
        (println "Cannot compile: circular dependencies detected.\n")
        (info/execute ws-path top-dir cleaned-args)
        (throw (Exception. "Cannot compile: circular dependencies detected.")))
      (when (or skip-sync? (sync/sync-all ws-path top-dir "compile"))
        (compile-changes ws-path changed-components changed-bases changed-systems)))))
