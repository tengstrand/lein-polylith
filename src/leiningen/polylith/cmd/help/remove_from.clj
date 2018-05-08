(ns leiningen.polylith.cmd.help.remove-from)

(defn help []
  (println "  Removes a component from a system.")
  (println)
  (println "  lein polylith remove-from SYSTEM COMPONENT")
  (println "    SYSTEM = Name of the system")
  (println "    COMPONENT = Name of the component")
  (println)
  (println "  example:")
  (println "    lein polylith remove-from mysystem mycomponent"))
