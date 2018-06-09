(ns leiningen.polylith.cmd.help.delete)

(defn help []
  (println "  Deletes a component and its interface if no other components use it.")
  (println)
  (println "  lein polylith delete c NAME")
  (println "    NAME = component to delete")
  (println)
  (println "  example:")
  (println "    lein polylith delete mycomponent"))
