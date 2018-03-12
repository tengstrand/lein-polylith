(ns leiningen.polylith.cmd.help.diff)

(defn help [sha1 sha2]
  (println "  List all files and directories that has changed between two Git sha1's")
  (println)
  (println "  lein polylith diff s1 s2")
  (println "    s1 = last (successful) Git sha1")
  (println "    s2 = current Git sha1")
  (println)
  (println "  example:")
  (println "    lein polylith diff" sha1 sha2))
