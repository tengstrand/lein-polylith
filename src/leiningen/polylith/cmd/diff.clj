(ns leiningen.polylith.cmd.diff
  (:require [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]))

(defn do-diff [ws-path timestamp]
  (file/changed-relative-paths ws-path
                               (time/paths-except-time ws-path)
                               timestamp))

(defn ->string [string show-time? timestamp]
  (if show-time?
    (str timestamp " " (time/->time timestamp) " " string)
    string))

(defn execute [ws-path args]
  (let [[_ show-time? time] (time/parse-time-args ws-path args)
        paths (do-diff ws-path time)]
    (doseq [[last-modified filename] paths]
      (println " " (->string filename show-time? last-modified)))))
