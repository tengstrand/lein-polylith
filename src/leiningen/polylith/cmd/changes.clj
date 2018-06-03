(ns leiningen.polylith.cmd.changes
  (:require [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.help.changes :as changes-help]
            [leiningen.polylith.cmd.info :as info]))

(defn changes [ws-path top-dir cmd args]
  (let [paths (diff/changed-file-paths ws-path args)]
    (condp = cmd
      "i" (info/changed-interfaces ws-path top-dir paths)
      "interface" (info/changed-interfaces ws-path top-dir paths)
      "c" (info/changed-components ws-path paths)
      "component" (info/changed-components ws-path paths)
      "b" (info/changed-bases ws-path paths)
      "base" (info/changed-bases ws-path paths)
      "bases" (info/changed-bases ws-path paths)
      "s" (info/changed-systems ws-path top-dir paths)
      "system" (info/changed-systems ws-path top-dir paths)
      [])))

(defn execute [ws-path top-dir [cmd & args]]
  (if (nil? cmd)
    (do
      (println "Missing parameters.")
      (changes-help/help))
    (doseq [dir (changes ws-path top-dir cmd args)]
      (println (str " " dir)))))
