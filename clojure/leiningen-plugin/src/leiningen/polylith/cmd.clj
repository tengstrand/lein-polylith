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
  (println "    bcomponents s     List all components of for the build systems")
  (println "    components        List all components")
  (println "    deps              List all dependencies")
  (println "    gitdiff x s1 s2   List changed components and/or systems between two Git sha1:s")
  (println "    help              Show this help")
  (println "    info x [s1 s2]    list systems components and builds (and changes)")
  (println "    settings          The polylith settings in current project.clj")
  (println "    systems           List all systems"))

(defn- print-elem [spaces show? show-only-changes? elem diff]
  (let [changed? (contains? diff elem)]
    (if (and show?
             (or (not show-only-changes?)
                 (and changed? show-only-changes?)))
      (println (str spaces elem (if changed? " *" ""))))))

(defn info-diff [root-dir cmd last-success-sha1 current-sha1]
  (let [show-only-all-changes? (= "a+" cmd)
        show-all? (or show-only-all-changes? (= "a" cmd))
        show-only-component-changes? (or show-only-all-changes? (= "c+" cmd))
        show-only-system-changes? (or show-only-all-changes? (= "s+" cmd))
        show-component? (or show-all? show-only-component-changes? (= "c" cmd))
        show-system? (or show-all? show-only-system-changes? (= "s" cmd))
        cdiff (set (core/gitdiff root-dir "c" last-success-sha1 current-sha1))
        sdiff (set (core/gitdiff root-dir "s" last-success-sha1 current-sha1))]

    (when show-system?
      (println "systems:")
      (doseq [system (core/systems root-dir)]
        (print-elem "  " show-system? show-only-system-changes? system sdiff)))

    (when show-component?
      (println "components:")
      (doseq [component (core/components root-dir)]
        (print-elem "  " show-component? show-only-component-changes? component cdiff)))

    (when show-all?
      (println "builds:")
      (doseq [system (core/bsystems root-dir)]
        (print-elem "  " true false system sdiff)
        ;(println (str "  " system (contains system sdiff)))
        (doseq [component (core/bcomponents root-dir system)]
          (print-elem "    " show-component? show-only-component-changes? component cdiff))))))

(defn info-plain [root-dir cmd]
  (let [show-all? (= "a" cmd)
        show-component? (or show-all? (= "c" cmd))
        show-system? (or show-all? (= "s" cmd))]

    (when show-system?
      (println "systems:")
      (systems root-dir))

    (when show-component?
      (println "components:")
      (components root-dir))

    (when show-all?
      (println "builds:")
      (doseq [system (core/bsystems root-dir)]
        (println (str "  " system))
        (doseq [component (core/bcomponents root-dir system)]
          (println (str "    " component)))))))

(defn info [root-dir [cmd last-success-sha1 current-sha1]]
  (if (nil? cmd)
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith info x [s1 s2]")
      (println "     x = a -> show all")
      (println "         a+ -> show all, but only changes")
      (println "         s -> show systems")
      (println "         s+ -> show system changes")
      (println "         c -> show components")
      (println "         c+ -> show component changes")
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1"))
    (if (or (nil? last-success-sha1) (nil? current-sha1))
      (info-plain root-dir cmd)
      (info-diff root-dir cmd last-success-sha1 current-sha1))))

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
