(ns leiningen.polylith.cmd.help.remove-from)

(defn help []
  (println "  Removes a component from a system.")
  (println)
  (println "  lein polylith remove-from [TYPE] SYSTEM COMPONENT")
  (println "    TYPE   = s[ystem] -> Remove a component from a system")
  (println "    SYSTEM = Name of the system")
  (println "    COMPONENT = Name of the component")
  (println)
  (println "  example:")
  (println "    lein polylith remove-from mysystem mycomponent")
  (println "    lein polylith remove-from s mysystem mycomponent")
  (println "    lein polylith remove-from system mysystem mycomponent"))
