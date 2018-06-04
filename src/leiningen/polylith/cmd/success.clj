(ns leiningen.polylith.cmd.success
  (:require [leiningen.polylith.time :as time]
            [leiningen.polylith.git :as git]))

(defn execute [ws-path]
  (if (System/getProperty "CI")
    (git/set-last-successful-build! ws-path)
    (time/set-last-successful-build! ws-path)))
