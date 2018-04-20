(ns leiningen.polylith.cmd.test-and-build
  (:require [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.build :as build]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.test :as test]))

(defn execute [ws-path top-dir args]
  (compile/execute ws-path top-dir args)
  (test/execute ws-path top-dir args)
  (build/execute ws-path top-dir args)
  (time/set-last-successful-build! ws-path))
