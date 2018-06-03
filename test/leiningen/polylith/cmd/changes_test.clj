(ns leiningen.polylith.cmd.changes-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-changes--list-interface-changes--returns-changed-interfaces
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "c" "comp2")
                   (polylith/polylith project
                                      "changes"
                                      "i"
                                      "cfd1ecc4aa6e6ca0646548aeabd22a4ee3b07419"
                                      "3014244d1be37651f33e22858b8ff0e8314b79f5"))
          interfaces (set (map str/trim (str/split output #"\n")))]
      (is (= #{"comp1" "comp2"}
             interfaces)))))

(deftest polylith-changes--list-component-changes--returns-changed-components
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "c" "comp2")
                   (polylith/polylith project
                                      "changes"
                                      "c"))
          components (set (map str/trim (str/split output #"\n")))]
      (is (= #{"comp1" "comp2"}
             components)))))

(deftest polylith-changes--include-all-history-changes--returns-all-components
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "c" "comp2")
                   (polylith/polylith project
                                      "changes"
                                      "c"
                                      "0"))
          components (set (map str/trim (str/split output #"\n")))]
      (is (= #{"comp1" "comp2"}
             components)))))

(deftest polylith-changes--list-base-changes--returns-changed-bases
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "c" "comp2")
                   (polylith/polylith project "create" "s" "sys1" "base1" "")
                   (polylith/polylith project "create" "s" "sys2" "base2" "")
                   (polylith/polylith project
                                      "changes"
                                      "b"))
          bases (set (map str/trim (str/split output #"\n")))]
      (is (= #{"base1" "base2"}
             bases)))))
