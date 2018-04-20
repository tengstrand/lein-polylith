(ns leiningen.polylith.cmd.build
  (:require [leiningen.polylith.cmd.changes :as changes]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.java.io :as io]))

(defn find-changes [ws-path top-dir args]
  (let [changed-systems (changes/changes ws-path top-dir "s" args)]
    (println)
    (apply println "Changed systems:" changed-systems)
    (println)
    changed-systems))

(defn build [ws-path changed-systems]
  (doseq [system changed-systems]
    (println "Building" (str "systems/" system))
    (if-not (.exists (io/file (str ws-path "/systems/" system "/build.sh")))
      (println (shared/sh "lein uberjar" :dir (str ws-path "/systems/" system)))
      (println (shared/sh "./build.sh" :dir (str ws-path "/systems/" system))))))

(defn execute [ws-path top-dir args]
  (let [changed-systems (find-changes ws-path top-dir args)]
    (build ws-path changed-systems)))
