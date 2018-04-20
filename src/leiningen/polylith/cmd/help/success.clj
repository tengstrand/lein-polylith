(ns leiningen.polylith.cmd.help.success)

(defn help []
  (println "  Sets last-successful-build in time.edn to modified date of latest changed file in project.")
  (println)
  (println "  lein polylith success"))
