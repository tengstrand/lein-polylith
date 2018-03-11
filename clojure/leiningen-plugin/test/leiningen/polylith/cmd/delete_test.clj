(ns leiningen.polylith.cmd.delete-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.test-helper :as helper]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-delete--delete-component--deletes-a-component-with-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")]
      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith project "create" "c" "comp1")
      (polylith/polylith project "delete" "c" "comp1")
      (polylith/polylith project "create" "c" "comp1")
      (polylith/polylith project "delete" "c" "comp1")

      (is (empty? (file/files (str ws-dir "/components")))))))

(deftest polylith-delete--delete-component--deletes-a-component-without-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "" "")]
      (polylith/polylith nil "create" "w" "ws1" "")
      (polylith/polylith project
                         "create" "c" "comp1")
      (polylith/polylith project "delete" "c" "comp1")

      (is (empty? (file/files (str ws-dir "/components")))))))
