(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [leiningen.polylith.core :as core]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]))

(defn deps []
  (let [dependencies (core/all-dependencies)]
    (p/pprint dependencies)
    (flush)))

(defn build-git [[current-sha1 last-success-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Both current and last-success SHA1 must be set as arguments:")
      (println "  lein polylith build-git current-sha1 last-success-sha1")
      (println "e.g.:")
      (println "  lein polylith build-git 1c5196cb4a0aa5f30c8ac52220614e959440e37b 8dfb454c5ed7849b52991335be1a794d591671dd"))
    (core/build-git current-sha1 last-success-sha1)))

(defn gen-deps []
  (let [file-separator (file/file-separator)]
    (doseq [dependency (core/all-dependencies)]
      (core/create-dependency-file! dependency file-separator))))

(defn task-not-found [subtask]
    (println "Subtask" subtask "not found.")
    (println "Please type: `lein help polylith` for help."))
