(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [clojure.string :as str]
            [leiningen.polylith.core :as core]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.help :as help]
            [leiningen.polylith.info :as info]
            [leiningen.polylith.match :as match]))

(defn help [[cmd]]
  (condp = cmd
    "changes" (help/changes)
    "compile" (help/compile-components-and-systems)
    "deps" (help/deps)
    "diff" (help/diff)
    "info" (help/info)
    "settings" (help/settings)
    "tests" (help/tests)
    (help/help)))

(defn changes [root-dir [cmd last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters.")
      (help/changes))
    (doseq [dir (core/changes root-dir cmd last-success-sha1 current-sha1)]
      (println (str " " dir)))))

(defn- print-entities [directory changed-entities]
  (when (not (empty? changed-entities))
    (println (str "cd " directory))
    (doseq [[i entity] (map-indexed vector changed-entities)]
      (println "\necho '-----" entity "-----'")
      (let [dir (if (zero? i) "./" "../")]
        (println (str "cd " dir entity))
        (println "lein install")))
    (println "cd ../..")))

(defn compile-componens-and-systems [root-dir [last-success-sha1 current-sha1]]
  (let [{:keys [changed-components
                changed-systems]} (core/info root-dir last-success-sha1 current-sha1)]
    (println "# ===== apis =====")
    (println "cd ../apis")
    (println "lein install")
    (println "cd ..")
    (println)
    (print-entities "components" changed-components)
    (println)
    (print-entities "systems" changed-systems)
    (println "cd systems")))

(defn deps [root-dir]
  (doseq [dependency (core/all-dependencies root-dir)]
    (println dependency)))

(defn diff [root-dir [last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters.")
      (help/diff))
    (let [paths (core/diff root-dir last-success-sha1 current-sha1)]
      (doseq [path paths]
        (println " " path)))))

(defn info [root-dir args]
  (let [cmd (first args)
        a? (= "a" cmd)
        filter? (= 1 (count cmd))
        [show-changed?
         show-unchanged?
         show-apis?] (if filter?
                            [(or a? (= "c" cmd))
                             (or a? (= "u" cmd))
                             a?]
                            [true true false])
        [last-success-sha1
         current-sha1] (if filter? (rest args) args)
        data (if (and last-success-sha1 current-sha1)
               (core/info root-dir last-success-sha1 current-sha1)
               (core/info root-dir))]
    (info/print-info data show-changed? show-unchanged? show-apis?)))


(defn settings [root-dir settings]
  (println "root-dir:")
  (println " " root-dir)
  (println "settings:")
  (println " " settings))

(defn tests [root-dir ignore-tests [cmd last-success-sha1 current-sha1]]
  (if (nil? cmd)
    (do
      (println "Missing parameters.")
      (help/tests))
    (let [u? (str/includes? cmd "u")
          i? (str/includes? cmd "i")
          single-line-statement? (str/includes? cmd "-")
          execute? (str/includes? cmd "+")
          tests (match/filter-tests
                  (if (and last-success-sha1 current-sha1)
                    (core/tests root-dir [u? i?] [last-success-sha1 current-sha1])
                    (core/tests root-dir [u? i?]))
                  ignore-tests)]
      (if execute?
        (core/run-tests tests single-line-statement?)
        (core/show-tests tests single-line-statement?)))))

(defn task-not-found [subtask]
  (println "Subtask" subtask "not found.")
  (help/help))

