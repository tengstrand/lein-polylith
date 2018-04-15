(ns leiningen.polylith.cmd.help.build)

(defn help []
  (println "  UPDATE THIS!!! Compile, test, and build components, bases on changes from and systems between two Git hashes.")
  (println)
  (println "  lein polylith build BUILD-NUMBER [ARGS]")
  (println "    BUILD-NUMBER = The number of the build.")
  (println)
  (println "  Example:")
  (println "    lein polylith build 42"))
