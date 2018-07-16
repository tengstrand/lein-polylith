(ns leiningen.polylith.utils
  (:require [clojure.string :as str]
            [leiningen.polylith.git :as git]
            [leiningen.polylith.time :as time]))

(defn is-empty-str? [value]
  (or (nil? value) (str/blank? value)))

(defn bookmark? [ws-path bookmark]
  (when-not (nil? bookmark)
    (let [[timestamp? _] (time/parse-timestamp bookmark)]
      (not (or timestamp? (git/valid-sha1? ws-path bookmark))))))
