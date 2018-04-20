(ns leiningen.polylith.cmd.success
  (:require [leiningen.polylith.time :as time]))

(defn execute [ws-path]
  (time/set-last-successful-build! ws-path))
