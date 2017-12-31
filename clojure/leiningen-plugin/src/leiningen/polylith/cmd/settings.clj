(ns leiningen.polylith.cmd.settings)

(defn execute [ws-path settings]
  (println "workspace path:")
  (println " " ws-path)
  (println "settings:")
  (doseq [[k d] settings]
    (println " " k d)))
