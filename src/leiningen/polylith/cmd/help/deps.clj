(ns leiningen.polylith.cmd.help.deps)

(defn help []
  (println "  List dependencies.")
  (println)
  (println "  lein polylith deps [f]")
  (println)
  (println "  example:")
  (println "    lein polylith deps     list dependencies to interfaces")
  (println "    lein polylith deps f   list dependencies to functions"))
