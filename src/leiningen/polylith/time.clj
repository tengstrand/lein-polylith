(ns leiningen.polylith.time
  (:require [clojure.pprint :as pp]
            [leiningen.polylith.file :as file])
  (:import (java.io FileNotFoundException)
           (java.util Date)
           (java.text SimpleDateFormat)))

(defn time-bookmarks [ws-path]
  (try
    (read-string (slurp (str ws-path "/.polylith/time.edn")))
    (catch FileNotFoundException _ {})))

(defn last-successful-build-time [ws-path]
  (or (:last-successful-build (time-bookmarks ws-path))
      0))

(defn set-last-successful-build! [ws-path]
  (let [paths (file/valid-paths ws-path)
        latest-change (file/latest-modified paths)
        bookmarks (assoc (time-bookmarks ws-path)
                    :last-successful-build latest-change)
        file (str ws-path "/.polylith/time.edn")]
    (pp/pprint bookmarks (clojure.java.io/writer file))))

(def formatter (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))

(defn ->time [timestamp]
  (.format formatter (Date. timestamp)))

(defn parse-timestamp [bookmark-or-point-in-time]
  (try
    [true (Long/parseLong bookmark-or-point-in-time)]
    (catch Exception _ [false])))

(defn parse-time-argument [ws-path bookmark-or-point-in-time]
  (let [[ok? timestamp] (parse-timestamp bookmark-or-point-in-time)]
    (if ok?
      timestamp
      (let [bookmarks (time-bookmarks ws-path)
            bookmark (keyword bookmark-or-point-in-time)
            point-in-time (bookmarks bookmark)]
        (or point-in-time 0)))))

(defn parse-time-args [ws-path [bookmark-or-point-in-time]]
  (let [time (if bookmark-or-point-in-time
               (parse-time-argument ws-path bookmark-or-point-in-time)
               (last-successful-build-time ws-path))]
    time))
