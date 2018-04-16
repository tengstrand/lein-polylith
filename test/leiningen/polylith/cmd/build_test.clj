(ns leiningen.polylith.cmd.build-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-build--with-print-argument--print-tests
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "s" "system1" "base1")
                   (polylith/polylith project "build"))]
      (is (= (str "\n"
                  "Changed systems: system1\n"
                  "\n"
                  "Building systems/system1\n"
                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
             output)))))
