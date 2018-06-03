(ns leiningen.polylith.cmd.help.rename)

(defn help []
  (println "  Renames a component.")
  (println "  Only the top directory and links are updated,")
  (println "  not the actual source code.")
  (println)
  (println "  lein polylith rename c FROM TO")
  (println "    FROM = The component to rename.")
  (println "    TO   = The new name of the component.")
  (println)
  (println "  example:")
  (println "    lein polylith rename c oldname newname"))
