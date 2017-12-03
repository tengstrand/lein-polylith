(ns leiningen.polylith.help)

(defn info []
  (do
    (println " lein polylith info [x] [s1 s2]")
    (println "   x = c -> show changes")
    (println "       u -> show unchanges")
    (println "       (omitted) -> show all")
    (println "       help = show this help")
    (println "   s1 = last successful Git sha1")
    (println "   s2 = current Git sha1")
    (println)
    (println " example:")
    (println "   lein polylith info")
    (println "   lein polylith info 2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1 58cd8b3106c942f372a40616fe9155c9d2efd122")
    (println "   lein polylith info c 2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1 58cd8b3106c942f372a40616fe9155c9d2efd122")))
