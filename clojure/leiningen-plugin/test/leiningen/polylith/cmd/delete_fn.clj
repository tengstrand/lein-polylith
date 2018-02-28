(ns leiningen.polylith.cmd.delete-fn
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :refer [settings]]
            [clojure.string :as str]))

(defn delete-component-with-ns [ws-dir]
  (let [project (settings ws-dir "my.company" "my/company")]
    (polylith/polylith nil "create" "w" "ws1" "my.company")
    (polylith/polylith project
                       "create" "c" "comp1")
    (polylith/polylith project "delete" "c" "comp1")))
