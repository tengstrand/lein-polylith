(ns leiningen.polylith.cmd.add-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.add :as add]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-add--add-component-to-system--component-added
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")]

      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                         "create" "s" "sys1")
      (polylith/polylith project "create" "c" "comp1")
      (polylith/polylith project "add" "comp1" "sys1"))))
