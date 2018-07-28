(ns leiningen.polylith.cmd.help.doc)

(defn help []
  (println "  Generates system documentation to the WS-ROOT/doc directory by using")
  (println "  the Selmer template engine (https://github.com/yogthos/Selmer).")
  (println)
  (println "  lein polylith doc [TEMPLATE]")
  (println "    TEMPLATE = (omitted)  -> workspace.ftl.")
  (println "               else       -> Name of the template file in WS/ROOT/doc/templates.")
  (println)
  (println "  example:")
  (println "    lein polylith doc")
  (println "    lein polylith doc mytemplate.html"))
