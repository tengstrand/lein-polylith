(ns leiningen.polylith.cmd.info-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

;; todo: extend this test when we have support for adding bases and systems.
(deftest polylith-info--components-bases-and-systems-with-no-vcs-info--return-list-without-vcs-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                                      "create" "c" "comp1")
                   (polylith/polylith project "info"))]
      (is (= (str "components:\n"
                  "  comp1\n"
                  "bases:\n"
                  "systems:\n")
             output)))))

(deftest polylith-info--interfaces-components-bases-and-systems-with-no-vcs-info--return-list-without-vcs-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                                      "create" "c" "comp1")
                   (polylith/polylith project "info" "a"))]
      (is (= (str "interfaces:\n"
                  "  comp1\n"
                  "components:\n"
                  "  comp1\n"
                  "bases:\n"
                  "systems:\n")
             output)))))

(deftest polylith-info--components-bases-and-systems-with-vcs-info--return-list-with-vcs-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                                      "create" "c" "comp1")
                   (polylith/polylith project "info" "cfd1ecc4aa6e6ca0646548aeabd22a4ee3b07419" "3014244d1be37651f33e22858b8ff0e8314b79f5"))]
      (is (= (str "components:\n"
                  "  comp1 *\n"
                  "bases:\n"
                  "systems:\n")
             output)))))

(deftest polylith-info--changed-interfaces-components-bases-and-systems-with-vcs-info--return-unchanged
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                                      "create" "c" "comp1")
                   (polylith/polylith project "info" "u" "cfd1ecc4aa6e6ca0646548aeabd22a4ee3b07419" "3014244d1be37651f33e22858b8ff0e8314b79f5"))]
      (is (= (str "interfaces:\n"
                  "  comp1\n"
                  "components:\n"
                  "bases:\n"
                  "systems:\n")
             output)))))

