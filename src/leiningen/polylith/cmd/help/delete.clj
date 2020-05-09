(ns leiningen.polylith.cmd.help.delete)

(defn help []
  (println "  Deletes a component and its interface if no other components use it:")
  (println)
  (println "  lein polylith delete c[omponent] NAME")
  (println "    NAME = Component name")
  (println "  --------------------------------------------------------")
  (println "  Deletes a base:")
  (println)
  (println "  lein polylith delete b[ase] NAME")
  (println "    NAME = Base name.")
  (println "  --------------------------------------------------------")
  (println "  Deletes a system (and its base if given):")
  (println)
  (println "  lein polylith delete s[ystem] NAME [BASE]")
  (println "    NAME = System name.")
  (println "    BASE = Base name.")
  (println)
  (println "  example:")
  (println "    lein polylith delete c mycomponent")
  (println "    lein polylith delete component mycomponent")
  (println "    lein polylith delete b mybase")
  (println "    lein polylith create base mybase")
  (println "    lein polylith delete s mysystem")
  (println "    lein polylith delete system mysystem")
  (println "    lein polylith delete s mysystem mybase")
  (println "    lein polylith delete system mysystem mybase"))
