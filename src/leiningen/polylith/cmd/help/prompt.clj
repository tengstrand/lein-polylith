(ns leiningen.polylith.cmd.help.prompt)

(defn help []
  (println "  Starts a prompt for current workspace.")
  (println)
  (println "  Allows faster execution of all the commands from any directory.")
  (println)
  (println "  Type 'exit' or 'quit' to exit the prompt.")
  (println)
  (println "  examples:")
  (println "    lein polylith prompt"))
