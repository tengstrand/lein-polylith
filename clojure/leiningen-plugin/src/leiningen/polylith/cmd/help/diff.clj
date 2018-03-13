(ns leiningen.polylith.cmd.help.diff)

(defn help [sha1 sha2]
  (println "  List all files and directories that has changed between two Git hashes.")
  (println)
  (println "  lein polylith diff HASH1 HASH2")
  (println "    HASH1 = Last (successful) Git sha1")
  (println "    HASH2 = Current Git sha1")
  (println)
  (println "  example:")
  (println "    lein polylith diff" sha1 sha2))
