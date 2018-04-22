(ns leiningen.polylith.cmd.help.settings)

(defn help []
  (println "  Show project settings various information.")
  (println)
  (println "  lein polylith success PREFIX")
  (println "    PREFIX = (omitted) -> Uses local.time")
  (println "             prefix    -> Uses given prefix with .time extension.")
  (println)
  (println "  examples:")
  (println "    lein polylith settings")
  (println "    lein polylith settings local"))
