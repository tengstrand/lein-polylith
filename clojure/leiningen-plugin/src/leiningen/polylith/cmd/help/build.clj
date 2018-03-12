(ns leiningen.polylith.cmd.help.build)

(defn help [sha1 sha2]
  (println "  Compile, test, and build components, bases and systems between two Git hashes.")
  (println "")
  (println "  lein polylith build s1 s2")
  (println "    s1 = last (successful) Git sha1")
  (println "    s2 = current Git sha1")
  (println)
  (println "  example:")
  (println "    lein polylith build" sha1 sha2))
