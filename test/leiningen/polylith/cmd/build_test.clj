(ns leiningen.polylith.cmd.build-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-build--build-changed-systems--print-output
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
                  "Start execution of tests in 2 namespaces:\n"
                  "lein test my.company.base1.core-test my.company.comp1.core-test\n"
                  "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)\n"
                  "Building systems/system1\n"
                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
             output))
      (is (< 0 (-> (helper/content ws-dir ".polylith/time.local.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-compile-and-build-changed-systems--print-output
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "s" "system1" "base1")
                   (polylith/polylith project "build" "-compile"))]
      (is (= (str "\n"
                  "Changed systems: system1\n"
                  "\n"
                  "Start execution of tests in 2 namespaces:\n"
                  "lein test my.company.base1.core-test my.company.comp1.core-test\n"
                  "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)\n"
                  "Building systems/system1\n"
                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
             output))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.local.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-test-and-build-changed-systems--print-output
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "s" "system1" "base1")
                   (polylith/polylith project "build" "-test"))]
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
                  "Building systems/system1\n"
                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
             output))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.local.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-success-and-build-changed-systems--print-output
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "s" "system1" "base1")
                   (polylith/polylith project "build" "-success"))]
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
                  "Start execution of tests in 2 namespaces:\n"
                  "lein test my.company.base1.core-test my.company.comp1.core-test\n"
                  "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)\n"
                  "Building systems/system1\n"
                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
             output))

      (is (= 0 (-> (helper/content ws-dir ".polylith/time.local.edn")
                   first :last-successful-build))))))

(deftest polylith-build--with-prefix-and-build-changed-systems--print-output
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "create" "s" "system1" "base1")
                   (polylith/polylith project "build" "remote"))]
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
                  "Start execution of tests in 2 namespaces:\n"
                  "lein test my.company.base1.core-test my.company.comp1.core-test\n"
                  "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)\n"
                  "Building systems/system1\n"
                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
             output))

      (is (= 0 (-> (helper/content ws-dir ".polylith/time.local.edn")
                   first :last-successful-build)))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.remote.edn")
                   first :last-successful-build))))))
