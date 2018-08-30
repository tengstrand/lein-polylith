(ns leiningen.polylith.cmd.test-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-test--one-ns-changed-and-skip-compile--component-for-changed-ns-was-executed
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "test" "-compile"))]
      (is (= ["Start execution of tests in 1 namespaces:"
              "lein test my.company.comp1.core-test"
              (str "(lein test my.company.comp1.core-test :dir " ws-dir "/environments/development)")]
             (helper/split-lines output))))))

(deftest polylith-test--one-ns-changed--component-for-changed-ns-was-executed
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "test"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases:"
              "Changed systems:"
              ""
              "Compiling workspace interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Start execution of tests in 1 namespaces:"
              "lein test my.company.comp1.core-test"
              (str "(lein test my.company.comp1.core-test :dir " ws-dir "/environments/development)")]
             (helper/split-lines output))))))

(deftest polylith-test--one-ns-changed--component-for-referencing-component-also-executed
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir        (str @helper/root-dir "/ws1")
          project       (helper/settings ws-dir "my.company")
          base1-content ["(ns my.company.system-1.core"
                         "  (:require [my.company.comp-1.interface :as comp1])"
                         "  (:gen-class))\n\n(defn -main [& args]"
                         "  (comp1/add-two 10))"]
          core1-content ["(ns my.company.comp-1.core)"
                         "(defn add-two [x]"
                         "  (+ 2 x))"]
          core2-content ["(ns my.company.comp-2.core"
                         "  (:require [my.company.comp-1.interface :as comp1]))"
                         "(defn add-two [x]"
                         "  (comp1/add-two x))"]
          output        (with-out-str
                          (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                          (polylith/polylith project "create" "s" "system-1" "base-1")
                          (polylith/polylith project "create" "c" "comp-1")
                          (polylith/polylith project "create" "c" "comp-2")
                          (polylith/polylith project "add" "comp-1" "system-1")
                          (polylith/polylith project "add" "comp-2" "system-1")
                          (file/replace-file! (str ws-dir "/bases/base-1/src/my/company/base_1/core.clj") base1-content)
                          (file/replace-file! (str ws-dir "/components/comp-2/src/my/company/comp_2/core.clj") core2-content)
                          (polylith/polylith project "success")
                          ;; The file system updated the timestamp once per second (at least on Mac!)
                          (Thread/sleep 1000)
                          (file/replace-file! (str ws-dir "/components/comp-1/src/my/company/comp_1/core.clj") core1-content)
                          (polylith/polylith project "info")
                          (polylith/polylith project "test"))]
      (is (= ["  set :last-successful-build in .polylith/time.edn"
              "interfaces:"
              "  comp-1"
              "  comp-2"
              "components:"
              "  comp-1 *"
              "  comp-2 (*)"
              "bases:"
              "  base-1 (*)"
              "systems:"
              "  system-1"
              "    comp-1 *    -> component"
              "    comp-2 (*)  -> component"
              "    base-1 (*)  -> base"
              "environments:"
              "  development"
              "    comp-1 *    -> component"
              "    comp-2 (*)  -> component"
              "    base-1 (*)  -> base"
              ""
              "Changed components: comp-1"
              "Changed bases:"
              "Changed systems: system-1"
              ""
              "Compiling workspace interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp-1"
              (str "(lein compile :dir " ws-dir "/components/comp-1)")
              "Compiling systems/system-1"
              (str "(lein compile :dir " ws-dir "/systems/system-1)")

              "Start execution of tests in 3 namespaces:"
              "lein test my.company.base-1.core-test my.company.comp-1.core-test my.company.comp-2.core-test"
              (str "(lein test my.company.base-1.core-test my.company.comp-1.core-test my.company.comp-2.core-test :dir " ws-dir "/environments/development)")]
             (helper/split-lines output))))))

(deftest polylith-test--one-ns-changed-and-skip-compile-plus-success--component-for-changed-ns-was-executed-and-last-successful-test-saved
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "test" "-compile" "+success"))]
      (is (= ["Start execution of tests in 1 namespaces:"
              "lein test my.company.comp1.core-test"
              (str "(lein test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "  set :last-successful-test in .polylith/time.edn"]
             (helper/split-lines output)))
      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-test))))))

(deftest polylith-test--one-ns-changed-and-skip-compile-plus-success-and-bookmark--component-for-changed-ns-was-executed-and-given-bookmark-saved
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "test" "my-bookmark" "-compile" "+success"))]
      (is (= ["Start execution of tests in 1 namespaces:"
              "lein test my.company.comp1.core-test"
              (str "(lein test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "  set :my-bookmark in .polylith/time.edn"]
             (helper/split-lines output)))
      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :my-bookmark))))))

(deftest polylith-test--cyclic-dependencies-with-namespace--print-info
 (with-redefs [file/current-path (fn [] @helper/root-dir)]
   (let [ws-dir        (str @helper/root-dir "/ws1")
          project       (helper/settings ws-dir "my.company")
          core1-content ["(ns my.company.component1.core"
                         "  (:require [my.company.component3.interface :as component3]))"
                         "(defn add-two [x]"
                         "  (component3/add-two x))"]
          core2-content ["(ns my.company.component2.core"
                         "  (:require [my.company.interface1.interface :as interface1]))"
                         "(defn add-two [x]"
                         "  (interface1/add-two x))"]
          core3-content ["(ns my.company.component3.core"
                         "  (:require [my.company.component2.interface :as component2]))"
                         "(defn add-two [x]"
                         "  (component2/add-two x))"]
          base1-content ["(ns my.company.base1.core"
                         "  (:require [my.company.component2.interface :as component2])"
                         "  (:gen-class))\n\n(defn -main [& args]"
                         "  (component2/add-two 1))"]
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
