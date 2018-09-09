(ns leiningen.polylith.cmd.settings
  (:require [leiningen.polylith.version :as v]
            [leiningen.polylith.time :as time])
  (:import (java.util Date)))

(defn print-current-time []
  (let [millis (.getTime (Date.))]
    (println " " (str (time/->time millis) " (" millis ")"))))

(defn print-bookmarks [ws-path]
  (doseq [[key millis] (time/time-bookmarks ws-path)]
    (println " " (str (time/->time millis) " " (name key) " (" millis ")"))))

(defn execute [ws-path settings]
  (println "polylith version:")
  (println " " v/version)
  (println "workspace path:")
  (println " " ws-path)
  (println "settings:")
  (doseq [[k d] (into (sorted-map) settings)]
    (println (str "  " (name k) ": " d)))
  (println "current time:")
  (print-current-time)
  (println "bookmarks:")
  (print-bookmarks ws-path))
