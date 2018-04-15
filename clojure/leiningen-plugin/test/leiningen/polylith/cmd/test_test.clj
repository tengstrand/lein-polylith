(ns leiningen.polylith.cmd.test-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [_ changes]
  (if (empty? changes)
    []
    ['comp1.a-test 'comp1.another-test]))

(deftest polylith-test--with-print-argument--print-tests
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/do-diff (fn [_ _] helper/diff)
                leiningen.polylith.cmd.test/tests fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "comp1")
                   (polylith/polylith project "test" "+"))]
      (is (= (str "  comp1.a-test\n"
                  "  comp1.another-test\n")
             output)))))

(deftest polylith-test--with-leiningen-argument--print-leiningen-execution-statement
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/do-diff (fn [_ _] helper/diff)
                leiningen.polylith.cmd.test/tests fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "comp1")
                   (polylith/polylith project "test" "-"))]
      (is (= "lein test comp1.a-test comp1.another-test\n"
             output)))))
