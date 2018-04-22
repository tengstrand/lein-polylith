(ns leiningen.polylith.cmd.changes
  (:require [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.help.changes :as changes-help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.shared :as shared]))

(defn changes [ws-path top-dir cmd args]
  (let [[_ time] (time/parse-time-args ws-path args)
        paths (map second (diff/do-diff ws-path time))]
    (condp = cmd
      "i" (info/changed-interfaces ws-path paths top-dir)
      "interface" (info/changed-interfaces ws-path paths top-dir)
      "c" (info/changed-components ws-path paths)
      "component" (info/changed-components ws-path paths)
      "b" (info/changed-bases ws-path paths)
      "base" (info/changed-bases ws-path paths)
      "bases" (info/changed-bases ws-path paths)
      "s" (info/changed-systems ws-path paths top-dir (shared/all-bases ws-path))
      "system" (info/changed-systems ws-path paths top-dir (shared/all-bases ws-path))
      [])))

(defn execute [ws-path top-dir [cmd & args]]
  (if (nil? cmd)
    (do
      (println "Missing parameters.")
      (changes-help/help))
    (doseq [dir (changes ws-path top-dir cmd args)]
      (println (str " " dir)))))
