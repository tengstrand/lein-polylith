(ns leiningen.polylith.cmd.success
  (:require [leiningen.polylith.time :as time]))

;; todo: remove me when we have a proper solution!
(defn execute [ws-path]
  (time/set-last-successful-build! ws-path))
