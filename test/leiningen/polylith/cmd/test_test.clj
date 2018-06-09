(ns leiningen.polylith.cmd.test-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-test--one-ns-changed-and-skip-compile--component-for-changed-ns-was-executed
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "test" "-compile"))]
      (is (= ["Start execution of tests in 1 namespaces:"
              "lein test my.company.comp1.core-test"
              (str "(lein test my.company.comp1.core-test :dir " ws-dir "/environments/development)")]
             (helper/split-lines output))))))

(deftest polylith-test--one-ns-changed--component-for-changed-ns-was-executed
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "test"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases:"
              "Changed systems:"
              ""
              "Compiling interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Start execution of tests in 1 namespaces:"
              "lein test my.company.comp1.core-test"
              (str "(lein test my.company.comp1.core-test :dir " ws-dir "/environments/development)")]
             (helper/split-lines output))))))

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
                   (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                   (polylith/polylith project "create" "c" "comp-1")
                   (polylith/polylith project "create" "c" "comp-2")
                   (file/replace-file! (str ws-dir "/components/comp-2/src/my/company/comp_2/core.clj") core2-content)
                   (polylith/polylith project "success")
                   ;; The file system updated the timestamp once per second (at least on Mac!)
                   (Thread/sleep 1000)
                   (file/replace-file! (str ws-dir "/components/comp-1/src/my/company/comp_1/core.clj") core1-content)
                   (polylith/polylith project "info")
                   (polylith/polylith project "test"))]
      (is (= ["interfaces:"
              "  comp-1"
              "  comp-2"
              "components:"
              "  comp-1 *"
              "  comp-2 (*)"
              "bases:"
              "systems:"
              "environments:"
              "  development"
              "    comp-1 *    -> component"
              "    comp-2 (*)  -> component"
              ""
              "Changed components: comp-1"
              "Changed bases:"
              "Changed systems:"
              ""
              "Compiling interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp-1"
              (str "(lein compile :dir " ws-dir "/components/comp-1)")
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.comp-1.core-test my.company.comp-2.core-test"
              (str "(lein test my.company.comp-1.core-test my.company.comp-2.core-test :dir " ws-dir "/environments/development)")]
             (helper/split-lines output))))))

(deftest polylith-test--cyclic-dependencies-with-namespace--print-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
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
          exception (atom nil)
          output (with-out-str
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
                     (polylith/polylith project "test")
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
