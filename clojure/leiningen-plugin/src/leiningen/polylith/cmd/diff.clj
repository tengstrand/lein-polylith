(ns leiningen.polylith.cmd.diff
  (:require [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]))

(defn diff [ws-path point-in-time]
  (file/changed-relative-paths ws-path
                               (time/paths-except-time ws-path)
                               point-in-time))

(defn parse-timestamp [bookmark-or-point-in-time]
  (try
    [true (Long/parseLong bookmark-or-point-in-time)]
    (catch Exception _ [false])))

(defn parse-time-argument [ws-path bookmark-or-point-in-time]
  (let [[ok? timestamp] (parse-timestamp bookmark-or-point-in-time)]
    (if ok?
      timestamp
      (let [bookmarks (time/time-bookmarks ws-path)
            bookmark (keyword bookmark-or-point-in-time)
            point-in-time (:timestamp (bookmarks bookmark))]
        (or point-in-time 0)))))

(defn execute [ws-path [bookmark-or-point-in-time]]
  (let [time (if bookmark-or-point-in-time
               (parse-time-argument ws-path bookmark-or-point-in-time)
               (time/last-successful-build-time ws-path))
        paths (diff ws-path time)]
    (doseq [path paths]
      (println " " path))))
