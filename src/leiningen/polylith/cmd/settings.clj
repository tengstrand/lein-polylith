(ns leiningen.polylith.cmd.settings
  (:require [leiningen.polylith.version :as v]
            [leiningen.polylith.time :as time]))

(defn print-bookmarks [ws-path prefix]
  (doseq [[key timestamp] (time/time-bookmarks ws-path prefix)]
    (println " " (time/->time timestamp) (name key))))

(defn execute [ws-path settings [prefix]]
  (println "version:")
  (println " " v/version)
  (println "workspace path:")
  (println " " ws-path)
  (println "settings:")
  (doseq [[k d] (into (sorted-map) settings)]
    (println " " k d))
  (println "bookmarks:")
  (print-bookmarks ws-path prefix))
