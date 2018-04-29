(ns leiningen.polylith.time
  (:require [clojure.pprint :as pp]
            [leiningen.polylith.file :as file]
            [clojure.string :as str])
  (:import (java.io FileNotFoundException)
           (java.util Date)
           (java.text SimpleDateFormat)))

(defn time-bookmarks [ws-path prefix]
  (try
    (read-string (slurp (str ws-path "/.polylith/time." (or prefix "local") ".edn")))
    (catch FileNotFoundException _ {})))

(defn last-successful-build-time
  ([ws-path prefix]
   (or (:last-successful-build (time-bookmarks ws-path prefix)
        0)))
  ([ws-path]
   (last-successful-build-time ws-path nil)))

(defn paths [ws-path]
  (filter #(not (or (str/includes? (str %) "/.")
                    (str/includes? (str %) "/target/")))
          (file/paths ws-path)))

(defn set-last-successful-build!
  ([ws-path prefix]
   (let [paths (paths ws-path)
         latest-change (file/latest-modified paths)
         bookmarks (assoc (time-bookmarks ws-path prefix)
                     :last-successful-build latest-change)
         file (str ws-path "/.polylith/time." (or prefix "local") ".edn")]
     (pp/pprint bookmarks (clojure.java.io/writer file))))
  ([ws-path]
   (set-last-successful-build! ws-path "local")))

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

(defn parse-time-argument [ws-path prefix bookmark-or-point-in-time]
  (let [[ok? timestamp] (parse-timestamp bookmark-or-point-in-time)]
    (if ok?
      timestamp
      (let [bookmarks (time-bookmarks ws-path prefix)
            bookmark (keyword bookmark-or-point-in-time)
            point-in-time (bookmarks bookmark)]
        (or point-in-time 0)))))

(defn parse-time-args [ws-path args]
  (let [plus? (contains? (set args) "+")
        without-plus (filter #(not= "+" %) args)
        prefix (first without-plus)
        bookmark-or-point-in-time (second without-plus)
        time (if bookmark-or-point-in-time
               (parse-time-argument ws-path prefix bookmark-or-point-in-time)
               (last-successful-build-time ws-path prefix))]
    [plus? time]))
