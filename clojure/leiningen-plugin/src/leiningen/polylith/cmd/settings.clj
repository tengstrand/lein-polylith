(ns leiningen.polylith.cmd.settings
  (:require [leiningen.polylith.version :as v]))

(defn execute [ws-path settings]
  (println "version:")
  (println " " v/version)
  (println "workspace path:")
  (println " " ws-path)
  (println "settings:")
  (doseq [[k d] (into (sorted-map) settings)]
    (println " " k d)))
