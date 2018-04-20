(ns leiningen.polylith.cmd.test-and-build-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.time :as time]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-test-and-build--with-print-argument--print-tests
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir                       (str @helper/root-dir "/ws1")
          project                      (helper/settings ws-dir "my.company")
          _                            (polylith/polylith nil "create" "w" "ws1" "my.company")
          _                            (polylith/polylith project "create" "c" "comp1")
          _                            (polylith/polylith project "create" "s" "system1" "base1")
          last-successful-build-before (time/last-successful-build-time ws-dir)
          output-before                (with-out-str
                                         (polylith/polylith project "changes" "c")
                                         (polylith/polylith project "changes" "s"))
          output                       (with-out-str
                                         (polylith/polylith project "test-and-build"))
          last-successful-build-after  (time/last-successful-build-time ws-dir)
          output-after                 (with-out-str
                                         (polylith/polylith project "changes" "c")
                                         (polylith/polylith project "changes" "s"))]
      (is (= 0 last-successful-build-before))
      (is (= (str " comp1\n"
                  " system1\n")
             output-before))
      (is (= (str "\n"
                  "Changed components: comp1\n"
                  "Changed bases: base1\n"
                  "Changed systems: system1\n"
                  "\n"
                  "Compiling interfaces\n"
                  "(lein install :dir " ws-dir "/interfaces)\n"
                  "Compiling components/comp1\n"
                  "(lein compile :dir " ws-dir "/components/comp1)\n"
                  "Compiling bases/base1\n"
                  "(lein compile :dir " ws-dir "/bases/base1)\n"
                  "Compiling systems/system1\n"
                  "(lein compile :dir " ws-dir "/systems/system1)\n"
                  "Start execution of 2 tests:\n"
                  "lein test my.company.base1.core-test my.company.comp1.core-test\n"
                  "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)\n"
                  "\n"
                  "Changed systems: system1\n"
                  "\n"
                  "Building systems/system1\n"
                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
             output))
      (is (< 0 last-successful-build-after))
      (is (= "" output-after)))))
