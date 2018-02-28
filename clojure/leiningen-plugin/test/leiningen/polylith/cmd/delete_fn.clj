(ns leiningen.polylith.cmd.delete-fn
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :refer [settings]]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]))

(defn delete-component-with-ns [ws-dir]
  (let [project (settings ws-dir "my.company" "my/company")]
    (polylith/polylith nil "create" "w" "ws1" "my.company")
    (polylith/polylith project
                       "create" "c" "comp1")
    (polylith/polylith project "delete" "c" "comp1")

    (is (empty? (file/files (str ws-dir "/components"))))))

(defn delete-component-without-ns [ws-dir]
  (let [project (settings ws-dir "" "")]
    (polylith/polylith nil "create" "w" "ws1" "")
    (polylith/polylith project
                       "create" "c" "comp1")
    (polylith/polylith project "delete" "c" "comp1")

    (is (empty? (file/files (str ws-dir "/components"))))))
