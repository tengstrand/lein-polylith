(ns leiningen.polylith.cmd.changes
  (:require [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]))

(defn changes [ws-path cmd last-success-sha1 current-sha1]
  (let [{:keys [changed-apis
                changed-builds
                changed-systems
                changed-components]} (info/info ws-path last-success-sha1 current-sha1)]
    (condp = cmd
      "a" changed-apis
      "b" changed-builds
      "s" changed-systems
      "c" changed-components
      [])))

(defn execute [ws-path [cmd last-success-sha1 current-sha1]]
  (if (nil? current-sha1)
    (do
      (println "Missing parameters.")
      (help/changes))
    (doseq [dir (changes ws-path cmd last-success-sha1 current-sha1)]
      (println (str " " dir)))))
