(ns leiningen.polylith.cmd.help.settings)

(defn help []
  (println "  Show project settings various information.")
  (println)
  (println "  lein polylith success PREFIX")
  (println "    PREFIX = (omitted) -> Uses time.local.edn")
  (println "             prefix    -> Uses given prefix as time.PREFIX.edn.")
  (println)
  (println "  examples:")
  (println "    lein polylith settings")
  (println "    lein polylith settings local"))
