(ns leiningen.polylith.cmd.help.remove)

(defn help []
  (println "  Removes a component from a system.")
  (println)
  (println "  lein polylith remove COMPONENT SYSTEM")
  (println "    COMPONENT = Name of the component")
  (println "    SYSTEM    = Name of the system")
  (println)
  (println "  example:")
  (println "    lein polylith remove mycomponent mysystem"))
