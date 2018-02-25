(ns leiningen.polylith.cmd.test-helper
  (:require [clojure.test :refer :all]
            [leiningen.polylith.file :as file]))

(defn content [ws-dir directory]
  (file/read-file (str ws-dir "/" directory)))
