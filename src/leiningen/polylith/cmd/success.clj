(ns leiningen.polylith.cmd.success
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.git :as git]
            [leiningen.polylith.time :as time]))

(defn execute [ws-path]
  (if (shared/ci?)
    (git/set-last-successful-build! ws-path)
    (time/set-last-successful-build! ws-path)))
