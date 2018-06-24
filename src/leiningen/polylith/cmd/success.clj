(ns leiningen.polylith.cmd.success
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.git :as git]
            [leiningen.polylith.time :as time]))

(defn bookmark? [ws-path bookmark]
  (when-not (nil? bookmark)
    (let [[timestamp? _] (time/parse-timestamp bookmark)]
      (not (or timestamp? (git/valid-sha1? ws-path bookmark))))))

(defn execute [ws-path [bookmark]]
  (let [bookmark (if (bookmark? ws-path bookmark)
                   (keyword bookmark)
                   :last-successful-build)]
    (if (shared/ci?)
      (git/set-bookmark! ws-path bookmark)
      (time/set-bookmark! ws-path bookmark))))
