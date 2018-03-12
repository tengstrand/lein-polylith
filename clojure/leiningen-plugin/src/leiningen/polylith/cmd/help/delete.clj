(ns leiningen.polylith.cmd.help.delete)

(defn help []
  (println "  Deletes a component")
  (println)
  (println "  lein polylith delete c[omponent] n")
  (println "    deletes component 'n'")
  (println)
  (println "  example:")
  (println "    lein polylith delete c mycomponent")
  (println "    lein polylith delete component mycomponent"))

