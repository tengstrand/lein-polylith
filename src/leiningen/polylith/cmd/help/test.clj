(ns leiningen.polylith.cmd.help.test)

(defn help []
  (println "  Execute tests.")
  (println)
  (println "  lein polylith test [ARG]")
  (println "    ARG = (omitted) -> Since last successful build, stored in bookmark")
  (println "                       :last-successful-build in WS-ROOT/time.edn.")
  (println "          timestamp -> Since the given timestamp (milliseconds since 1970).")
  (println "          bookmark  -> Since the timestamp for the given bookmark in WS-ROOT/time.edn.")
  (println)
  (println "  examples:")
  (println "    lein polylith test")
  (println "    lein polylith test 1523649477000")
  (println "    lein polylith test mybookmark"))

