(ns leiningen.polylith.cmd.help.delete)

(defn help []
  (println "  Deletes a component.")
  (println)
  (println "  lein polylith delete c[omponent] NAME")
  (println "    NAME = Then name of the component to delete")
  (println "           (the component will be removed from all environments).")
  (println)
  (println "  example:")
  (println "    lein polylith delete c mycomponent")
  (println "    lein polylith delete component mycomponent"))
