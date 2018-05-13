(ns leiningen.polylith.cmd.compile-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-compile--compile-a-system--system-compiled
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "s" "system1")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "add" "comp1" "system1")
                   (polylith/polylith project "compile"))]
      (is (= (str "\n"
                  "Changed components: comp1\n"
                  "Changed bases: system1\n"
                  "Changed systems: system1\n\n"
                  "Compiling interfaces\n"
                  "(lein install :dir " ws-dir "/interfaces)\n"
                  "Compiling components/comp1\n"
                  "(lein compile :dir " ws-dir "/components/comp1)\n"
                  "Compiling bases/system1\n"
                  "(lein compile :dir " ws-dir "/bases/system1)\n"
                  "Compiling systems/system1\n"
                  "(lein compile :dir " ws-dir "/systems/system1)\n")
             output)))))

(deftest polylith-compile--compile-a-system-with-a-base-that-has-another-namespace--system-compiled
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "s" "system1" "system1" "com.abc")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "add" "comp1" "system1")
                   (polylith/polylith project "compile"))]
      (is (= (str "\n"
                  "Changed components: comp1\n"
                  "Changed bases: system1\n"
                  "Changed systems: system1\n\n"
                  "Compiling interfaces\n"
                  "(lein install :dir " ws-dir "/interfaces)\n"
                  "Compiling components/comp1\n"
                  "(lein compile :dir " ws-dir "/components/comp1)\n"
                  "Compiling bases/system1\n"
                  "(lein compile :dir " ws-dir "/bases/system1)\n"
                  "Compiling systems/system1\n"
                  "(lein compile :dir " ws-dir "/systems/system1)\n")
             output)))))

