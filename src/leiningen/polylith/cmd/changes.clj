(ns leiningen.polylith.cmd.changes
  (:require [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.help.changes :as changes-help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.shared :as shared]))

(defn changes [ws-path top-dir cmd args]
  (let [paths (diff/changed-file-paths ws-path args)]
    (cond
      (shared/interface? cmd) (info/changed-interfaces ws-path top-dir paths)
      (shared/component? cmd) (info/changed-components ws-path paths)
      (shared/base? cmd) (info/changed-bases ws-path paths)
      (shared/system? cmd) (info/changed-systems ws-path top-dir paths)
      :else [])))

(defn execute [ws-path top-dir [cmd & args]]
  (if (nil? cmd)
    (do
      (println "Missing parameters.")
      (changes-help/help))
    (doseq [dir (changes ws-path top-dir cmd args)]
      (println (str " " dir)))))
