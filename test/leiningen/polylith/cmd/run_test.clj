(ns leiningen.polylith.cmd.run-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]))

(use-fixtures :each helper/test-setup-and-tear-down)

(def time-atom (atom 0))

(defn fake-current-time []
  (swap! time-atom inc)
  (* @time-atom 1200))

(deftest run--execute-missing-system--print-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "run"))]

      (is (= "The system 'sys-1' does not exist.\n"
             output)))))

(deftest run--execute-missing-system--print-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "run"))]

      (is (= "Missing system name.\n"
             output)))))

(deftest run--execute-missing-jar--print-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "s" "sys-1" "base-1")
                    (polylith/polylith project "run" "sys-1"))]

      (is (= "No standalone jar found for 'sys-1'. Try execute 'build' first.\n"
             output)))))

(deftest run--build-standalone-jar-and-execute-it--print-out-result
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                time/current-time fake-current-time]
    (let [ws-dir            (str @helper/root-dir "/ws1")
          project           (helper/settings ws-dir "my.company")
          core-content      ["(ns my.company.base-1.core\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& [name]]\n"
                             "  (println \"Hello\" name))"]
          core-test-content ["(ns my.company.base-1.core-test)"]
          output            (with-out-str
                              (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                              (polylith/polylith project "create" "s" "sys-1" "base-1")
                              (file/replace-file! (str ws-dir "/bases/base-1/src/my/company/base_1/core.clj") core-content)
                              (file/replace-file! (str ws-dir "/bases/base-1/test/my/company/base_1/core_test.clj") core-test-content)
                              (polylith/polylith project "build")
                              (polylith/polylith project "run" "sys-1" "Buddy!"))
          out               (helper/split-lines output)
          index             (first (keep-indexed #(if (= "Execution time: 3.6 seconds" %2) %1) out))]

      (is (= ["Hello Buddy!"]
             (keep-indexed #(if (> %1 index) %2) out))))))
