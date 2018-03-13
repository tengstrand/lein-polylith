(ns leiningen.polylith.cmd.help.build)

(defn help [sha1 sha2]
  (println "  Compile, test, and build components, bases and systems between two Git hashes.")
  (println)
  (println "  lein polylith build HASH1 HASH2")
  (println "    HASH1 = Last (successful) Git sha1")
  (println "    HASH2 = Current Git sha1")
  (println)
  (println "  Example:")
  (println "    lein polylith build" sha1 sha2))
