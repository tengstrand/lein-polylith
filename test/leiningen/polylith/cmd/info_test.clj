(ns leiningen.polylith.cmd.info-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest indirect-entity-changes--depends-on-something-that-depends-on-a-changed-component--returns-true
  (let [all-deps {"user" #{"email"}
                  "email" #{"common"}
                  "common" #{}}]
    (is (= [true]
           (info/indirect-entity-changes "user" #{"user"} all-deps #{"common"})))))

(deftest polylith-info--workspace-with-namespace--return-list-empty-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "info"))]
      (is (= (str "interfaces:\n"
                  "components:\n"
                  "bases:\n"
                  "systems:\n"
                  "environments:\n"
                  "  development\n")
             output)))))

(deftest polylith-info--workspace-with-namespace--return-list-with-change-information
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "comp1" "ifc1")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "component2")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "s" "sys1" "sys")
                   (polylith/polylith project "info"))]
      (is (= (str "interfaces:\n"
                  "  component2 *\n"
                  "  ifc1 *\n"
                  "components:\n"
                  "  comp1 *        > ifc1\n"
                  "  component2 *\n"
                  "bases:\n"
                  "  sys *\n"
                  "systems:\n"
                  "  sys1 *\n"
                  "    sys *   -> base\n"
                  "environments:\n"
                  "  development\n"
                  "    comp1 *        -> component\n"
                  "    component2 *   -> component\n"
                  "    sys *          -> base\n")
             output)))))

(deftest polylith-info--workspace-without-namespace--return-list-with-change-information
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "")
                   (polylith/polylith (helper/settings ws-dir "")
                                      "create" "c" "comp1")
                   (polylith/polylith (helper/settings ws-dir "")
                                      "create" "s" "sys1" "sys")
                   (polylith/polylith project "info"))]
      (is (= (str "interfaces:\n"
                  "  comp1 *\n"
                  "components:\n"
                  "  comp1 *\n"
                  "bases:\n"
                  "  sys *\n"
                  "systems:\n"
                  "  sys1 *\n"
                  "    sys *   -> base\n"
                  "environments:\n"
                  "  development\n"
                  "    comp1 *   -> component\n"
                  "    sys *     -> base\n")
             output)))))

(deftest polylith-info--cyclic-dependencies-with-namespace--print-cyclic-dependencies
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
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "s" "system1")
                   (polylith/polylith project "create" "c" "component1" "interface1")
                   (polylith/polylith project "create" "c" "component2")
                   (polylith/polylith project "create" "c" "component3")
                   (polylith/polylith project "add" "component1" "system1")
                   (polylith/polylith project "add" "component2" "system1")
                   (polylith/polylith project "add" "component3" "system1")
                   (file/replace-file (str ws-dir "/components/component1/src/my/company/component1/core.clj") core1-content)
                   (file/replace-file (str ws-dir "/components/component2/src/my/company/component2/core.clj") core2-content)
                   (file/replace-file (str ws-dir "/components/component3/src/my/company/component3/core.clj") core3-content)
                   (polylith/polylith project "info"))]
      (is (= (str "interfaces:\n"
                  "  component2 *\n"
                  "  component3 *\n"
                  "  interface1 *\n"
                  "components:\n"
                  "  component1 *   > interface1\n"
                  "  component2 *\n"
                  "  component3 *\n"
                  "bases:\n"
                  "  system1 *\n"
                  "systems:\n"
                  "  system1 *\n"
                  "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)\n"
                  "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)\n"
                  "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)\n"
                  "    system1 *      -> base\n"
                  "environments:\n"
                  "  development\n"
                  "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)\n"
                  "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)\n"
                  "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)\n"
                  "    system1 *      -> base\n")
             output)))))
