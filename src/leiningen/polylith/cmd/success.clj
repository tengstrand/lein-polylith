(ns leiningen.polylith.cmd.success
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.git :as git]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.utils :as utils]))

(defn execute [ws-path [bookmark]]
  (let [bookmark (if (utils/bookmark? ws-path bookmark)
                   (keyword bookmark)
                   :last-success)]
    (if (shared/ci?)
      (git/set-bookmark! ws-path bookmark)
      (time/set-bookmark! ws-path bookmark))))
