(ns leiningen.polylith.cmd.changes
  (:require [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]))

(defn changes [ws-path cmd top-dir last-success-sha1 current-sha1]
  (let [paths (diff/diff ws-path last-success-sha1 current-sha1)]
    (condp = cmd
      "i" (info/changed-interfaces ws-path paths top-dir)
      "interface" (info/changed-interfaces ws-path paths top-dir)
      "c" (info/changed-components ws-path paths)
      "component" (info/changed-components ws-path paths)
      "b" (info/changed-bases ws-path paths)
      "base" (info/changed-bases ws-path paths)
      "bases" (info/changed-bases ws-path paths)
      "s" (info/changed-systems ws-path paths top-dir (info/all-bases ws-path))
      "system" (info/changed-systems ws-path paths top-dir (info/all-bases ws-path))
      [])))

(defn execute [ws-path top-dir [cmd sha1 sha2]]
  (if (nil? sha2)
    (do
      (println "Missing parameters.")
      (help/changes sha1 sha2))
    (doseq [dir (changes ws-path cmd top-dir sha1 sha2)]
      (println (str " " dir)))))
