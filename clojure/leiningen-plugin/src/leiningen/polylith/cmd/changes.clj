(ns leiningen.polylith.cmd.changes
  (:require [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]))

(defn changes [ws-path cmd top-dir last-success-sha1 current-sha1]
  (let [paths (diff/diff ws-path last-success-sha1 current-sha1)]
    (condp = cmd
      "a" (info/changed-apis ws-path paths top-dir)
      "b" (info/changed-builds ws-path paths top-dir (info/all-systems ws-path))
      "s" (info/changed-systems ws-path paths (info/all-systems ws-path))
      "c" (info/changed-components ws-path paths)
      [])))

(defn execute [ws-path [cmd sha1 sha2]]
  (if (nil? sha2)
    (do
      (println "Missing parameters.")
      (help/changes sha1 sha2))
    (doseq [dir (changes ws-path cmd sha1 sha2)]
      (println (str " " dir)))))
