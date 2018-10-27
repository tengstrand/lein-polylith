(ns leiningen.polylith.cmd.info-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest indirect-entity-changes--depends-on-something-that-depends-on-a-changed-component--returns-true
  (let [all-deps {"user"   #{"email"}
                  "email"  #{"common"}
                  "common" #{}}]
    (is (= [true]
           (info/indirect-entity-changes "user" #{"user"} all-deps #{"common"})))))

(deftest indirect-entity-changes--depends-on-x-that-depends-on-y-that--depends-on-a-changed-component--returns-true
  (let [all-deps {"user"   #{"x"}
                  "x"      #{"y"}
                  "y"      #{"z"}
                  "z" #{}}]
    (is (= [true]
           (info/indirect-entity-changes "user" #{"user"} all-deps #{"z"})))))

(deftest polylith-info--workspace-with-namespace--return-list-empty-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                    (helper/execute-polylith project "info"))]
      (is (= ["interfaces:"
              "components:"
              "bases:"
              "systems:"
              "environments:"
              "  development"]
             (helper/split-lines output))))))

(deftest polylith-info--workspace-with-namespace--return-list-with-change-information
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                    (helper/execute-polylith project "create" "c" "comp1" "ifc1")
                    (helper/execute-polylith project "create" "c" "component2")
                    (helper/execute-polylith project "create" "s" "sys1" "sys")
                    (helper/execute-polylith project "info"))]
      (is (= ["interfaces:"
              "  component2 *"
              "  ifc1 *"
              "components:"
              "  comp1 *        > ifc1"
              "  component2 *"
              "bases:"
              "  sys *"
              "systems:"
              "  sys1 *"
              "    sys *   -> base"
              "environments:"
              "  development"
              "    comp1 *        -> component"
              "    component2 *   -> component"
              "    sys *          -> base"]
             (helper/split-lines output))))))

(deftest polylith-info--workspace-without-namespace--return-list-with-change-information
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          output  (with-out-str
                    (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "s" "sys1" "sys")
                    (helper/execute-polylith project "info"))]
      (is (= ["interfaces:"
              "  comp1 *"
              "components:"
              "  comp1 *"
              "bases:"
              "  sys *"
              "systems:"
              "  sys1 *"
              "    sys *   -> base"
              "environments:"
              "  development"
              "    comp1 *   -> component"
              "    sys *     -> base"]
             (helper/split-lines output))))))

(deftest polylith-info--cyclic-dependencies-with-namespace--print-cyclic-dependencies
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
                         "  (component2/add-two 1))\n"]
          output        (with-out-str
                          (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                          (helper/execute-polylith project "create" "s" "system1" "base1")
                          (helper/execute-polylith project "create" "c" "component1" "interface1")
                          (helper/execute-polylith project "create" "c" "component2")
                          (helper/execute-polylith project "create" "c" "component3")
                          (helper/execute-polylith project "add" "component1" "system1")
                          (helper/execute-polylith project "add" "component2" "system1")
                          (helper/execute-polylith project "add" "component3" "system1")
                          (file/replace-file! (str ws-dir "/components/component1/src/my/company/interface1/core.clj") core1-content)
                          (file/replace-file! (str ws-dir "/components/component2/src/my/company/component2/core.clj") core2-content)
                          (file/replace-file! (str ws-dir "/components/component3/src/my/company/component3/core.clj") core3-content)
                          (file/replace-file! (str ws-dir "/bases/base1/src/my/company/base1/core.clj") base1-content)
                          (helper/execute-polylith project "info"))]
      (is (= ["interfaces:"
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
             (helper/split-lines output))))))
