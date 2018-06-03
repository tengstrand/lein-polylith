(ns leiningen.polylith.cmd.success-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-success--without-arg--print-to-time
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          _       (polylith/polylith nil "create" "w" "ws1" "my.company")
          _       (polylith/polylith project "create" "c" "comp1")
          _       (polylith/polylith project "create" "s" "system1" "base1")
          _       (polylith/polylith project "success")]

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))
