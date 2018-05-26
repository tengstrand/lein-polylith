(ns leiningen.polylith.cmd.help.sync)

(defn help []
  (println "  Make sure the library versions in project.clj files of")
  (println "  components and bases are in sync with the library")
  (println "  versions in environment/development/project.clj.")
  (println)
  (println "  lein polylith sync")
  (println)
  (println "  examples:")
  (println "    lein polylith sync"))
