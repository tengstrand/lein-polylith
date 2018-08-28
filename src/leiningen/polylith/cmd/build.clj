(ns leiningen.polylith.cmd.build
  (:require [clojure.java.io :as io]
            [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.success :as success]
            [leiningen.polylith.cmd.sync :as sync]
            [leiningen.polylith.cmd.test :as test]))

(defn find-changes [ws-path top-dir args print-info?]
  (let [changed-systems (changes/changes ws-path top-dir "s" args)]
    (when print-info?
      (println)
      (apply println "Changed systems:" changed-systems)
      (println))
    changed-systems))

(defn build [ws-path changed-systems]
  (doseq [system (sort changed-systems)]
    (println "Building" (str "systems/" system))
    (if-not (.exists (io/file (str ws-path "/systems/" system "/build.sh")))
      (println (shared/sh "lein" "uberjar" :dir (str ws-path "/systems/" system)))
      (println (shared/sh "./build.sh" :dir (str ws-path "/systems/" system))))))

(defn execute [ws-path top-dir args]
  (let [skip-circular-deps? (contains? (set args) "-circular-deps")
        skip-compile?       (contains? (set args) "-compile")
        skip-test?          (contains? (set args) "-test")
        skip-success?       (contains? (set args) "-success")
        skip-sync?          (contains? (set args) "-sync")
        cleaned-args        (filter #(and (not= "-compile" %)
                                          (not= "-test" %)
                                          (not= "-success" %)
                                          (not= "-sync" %)
                                          (not= "-circular-deps" %))
                                    args)
        changed-systems     (find-changes ws-path top-dir cleaned-args skip-compile?)]
    (if (and (not skip-circular-deps?)
             (info/has-circular-dependencies? ws-path top-dir))
      (do
        (println "Cannot compile: circular dependencies detected.\n")
        (info/execute ws-path top-dir args)
        (throw (Exception. "Cannot compile: circular dependencies detected.")))
      (do
        (when-not skip-sync? (sync/execute ws-path top-dir ["all"]))
        (when-not skip-compile? (compile/execute ws-path top-dir (conj cleaned-args "-sync" "-circular-deps")))
        (when-not skip-test? (test/execute ws-path top-dir (conj cleaned-args "-compile" "-sync" "-circular-deps")))
        (build ws-path changed-systems)
        (when-not skip-success? (success/execute ws-path cleaned-args))))))
