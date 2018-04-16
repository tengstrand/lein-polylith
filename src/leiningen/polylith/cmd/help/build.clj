(ns leiningen.polylith.cmd.help.build)

(defn help []
  (println "  Build artifacts.")
  (println)
  (println "  lein polylith build [ARG]")
  (println "    ARG = (omitted) -> Since last successful build, stored in bookmark")
  (println "                       :last-successful-build in WS-ROOT/.polylith/local.time.")
  (println "          timestamp -> Since the given timestamp (milliseconds since 1970).")
  (println "          bookmark  -> Since the timestamp for the given bookmark in WS-ROOT/.polylith/local.time.")
  (println)
  (println "  examples:")
  (println "    lein polylith build")
  (println "    lein polylith build 1523649477000")
  (println "    lein polylith build mybookmark"))

