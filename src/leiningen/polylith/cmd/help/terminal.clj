(ns leiningen.polylith.cmd.help.terminal)

(defn help []
  (println "  Starts a terminal for current workspace.")
  (println)
  (println "  When working from the terminal every command will run instantly with no delay.")
  (println "  Another advantage is that you don't have to repeat 'lein polylith' all the time.")
  (println "  Just type (e.g.) 'info' instead of 'lein polylith info'.")
  (println)
  (println "  Type 'exit' or 'quit' to exit the terminal.")
  (println)
  (println "  examples:")
  (println "    lein polylith terminal"))
