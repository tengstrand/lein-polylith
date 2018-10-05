(ns leiningen.polylith.cmd.deps-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-deps--try-filter-on-unknown-entity--print-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                         (helper/execute-polylith project "create" "c" "comp-1a" "interface-1")
                         (helper/execute-polylith project "deps" "x"))]
      (is (= ["Couldn't show dependencies for 'x'. It's not a system, environment, base or component."]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace--print-interface-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content ["(ns my.company.comp-2.core"
                        "  (:require [my.company.interface-1.interface :as interface1]))"
                        "(defn add-two [x]\n  (interface1/add-two x))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                         (helper/execute-polylith project "create" "c" "comp-1a" "interface-1")
                         (helper/execute-polylith project "create" "c" "comp-1b" "interface-1")
                         (helper/execute-polylith project "create" "c" "comp-2")
                         (file/replace-file! (str ws-dir "/components/comp-2/src/my/company/comp_2/core.clj") core-content)
                         (helper/execute-polylith project "deps"))]
      (is (= ["FYI: the component comp-1b was created but not added to development because it's interface interface-1 was already used by comp-1a."
              "comp-1a:"
              "comp-1b:"
              "comp-2:"
              "  interface-1"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace--print-interface-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content ["(ns my.company.comp-2.core"
                        "  (:require [my.company.interface-1.interface :as interface1]))"
                        "(defn add-two [x]\n  (interface1/add-two x))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                         (helper/execute-polylith project "create" "c" "comp-1a" "interface-1")
                         (helper/execute-polylith project "create" "c" "comp-1b" "interface-1")
                         (helper/execute-polylith project "create" "c" "comp-2")
                         (file/replace-file! (str ws-dir "/components/comp-2/src/my/company/comp_2/core.clj") core-content)
                         (helper/execute-polylith project "deps" "+component"))]
      (is (= ["FYI: the component comp-1b was created but not added to development because it's interface interface-1 was already used by comp-1a."
              "comp-1a:"
              "comp-2:"
              ;; We don't print comp-1b, because it's not part of any environment or system.
              ;; When creating 'comp-1a' it is also added to the 'development' environment,
              ;; but when adding the 'comp-1b' component with interface 'interface-1',
              ;; an already existing component (comp1a) with the same interface already exists
              ;; and is therefore not added.
              "  comp-1a"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content ["(ns comp2.core"
                        "  (:require [interface1.interface :as interface1]))"
                        "(defn add-two [x]\n  (interface1/add-two x))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                         (helper/execute-polylith project "create" "c" "comp1" "interface1")
                         (helper/execute-polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/components/comp2/src/comp2/core.clj") core-content)
                         (helper/execute-polylith project "deps" "+component"))]
      (is (= ["comp1:"
              "comp2:"
              "  comp1"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content ["(ns comp2.core"
                        "  (:require [interface1.interface :as interface1]))"
                        "(defn add-two [x]\n  (interface1/add-two x))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                         (helper/execute-polylith project "create" "c" "comp1" "interface1")
                         (helper/execute-polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/components/comp2/src/comp2/core.clj") core-content)
                         (helper/execute-polylith project "deps" "comp2" "+component"))]
      (is (= ["comp2:"
              "  comp1"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace-from-base--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content ["(ns my.company.system1.core"
                        "  (:require [my.company.interface1.interface :as interface1])"
                        "  (:gen-class))"
                        "(defn -main [& args]"
                        "  (interface1/add-two 1)"
                        "  (println \"Hello world!\"))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                         (helper/execute-polylith project "create" "s" "system1" "system1")
                         (helper/execute-polylith project "create" "c" "comp1a" "interface1")
                         (helper/execute-polylith project "create" "c" "comp1b" "interface1")
                         (helper/execute-polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/systems/system1/src/my/company/system1/core.clj") core-content)
                         (helper/execute-polylith project "deps" "+c"))]
      (is (= ["FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "comp2:"
              "system1:"
              "  comp1a"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace-from-base--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content ["(ns system1.core"
                        "  (:require [interface1.interface :as interface1])"
                        "  (:gen-class))"
                        "(defn -main [& args]"
                        "  (interface1/add-two 1)"
                        "  (println \"Hello world!\"))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                         (helper/execute-polylith project "create" "s" "system1" "system1")
                         (helper/execute-polylith project "create" "c" "comp1a" "interface1")
                         (helper/execute-polylith project "create" "c" "comp1b" "interface1")
                         (helper/execute-polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") core-content)
                         (helper/execute-polylith project "deps" "+c"))]
      (is (= ["FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "comp2:"
              "system1:"
              "  comp1a"]
             (helper/split-lines output))))))

(deftest polylith-deps--several-components-using-same-interface--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          sys1-content ["(ns system1.core"
                        "  (:require [interface1.interface :as interface1])"
                        "  (:gen-class))"
                        "(defn -main [& args]"
                        "  (interface1/add-two 1)"
                        "  (println \"Hello world!\"))"]
          output (with-out-str
                   (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                   (helper/execute-polylith project "create" "s" "system1" "system1")
                   (helper/execute-polylith project "create" "s" "system2" "system2")
                   (helper/execute-polylith project "create" "c" "comp")
                   (helper/execute-polylith project "create" "c" "comp1a" "interface1")
                   (helper/execute-polylith project "create" "c" "comp1b" "interface1")
                   (helper/execute-polylith project "create" "c" "comp2")
                   (helper/execute-polylith project "add" "comp1a" "system1")
                   (helper/execute-polylith project "add" "comp1b" "system2")
                   (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
                   (helper/execute-polylith project "deps" "+c"))]
      (is (= ["FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp:"
              "comp1a:"
              "comp1b:"
              "comp2:"
              "system1:"
              "  comp1a"
              "  comp1b"
              "system2:"]
             (helper/split-lines output))))))

(deftest polylith-deps--several-components-using-same-interface-filter-on-system--print-interface-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          sys1-content ["(ns system1.core"
                        "  (:require [interface1.interface :as interface1])"
                        "  (:gen-class))"
                        "(defn -main [& args]"
                        "  (interface1/add-two 1)"
                        "  (println \"Hello world!\"))"]
          output (with-out-str
                   (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                   (helper/execute-polylith project "create" "s" "system1" "system1")
                   (helper/execute-polylith project "create" "s" "system2" "system2")
                   (helper/execute-polylith project "create" "c" "comp")
                   (helper/execute-polylith project "create" "c" "comp1a" "interface1")
                   (helper/execute-polylith project "create" "c" "comp1b" "interface1")
                   (helper/execute-polylith project "create" "c" "comp2")
                   (helper/execute-polylith project "add" "comp1a" "system1")
                   (helper/execute-polylith project "add" "comp1b" "system2")
                   (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
                   (helper/execute-polylith project "deps" "system1"))]
      (is (= ["FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "system1:"
              "  interface1"]
             (helper/split-lines output))))))

(deftest polylith-deps--several-components-using-same-interface-filter-on-system--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          sys1-content ["(ns system1.core"
                        "  (:require [interface1.interface :as interface1])"
                        "  (:gen-class))"
                        "(defn -main [& args]"
                        "  (interface1/add-two 1)"
                        "  (println \"Hello world!\"))"]
          output (with-out-str
                   (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                   (helper/execute-polylith project "create" "s" "system1" "system1")
                   (helper/execute-polylith project "create" "s" "system2" "system2")
                   (helper/execute-polylith project "create" "c" "comp")
                   (helper/execute-polylith project "create" "c" "comp1a" "interface1")
                   (helper/execute-polylith project "create" "c" "comp1b" "interface1")
                   (helper/execute-polylith project "create" "c" "comp2")
                   (helper/execute-polylith project "add" "comp1a" "system1")
                   (helper/execute-polylith project "add" "comp1b" "system2")
                   (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
                   (helper/execute-polylith project "deps" "system1" "+c"))]
      (is (= ["FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "system1:"
              "  comp1a"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace--print-function-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content1 ["(ns my.company.comp2.core"
                         "  (:require [my.company.comp1.interface :as comp1]))"
                         "(defn add-two [x]"
                         "  (comp1/add-two x))"]
          core-content2 ["(ns my.company.comp3.core2"
                         "  (:require [my.company.comp1.interface :as comp1]))"
                         "(defn add-two [x]"
                         "  (comp1/add-two x))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                         (helper/execute-polylith project "create" "c" "comp1")
                         (helper/execute-polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/components/comp2/src/my/company/comp2/core.clj") core-content1)
                         (file/replace-file! (str ws-dir "/components/comp2/src/my/company/comp2/core2.clj") core-content2)
                         (helper/execute-polylith project "deps" "+f"))]
      (is (= ["comp1:"
              "comp2:"
              "  my.company.comp1.interface/add-two"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace-filter-out-environment--print-function-dependencies-from-the-development-environment
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          comp1-content ["(ns comp1.core"
                         "  (:require [interface1.interface :as interface1]))"
                         "(defn add-two [x]\n  (interface1/add-two x))"]
          comp2-content ["(ns comp2.core"
                         "  (:require [interface1.interface :as interface1]))"
                         "(defn add-two [x]\n  (interface1/add-two x))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
                         (helper/execute-polylith project "create" "c" "comp1" "interface1")
                         (helper/execute-polylith project "create" "c" "comp2" "interface1")
                         (helper/execute-polylith project "create" "s" "system1" "system1")
                         (helper/execute-polylith project "add" "comp2" "system1")
                         (file/replace-file! (str ws-dir "/components/comp1/src/comp1/core.clj") comp1-content)
                         (file/replace-file! (str ws-dir "/components/comp2/src/comp2/core.clj") comp2-content)
                         (helper/execute-polylith project "deps" "development" "+function"))]
      (is (= ["FYI: the component comp2 was created but not added to development because it's interface interface1 was already used by comp1."
              "comp1:"
              "  interface1.interface/add-two"
              "system1:"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace-and-changed-interface--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content ["(ns interface1.interface"
                        "  (:require [component1.core :as core]"
                        "            [database.interface :as database]))"
                        "(defn add-two [x]"
                        "  (database/add-two x)"
                        "  (core/add-two x))"]
          output       (with-out-str
                         (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                         (helper/execute-polylith project "create" "c" "component1" "interface1")
                         (helper/execute-polylith project "create" "c" "component2")
                         (helper/execute-polylith project "create" "c" "database")
                         (file/replace-file! (str ws-dir "/components/component1/src/interface1/interface.clj") core-content)
                         (helper/execute-polylith project "deps" "+c"))]
      (is (= ["component1:"
              "  database"
              "component2:"
              "database:"]
             (helper/split-lines output))))))

(deftest polylith-deps--non-functional-dependencies-with-namespace--print-function-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir     (str @helper/root-dir "/ws1")
          project    (helper/settings ws-dir "com.abc")
          i1-content ["(ns com.abc.component1.interface)"
                      "(def val1 1)"]
          c1-content ["(ns com.abc.component1.core)"]
          i2-content ["(ns com.abc.component2.interface)"
                      "(def val2 2)"]
          c2-content ["(ns com.abc.component2.core"
                      "  (:require [com.abc.component1.interface :as component1]))"
                      "(def ref1 component1/val1)"]
          i3-content ["(ns com.abc.component3.interface"
                      "  (:require [com.abc.component2.interface :as component2]))"
                      "(def ref2 (+ 1 component2/val2))"]
          c3-content ["(ns com.abc.component3.core)"]
          output     (with-out-str
                       (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                       (helper/execute-polylith project "create" "s" "system1" "system1")
                       (helper/execute-polylith project "create" "c" "component1")
                       (helper/execute-polylith project "create" "c" "component2")
                       (helper/execute-polylith project "create" "c" "component3")
                       (file/replace-file! (str ws-dir "/components/component1/src/com/abc/component1/interface.clj") i1-content)
                       (file/replace-file! (str ws-dir "/components/component1/src/com/abc/component1/core.clj") c1-content)
                       (file/replace-file! (str ws-dir "/components/component2/src/com/abc/component2/interface.clj") i2-content)
                       (file/replace-file! (str ws-dir "/components/component2/src/com/abc/component2/core.clj") c2-content)
                       (file/replace-file! (str ws-dir "/components/component3/src/com/abc/component3/interface.clj") i3-content)
                       (file/replace-file! (str ws-dir "/components/component3/src/com/abc/component3/core.clj") c3-content)
                       (helper/execute-polylith project "deps" "+c")
                       (println "-----------")
                       (helper/execute-polylith project "deps" "+f"))]
      (is (= ["component1:"
              "component2:"
              "  component1"
              "component3:"
              "  component2"
              "system1:"
              "-----------"
              "component1:"
              "component2:"
              "  com.abc.component1.interface/val1"
              "component3:"
              "  com.abc.component2.interface/val2"
              "system1:"]
             (helper/split-lines output))))))

(deftest component-dependencies-test
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content ["(ns comp2.core"
                        "  (:require [interface1.interface :as interface1]))"
                        "(defn add-two [x]\n  (interface1/add-two x))"]]
      (helper/execute-polylith nil "create" "w" "ws1" "" "-git")
      (helper/execute-polylith project "create" "c" "comp1" "interface1")
      (helper/execute-polylith project "create" "c" "comp2")
      (file/replace-file! (str ws-dir "/components/comp2/src/comp2/core.clj") core-content)

      (is (= {"comp1" []
              "comp2" ["comp1"]}
             (deps/component-dependencies ws-dir ""))))))
