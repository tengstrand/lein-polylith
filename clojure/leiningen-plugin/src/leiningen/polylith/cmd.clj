(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [leiningen.polylith.core :as core]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]))

(defn bsystems [root-dir]
  (doseq [dir (core/bsystems root-dir)]
    (println (str "  " dir))))

(defn components [root-dir]
  (doseq [dir (core/components root-dir)]
    (println (str "  " dir))))

(defn systems [root-dir]
  (doseq [dir (core/systems root-dir)]
    (println (str "  " dir))))

(defn bcomponents [root-dir [system]]
  (doseq [dir (core/bcomponents root-dir system)]
    (println (str "  " dir))))

(defn help []
  (println "The Polylith architecture: https://github.com/tengstrand/polylith")
  (println)
  (println "  lein polylith x     where x is:")
  (println)
  (println "    bsystems          List all build systems")
  (println "    bcomponents s     List all components of a system (builds/systems)")
  (println "    components        List all components")
  (println "    deps              List all dependencies")
  (println "    gitdiff x s1 s2   List changed components and/or systems between two Git sha1:s")
  (println "    help              Show this help")
  (println "    info              list builds, systems and components")
  (println "    settings          The polylith settings in current project.clj")
  (println "    systems           List all systems"))

(defn info [root-dir]
  (println "systems:")
  (systems root-dir)
  (println "components:")
  (components root-dir)
  (println "builds:")
  (doseq [system (core/bsystems root-dir)]
    (println (str "  " system ":"))
    (doseq [component (core/bcomponents root-dir system)]
      (println (str "    " component)))))

(defn deps [root-dir]
  (doseq [dependency (core/all-dependencies root-dir)]
    (println dependency)))

(defn gitdiff [root-dir [cmd last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith gitdiff x s1 s2")
      (println "     x = a -> show both component and system changes")
      (println "         s -> show system changes")
      (println "         c -> show component changes")
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1")
      (println)
      (println "   examples:")
      (println "     lein polylith gitdiff a 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd")
      (println "     lein polylith gitdiff s 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd")
      (println "     lein polylith gitdiff c 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd"))
    (doseq [dir (core/gitdiff root-dir cmd last-success-sha1 current-sha1)]
      (println (str " " dir)))))

(defn task-not-found [subtask]
  (println "Subtask" subtask "not found.")
  (println "Please type `lein polylith help` for help."))

(defn project-settings [settings]
  (println settings))
