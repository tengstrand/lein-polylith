(ns leiningen.polylith.cmd.success-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-success--local--print-to-time
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")]
      (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
      (polylith/polylith project "create" "c" "comp1")
      (polylith/polylith project "create" "s" "system1" "base1")
      (polylith/polylith project "success")

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-success--ci--print-to-git
  (try
    (with-redefs [file/current-path (fn [] @helper/root-dir)]
      (System/setProperty "CI" "CIRCLE")
      (let [ws-dir  (str @helper/root-dir "/ws1")
            project (helper/settings ws-dir "my.company")]
        (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
        (polylith/polylith project "create" "c" "comp1")
        (polylith/polylith project "create" "s" "system1" "base1")
        (shared/sh "git" "init" :dir ws-dir)
        (shared/sh "git" "add" "." :dir ws-dir)
        (shared/sh "git" "commit" "-m" "Initial Commit" :dir ws-dir)
        (polylith/polylith project "success")
        (System/clearProperty "CI")

        (is (not (nil? (-> (helper/content ws-dir ".polylith/git.edn")
                           first :last-successful-build))))))
    (catch Exception _
      (System/clearProperty "CI"))))
