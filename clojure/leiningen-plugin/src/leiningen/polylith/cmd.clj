(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [leiningen.polylith.core :as core]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]))

(defn components [root-dir]
  (doseq [dir (core/components root-dir)]
    (println (str " " dir))))

(defn systems [root-dir]
  (doseq [dir (core/systems root-dir)]
    (println (str " " dir))))

(defn help []
  (println "The Polylith architecture: https://github.com/tengstrand/polylith")
  (println)
  (println "  lein polylith x     where x is:")
  (println)
  (println "    components     List all components")
  (println "    deps           List all dependencies")
  ;(println "    gen-deps       Generate dependency files")
  (println "    git-changes    List changed components and/or systems between two Git sha1:s")
  (println "    help           Show this help")
  (println "    settings       The polylith settings in current project.clj"))

(defn deps [root-dir]
  (let [dependencies (core/all-dependencies root-dir)]
    (p/pprint dependencies)
    (flush)))

(defn git-changes [root-dir [dir current-sha1 last-success-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith git-changes x s1 s2")
      (println "     x = all -> show both component and system changes")
      (println "         components -> show component changes")
      (println "         systems -> show system changes")
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1")
      (println)
      (println "   examples:")
      (println "     lein polylith git-changes all 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd")
      (println "     lein polylith git-changes systems 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd")
      (println "     lein polylith git-changes components 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd"))
    (doseq [dir (core/git-changes root-dir dir last-success-sha1 current-sha1)]
      (println (str " " dir)))))

(defn task-not-found [subtask]
  (println "Subtask" subtask "not found.")
  (println "Please type: `lein help polylith` for help."))

;(defn gen-deps []
;  (let [file-separator (file/file-separator)]
;    (doseq [dependency (core/all-dependencies)]
;      (core/create-dependency-file! dependency file-separator))))

(defn project-settings [settings]
  (println settings))
