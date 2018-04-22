(ns leiningen.polylith.cmd.help.success)

(defn help []
  (println "  Sets last-successful-build to modified date of latest changed file in project.")
  (println)
  (println "  lein polylith success PREFIX")
  (println "    PREFIX = (omitted) -> Uses local.time")
  (println "             prefix    -> Uses given prefix with .time extension.")
  (println)
  (println "  examples:")
  (println "    lein polylith success")
  (println "    lein polylith success local")
  (println "    lein polylith success remote"))
