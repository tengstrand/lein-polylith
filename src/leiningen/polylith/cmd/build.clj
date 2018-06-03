(ns leiningen.polylith.cmd.build
  (:require [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.test :as test]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.info :as info]
            [clojure.java.io :as io]))

(defn find-changes [ws-path top-dir args print-info?]
  (let [changed-systems (changes/changes ws-path top-dir "s" args)]
    (when print-info?
      (println)
      (apply println "Changed systems:" changed-systems)
      (println))
    changed-systems))

(defn build [ws-path changed-systems]
  (doseq [system changed-systems]
    (println "Building" (str "systems/" system))
    (if-not (.exists (io/file (str ws-path "/systems/" system "/build.sh")))
      (println (shared/sh "lein" "uberjar" :dir (str ws-path "/systems/" system)))
      (println (shared/sh "./build.sh" :dir (str ws-path "/systems/" system))))))

(defn execute [ws-path top-dir args]
  (let [skip-compile?   (contains? (set args) "-compile")
        skip-test?      (contains? (set args) "-test")
        skip-success?   (contains? (set args) "-success")
        cleaned-args    (filter #(and (not= "-compile" %)
                                      (not= "-test" %)
                                      (not= "-success" %))
                                args)
        changed-systems (find-changes ws-path top-dir cleaned-args skip-compile?)]
    (if (info/has-circular-dependencies? ws-path top-dir)
      (do
        (println "Cannot compile: circular dependencies detected.\n")
        (info/execute ws-path top-dir args)
        (throw (Exception. "Cannot compile: circular dependencies detected.")))
      (do
        (when-not skip-compile? (compile/execute ws-path top-dir cleaned-args))
        (when-not skip-test? (test/execute ws-path top-dir (conj cleaned-args "-compile")))
        (build ws-path changed-systems)
        (when-not skip-success? (time/set-last-successful-build! ws-path))))))
