(ns leiningen.polylith.cmd.compile-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-compile--with-print-argument--print-tests
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "compile"))]
      (is (= (str "\n"
                  "Changed components: comp1\n"
                  "Changed bases:\n"
                  "Changed systems:\n"
                  "\n"
                  "Compiling interfaces\n"
                  "(lein install :dir " ws-dir "/interfaces)\n"
                  "Compiling components/comp1\n"
                  "(lein compile :dir " ws-dir "/components/comp1)\n")
             output)))))
