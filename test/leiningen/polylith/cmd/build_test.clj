(ns leiningen.polylith.cmd.build-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-build--build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Compiling bases/base1"
              (str "(lein compile :dir " ws-dir "/bases/base1)")
              "Compiling systems/system1"
              (str "(lein compile :dir " ws-dir "/systems/system1)")
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.base1.core-test my.company.comp1.core-test"
              (str "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))
      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-compile-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build" "-compile"))]
      (is (= [""
              "Changed systems: system1"
              ""
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.base1.core-test my.company.comp1.core-test"
              (str "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-test-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build" "-test"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Compiling bases/base1"
              (str "(lein compile :dir " ws-dir "/bases/base1)")
              "Compiling systems/system1"
              (str "(lein compile :dir " ws-dir "/systems/system1)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-success-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build" "-success"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Compiling bases/base1"
              (str "(lein compile :dir " ws-dir "/bases/base1)")
              "Compiling systems/system1"
              (str "(lein compile :dir " ws-dir "/systems/system1)")
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.base1.core-test my.company.comp1.core-test"
              (str "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))

      (is (= 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

;(deftest polylith-build--with-prefix-and-build-changed-systems--print-output
;  (with-redefs [file/current-path (fn [] @helper/root-dir)
;                leiningen.polylith.cmd.shared/sh fake-fn]
;    (let [ws-dir (str @helper/root-dir "/ws1")
;          project (helper/settings ws-dir "my.company")
;          output (with-out-str
;                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
;                   (polylith/polylith project "create" "c" "comp1")
;                   (polylith/polylith project "create" "s" "system1" "base1")
;                   (polylith/polylith project "build" "remote"))]
;      (is (= (str "\n"
;                  "Changed components: comp1\n"
;                  "Changed bases: base1\n"
;                  "Changed systems: system1\n"
;                  "\n"
;                  "Compiling interfaces\n"
;                  "(lein install :dir " ws-dir "/interfaces)\n"
;                  "Compiling components/comp1\n"
;                  "(lein compile :dir " ws-dir "/components/comp1)\n"
;                  "Compiling bases/base1\n"
;                  "(lein compile :dir " ws-dir "/bases/base1)\n"
;                  "Compiling systems/system1\n"
;                  "(lein compile :dir " ws-dir "/systems/system1)\n"
;                  "Start execution of tests in 2 namespaces:\n"
;                  "lein test my.company.base1.core-test my.company.comp1.core-test\n"
;                  "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)\n"
;                  "Building systems/system1\n"
;                  "(./build.sh :dir " ws-dir "/systems/system1)\n")
;             output))
;
;      (is (= 0 (-> (helper/content ws-dir ".polylith/time.local.edn")
;                   first :last-successful-build)))
;
;      (is (< 0 (-> (helper/content ws-dir ".polylith/time.remote.edn")
;                   first :last-successful-build))))))

(deftest polylith-build--cyclic-dependencies-with-namespace--print-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir        (str @helper/root-dir "/ws1")
          project       (helper/settings ws-dir "my.company")
          core1-content [(str "(ns my.company.component1.core\n"
                              "  (:require [my.company.component3.interface :as component3]))\n\n"
                              "(defn add-two [x]\n"
                              "  (component3/add-two x))")]
          core2-content [(str "(ns my.company.component2.core\n"
                              "  (:require [my.company.interface1.interface :as interface1]))\n\n"
                              "(defn add-two [x]\n"
                              "  (interface1/add-two x))")]
          core3-content [(str "(ns my.company.component3.core\n"
                              "  (:require [my.company.component2.interface :as component2]))\n\n"
                              "(defn add-two [x]\n"
                              "  (component2/add-two x))")]
          base1-content [(str "(ns my.company.base1.core\n"
                              "  (:require [my.company.component2.interface :as component2])\n"
                              "  (:gen-class))\n\n(defn -main [& args]\n"
                              "  (component2/add-two 1))\n")]
          exception     (atom nil)
          output        (with-out-str
                          (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                          (polylith/polylith project "create" "s" "system1" "base1")
                          (polylith/polylith project "create" "c" "component1" "interface1")
                          (polylith/polylith project "create" "c" "component2")
                          (polylith/polylith project "create" "c" "component3")
                          (polylith/polylith project "add" "component1" "system1")
                          (polylith/polylith project "add" "component2" "system1")
                          (polylith/polylith project "add" "component3" "system1")
                          (file/replace-file! (str ws-dir "/components/component1/src/my/company/component1/core.clj") core1-content)
                          (file/replace-file! (str ws-dir "/components/component2/src/my/company/component2/core.clj") core2-content)
                          (file/replace-file! (str ws-dir "/components/component3/src/my/company/component3/core.clj") core3-content)
                          (file/replace-file! (str ws-dir "/bases/base1/src/my/company/base1/core.clj") base1-content)
                          (try
                            (polylith/polylith project "build")
                            (catch Exception e
                              (swap! exception conj e))))]

      (is (= ["Cannot compile: circular dependencies detected."
              ""
              "interfaces:"
              "  component2 *"
              "  component3 *"
              "  interface1 *"
              "components:"
              "  component1 *   > interface1"
              "  component2 *"
              "  component3 *"
              "bases:"
              "  base1 *"
              "systems:"
              "  system1 *"
              "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)"
              "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)"
              "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)"
              "    base1 *        -> base       (circular deps: base1 > component2 > component1 > component3 > component2)"
              "environments:"
              "  development"
              "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)"
              "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)"
              "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)"
              "    base1 *        -> base       (circular deps: base1 > component2 > component1 > component3 > component2)"]
             (helper/split-lines output)))

      (is (= "Cannot compile: circular dependencies detected." (-> @exception first .getLocalizedMessage))))))
