(ns leiningen.polylith.cmd.help.test-and-build)

(defn help []
  (println "  Compiles, execute tests, and builds changes since specific point in time. It also updates local last success time.")
  (println "  Mainly designed to make it easy when you are working on your local.")
  (println)
  (println "  lein polylith test [ARG]")
  (println "    ARG = (omitted) -> Since last successful build, stored in bookmark")
  (println "                       :last-successful-build in WS-ROOT/time.edn.")
  (println "          timestamp -> Since the given timestamp (milliseconds since 1970).")
  (println "          bookmark  -> Since the timestamp for the given bookmark in WS-ROOT/time.edn.")
  (println)
  (println "  examples:")
  (println "    lein polylith test-and-build")
  (println "    lein polylith test-and-build 1523649477000")
  (println "    lein polylith test-and-build mybookmark"))

