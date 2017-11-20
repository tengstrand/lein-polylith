(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [leiningen.polylith.core :as core]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]))

(defn deps []
  (let [dependencies (core/all-dependencies)]
    (p/pprint dependencies)
    (flush)))

(defn build-jenkins [project args]
  (let [url (-> project :polylith :jenkins-build :url)]
    (if (nil? url)
      (do
        (println "Missing 'uri' in project.clj.")
        (println "Add it with e.g.:")
        (println "  :polylith {:jenkins-build {:url \"http://jenkins.mysite.com/job/MyProject\"}}"))
      (core/build-jenkins url args))))

(defn gen-deps []
  (let [file-separator (file/file-separator)]
    (doseq [dependency (core/all-dependencies)]
      (core/create-dependency-file! dependency file-separator))))

(defn task-not-found [subtask]
    (println "Subtask" subtask "not found.")
    (println "Please type: `lein help polylith` for help."))
