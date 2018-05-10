(ns leiningen.polylith.cmd.test-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-test--one-ns-changed--component-for-changed-ns-was-executed
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "test"))]
      (is (= (str "Start execution of tests in 1 namespaces:\n"
                  "lein test my.company.comp1.core-test\n"
                  "(lein test my.company.comp1.core-test :dir " ws-dir "/environments/development)\n")
             output)))))

(deftest polylith-test--one-ns-changed--component-for-referencing-component-also-executed
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          core1-content [(str "(ns my.company.comp-1.core)\n\n"
                              "(defn add-two [x]\n"
                              "  (+ 2 x))\n")]
          core2-content [(str "(ns my.company.comp-2.core\n"
                              "  (:require [my.company.comp-1.interface :as comp1]))\n\n"
                              "(defn add-two [x]\n"
                              "  (comp1/add-two x))\n")]
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp-1")
                   (polylith/polylith project "create" "c" "comp-2")
                   (file/replace-file! (str ws-dir "/components/comp-2/src/my/company/comp_2/core.clj") core2-content)
                   (polylith/polylith project "success")
                   ;; The file system updated the timestamp once per second (at least on Mac!)
                   (Thread/sleep 1000)
                   (file/replace-file! (str ws-dir "/components/comp-1/src/my/company/comp_1/core.clj") core1-content)
                   (polylith/polylith project "info")
                   (polylith/polylith project "test"))]
      (is (= (str "interfaces:\n"
                  "  comp-1\n"
                  "  comp-2\n"
                  "components:\n"
                  "  comp-1 *\n"
                  "  comp-2 (*)\n"
                  "bases:\n"
                  "systems:\n"
                  "environments:\n"
                  "  development\n"
                  "    comp-1 *   -> component\n"
                  "    comp-2 (*) -> component\n"

                  "Start execution of tests in 2 namespaces:\n"
                  "lein test my.company.comp-1.core-test my.company.comp-2.core-test\n"
                  "(lein test my.company.comp-1.core-test my.company.comp-2.core-test :dir " ws-dir "/environments/development)\n")
             output)))))
