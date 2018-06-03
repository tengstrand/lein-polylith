(ns leiningen.polylith.time
  (:require [clojure.pprint :as pp]
            [leiningen.polylith.file :as file]
            [clojure.string :as str])
  (:import (java.io FileNotFoundException)
           (java.util Date)
           (java.text SimpleDateFormat)))

(defn time-bookmarks [ws-path]
  (try
    (read-string (slurp (str ws-path "/.polylith/time.edn")))
    (catch FileNotFoundException _ {})))

(defn last-successful-build-time [ws-path]
   (or (:last-successful-build (time-bookmarks ws-path)
        0)))

(defn paths [ws-path]
  (filter #(not (or (str/includes? (str %) "/.")
                    (str/includes? (str %) "/target/")))
          (file/paths ws-path)))

(defn set-last-successful-build! [ws-path]
  (let [paths (paths ws-path)
        latest-change (file/latest-modified paths)
        bookmarks (assoc (time-bookmarks ws-path)
                    :last-successful-build latest-change)
        file (str ws-path "/.polylith/time.edn")]
    (pp/pprint bookmarks (clojure.java.io/writer file))))

(def formatter (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss"))

(defn ->time [timestamp]
  (let [time (.format formatter (Date. timestamp))]
    (str (subs time 0 10) " "
         (subs time 11 16) ":"
         (subs time 17 19))))

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

(defn parse-time-args [ws-path args]
  (let [bookmark-or-point-in-time (second args)
        time (if bookmark-or-point-in-time
               (parse-time-argument ws-path bookmark-or-point-in-time)
               (last-successful-build-time ws-path))]
    time))
