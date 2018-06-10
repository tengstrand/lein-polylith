(ns leiningen.polylith.time-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith])
  (:import (java.util Date)))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest set-last-successful-build!--test
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          _ (polylith/polylith nil "create" "w" "ws1" "-" "-git")
          _ (time/set-last-successful-build! ws-dir)
          build-time (time/last-successful-build-time ws-dir)]

      (is (> build-time 0)))))

(deftest ->time--test
  (let [date (Date. 118 5 10 6 40 34)]
    (is (= "2018-06-10 06:40:34"
           (time/->time (.getTime date))))))

(deftest parse-timestamp--number--returns-parsed-number
  (is (= [true 123]
         (time/parse-timestamp "123"))))

(deftest parse-timestamp--no-number--return-false
  (is (= [false]
         (time/parse-timestamp "x"))))

(deftest parse-time-argument--pass-time--returns-passed-time
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          _ (polylith/polylith nil "create" "w" "ws1" "-" "-git")
          mytime (time/parse-time-argument ws-dir "1528602169000")]

      (is (= 1528602169000
             mytime)))))

(deftest parse-time-argument--pass-bookmark--returns-time-for-bookmark
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          content ["{:last-successful-build 0"
                   " :mybookmark 1528602169000}"]
          _ (polylith/polylith nil "create" "w" "ws1" "-" "-git")
          _ (file/create-file (str ws-dir "/.polylith/time.edn") content)
          unknown (time/parse-time-argument ws-dir "unknown-bookmark")
          mytime (time/parse-time-argument ws-dir "mybookmark")]

      (is (zero? unknown))

      (is (= 1528602169000
             mytime)))))

(deftest parse-time-args--pass-no-args--returns-last-success
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          content ["{:last-successful-build 1528602169000}"]
          _ (polylith/polylith nil "create" "w" "ws1" "-" "-git")
          _ (file/create-file (str ws-dir "/.polylith/time.edn") content)
          last-success (time/parse-time-args ws-dir [])]

      (is (= 1528602169000
             last-success)))))

(deftest parse-time-args--pass-bookmark--returns-bookmark-time
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          content ["{:last-successful-build 1528602555000"
                   " :mybookmark 1528602777000}"]
          _ (polylith/polylith nil "create" "w" "ws1" "-" "-git")
          _ (file/create-file (str ws-dir "/.polylith/time.edn") content)
          bookmark (time/parse-time-args ws-dir ["mybookmark"])]

      (is (= 1528602777000
             bookmark)))))
