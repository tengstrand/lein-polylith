(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [clojure.string :as str]
            [leiningen.polylith.core :as core]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.info :as info]))

(defn help []
  (println "Polylith 0.7-alpha-snapshot")
  (println "  https://github.com/tengstrand/polylith")
  (println)
  (println "  lein polylith x     (where x is):")
  (println)
  (println "    changed x s1 s2   List changed components, systems or builds")
  (println "                      between two Git sha1:s")
  (println "    deps              List all dependencies")
  (println "    help              Show this help")
  (println "    info x [s1 s2]    list systems, components and builds")
  (println "    settings          The polylith settings in current project.clj"))

(defn info [root-dir [cmd last-success-sha1 current-sha1]]
  (if (nil? cmd)
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith info x [s1 s2]")
      (println "     x = a -> show all")
      (println "         c -> show changes")
      (println "         + -> show Git diff")
      (println "         a+ -> show all + diff")
      (println "         c+ -> show changes + diff")
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1"))
    (let [[show-changed?
           show-unchanged?
           show-diff?] (condp = cmd
                         "a" [true true false]
                         "a+" [true true true]
                         "+" [false false true]
                         "c" [true false false]
                         "c+" [true false true])
           diff (core/info root-dir last-success-sha1 current-sha1)]
      (if (or show-changed? show-unchanged?)
        (info/print-info diff show-unchanged?)))))

(defn deps [root-dir]
  (doseq [dependency (core/all-dependencies root-dir)]
    (println dependency)))

(defn changed [root-dir [cmd last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith gitdiff x s1 s2")
      (println "     x = b -> show changed builds")
      (println "         s -> show changed systems")
      (println "         c -> show changed components")
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1")
      (println)
      (println "   example:")
      (println "     lein polylith gitdiff s 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd"))
    (doseq [dir (core/changed root-dir cmd last-success-sha1 current-sha1)]
      (println (str " " dir)))))

(defn task-not-found [subtask]
  (println "Subtask" subtask "not found.")
  (help))

(defn settings [polylith-settings]
  (println polylith-settings))
