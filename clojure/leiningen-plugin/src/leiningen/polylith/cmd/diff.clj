(ns leiningen.polylith.cmd.diff
  (:require [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]
            [clojure.string :as str])
  (:import (java.text SimpleDateFormat)
           (java.util Date)))

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
            point-in-time (bookmarks bookmark)]
        (or point-in-time 0)))))

(defn parse-args [ws-path args]
  (let [show-time? (contains? (set args) "+")
        bookmark-or-point-in-time (first (filter #(not= "+" %) args))
        time (if bookmark-or-point-in-time
               (parse-time-argument ws-path bookmark-or-point-in-time)
               (time/last-successful-build-time ws-path))]
    [show-time? time]))

(def formatter (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS"))

(defn ->time [timestamp]
  (let [time (.format formatter (Date. timestamp))]
    (str (subs time 0 10) " "
         (subs time 11 16) " "
         (subs time 17))))

(defn ->string [string show-time? timestamp]
  (if show-time?
    (str timestamp " " (->time timestamp) " " string)
    string))

(defn execute [ws-path args]
  (let [[show-time? time] (parse-args ws-path args)
        paths (diff ws-path time)]
    (doseq [[last-modified filename] paths]
      (println " " (->string filename show-time? last-modified)))))
