(ns leiningen.polylith.cmd.deps-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.shared :as shared]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-deps--interface-deps-with-namespace--print-interface-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content [(str "(ns my.company.comp-2.core\n"
                             "  (:require [my.company.interface-1.interface :as interface1]))\n\n"
                             "(defn add-two [x]\n  (interface1/add-two x))")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                         (polylith/polylith project "create" "c" "comp-1a" "interface-1")
                         (polylith/polylith project "create" "c" "comp-1b" "interface-1")
                         (polylith/polylith project "create" "c" "comp-2")
                         (file/replace-file! (str ws-dir "/components/comp-2/src/my/company/comp_2/core.clj") core-content)
                         (polylith/polylith project "deps"))]
      (is (= ["  FYI: the component comp-1b was created but not added to development because it's interface interface-1 was already used by comp-1a."
              "comp-1a:"
              "comp-1b:"
              "comp-2:"
              "  interface-1"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace--print-interface-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content [(str "(ns my.company.comp-2.core\n"
                             "  (:require [my.company.interface-1.interface :as interface1]))\n\n"
                             "(defn add-two [x]\n  (interface1/add-two x))")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                         (polylith/polylith project "create" "c" "comp-1a" "interface-1")
                         (polylith/polylith project "create" "c" "comp-1b" "interface-1")
                         (polylith/polylith project "create" "c" "comp-2")
                         (file/replace-file! (str ws-dir "/components/comp-2/src/my/company/comp_2/core.clj") core-content)
                         (polylith/polylith project "deps" "+component"))]
      (is (= ["  FYI: the component comp-1b was created but not added to development because it's interface interface-1 was already used by comp-1a."
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
          core-content [(str "(ns comp2.core\n"
                             "  (:require [interface1.interface :as interface1]))\n\n"
                             "(defn add-two [x]\n  (interface1/add-two x))")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "" "-git")
                         (polylith/polylith project "create" "c" "comp1" "interface1")
                         (polylith/polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/components/comp2/src/comp2/core.clj") core-content)
                         (polylith/polylith project "deps" "+component"))]
      (is (= ["comp1:"
              "comp2:"
              "  comp1"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace-from-base--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content [(str "(ns my.company.system1.core\n"
                             "  (:require [my.company.interface1.interface :as interface1])\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& args]\n"
                             "  (interface1/add-two 1)\n"
                             "  (println \"Hello world!\"))\n")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                         (polylith/polylith project "create" "s" "system1")
                         (polylith/polylith project "create" "c" "comp1a" "interface1")
                         (polylith/polylith project "create" "c" "comp1b" "interface1")
                         (polylith/polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/systems/system1/src/my/company/system1/core.clj") core-content)
                         (polylith/polylith project "deps" "+c"))]
      (is (= ["  FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "comp2:"
              "system1:"
              "  comp1a"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace-from-base--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content [(str "(ns system1.core\n"
                             "  (:require [interface1.interface :as interface1])\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& args]\n"
                             "  (interface1/add-two 1)\n"
                             "  (println \"Hello world!\"))\n")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "" "-git")
                         (polylith/polylith project "create" "s" "system1")
                         (polylith/polylith project "create" "c" "comp1a" "interface1")
                         (polylith/polylith project "create" "c" "comp1b" "interface1")
                         (polylith/polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") core-content)
                         (polylith/polylith project "deps" "+c"))]
      (is (= ["  FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "comp2:"
              "system1:"
              "  comp1a"]
             (helper/split-lines output))))))

(deftest polylith-deps--several-components-using-same-interface--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          sys1-content [(str "(ns system1.core\n"
                             "  (:require [interface1.interface :as interface1])\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& args]\n"
                             "  (interface1/add-two 1)\n"
                             "  (println \"Hello world!\"))\n")]
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
                   (polylith/polylith project "create" "s" "system1")
                   (polylith/polylith project "create" "s" "system2")
                   (polylith/polylith project "create" "c" "comp")
                   (polylith/polylith project "create" "c" "comp1a" "interface1")
                   (polylith/polylith project "create" "c" "comp1b" "interface1")
                   (polylith/polylith project "create" "c" "comp2")
                   (polylith/polylith project "add" "comp1a" "system1")
                   (polylith/polylith project "add" "comp1b" "system2")
                   (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
                   (polylith/polylith project "deps" "+c"))]
      (is (= ["  FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
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
          sys1-content [(str "(ns system1.core\n"
                             "  (:require [interface1.interface :as interface1])\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& args]\n"
                             "  (interface1/add-two 1)\n"
                             "  (println \"Hello world!\"))\n")]
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
                   (polylith/polylith project "create" "s" "system1")
                   (polylith/polylith project "create" "s" "system2")
                   (polylith/polylith project "create" "c" "comp")
                   (polylith/polylith project "create" "c" "comp1a" "interface1")
                   (polylith/polylith project "create" "c" "comp1b" "interface1")
                   (polylith/polylith project "create" "c" "comp2")
                   (polylith/polylith project "add" "comp1a" "system1")
                   (polylith/polylith project "add" "comp1b" "system2")
                   (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
                   (polylith/polylith project "deps" "system1"))]
      (is (= ["  FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "system1:"
              "  interface1"]
             (helper/split-lines output))))))

(deftest polylith-deps--several-components-using-same-interface-filter-on-system--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          sys1-content [(str "(ns system1.core\n"
                             "  (:require [interface1.interface :as interface1])\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& args]\n"
                             "  (interface1/add-two 1)\n"
                             "  (println \"Hello world!\"))\n")]
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
                   (polylith/polylith project "create" "s" "system1")
                   (polylith/polylith project "create" "s" "system2")
                   (polylith/polylith project "create" "c" "comp")
                   (polylith/polylith project "create" "c" "comp1a" "interface1")
                   (polylith/polylith project "create" "c" "comp1b" "interface1")
                   (polylith/polylith project "create" "c" "comp2")
                   (polylith/polylith project "add" "comp1a" "system1")
                   (polylith/polylith project "add" "comp1b" "system2")
                   (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
                   (polylith/polylith project "deps" "system1" "+c"))]
      (is (= ["  FYI: the component comp1b was created but not added to development because it's interface interface1 was already used by comp1a."
              "comp1a:"
              "system1:"
              "  comp1a"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace--print-function-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content1 [(str "(ns my.company.comp2.core\n"
                              "  (:require [my.company.comp1.interface :as comp1]))\n\n"
                              "(defn add-two [x]\n"
                              "  (comp1/add-two x))")]
          core-content2 [(str "(ns my.company.comp3.core2\n"
                              "  (:require [my.company.comp1.interface :as comp1]))\n\n"
                              "(defn add-two [x]\n"
                              "  (comp1/add-two x))")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                         (polylith/polylith project "create" "c" "comp1")
                         (polylith/polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/components/comp2/src/my/company/comp2/core.clj") core-content1)
                         (file/replace-file! (str ws-dir "/components/comp2/src/my/company/comp2/core2.clj") core-content2)
                         (polylith/polylith project "deps" "+f"))]
      (is (= ["comp1:"
              "comp2:"
              "  my.company.comp1.interface/add-two"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace--print-function-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content [(str "(ns comp2.core\n"
                             "  (:require [interface1.interface :as interface1]))\n\n"
                             "(defn add-two [x]\n  (interface1/add-two x))")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "" "-git")
                         (polylith/polylith project "create" "c" "comp1" "interface1")
                         (polylith/polylith project "create" "c" "comp2")
                         (file/replace-file! (str ws-dir "/components/comp2/src/comp2/core.clj") core-content)
                         (polylith/polylith project "deps" "+function"))]
      (is (= ["comp1:"
              "comp2:"
              "  interface1.interface/add-two"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-with-namespace-and-changed-interface--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "my.company")
          core-content [(str "(ns my.company.interface1.interface\n"
                             "  (:require [my.company.component1.core :as core]\n"
                             "            [my.company.database.interface :as database]))\n\n"
                             "(defn add-two [x]\n"
                             "  (database/add-two x)\n"
                             "  (core/add-two x))")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                         (polylith/polylith project "create" "c" "component1" "interface1")
                         (polylith/polylith project "create" "c" "component2")
                         (polylith/polylith project "create" "c" "database")
                         (file/replace-file! (str ws-dir "/components/component1/src/my/company/interface1/interface.clj") core-content)
                         (polylith/polylith project "deps" "+c"))]
      (is (= ["component1:"
              "  database"
              "component2:"
              "database:"]
             (helper/split-lines output))))))

(deftest polylith-deps--interface-deps-without-namespace-and-changed-interface--print-component-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir       (str @helper/root-dir "/ws1")
          project      (helper/settings ws-dir "")
          core-content [(str "(ns interface1.interface\n"
                             "  (:require [component1.core :as core]\n"
                             "            [database.interface :as database]))\n\n"
                             "(defn add-two [x]\n"
                             "  (database/add-two x)\n"
                             "  (core/add-two x))")]
          output       (with-out-str
                         (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                         (polylith/polylith project "create" "c" "component1" "interface1")
                         (polylith/polylith project "create" "c" "component2")
                         (polylith/polylith project "create" "c" "database")
                         (file/replace-file! (str ws-dir "/components/component1/src/interface1/interface.clj") core-content)
                         (polylith/polylith project "deps" "+c"))]
      (is (= ["component1:"
              "  database"
              "component2:"
              "database:"]
             (helper/split-lines output))))))

(deftest polylith-deps--non-functional-dependencies-with-namespace--print-function-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir     (str @helper/root-dir "/ws1")
          project    (helper/settings ws-dir "com.abc")
          i1-content [(str "(ns com.abc.component1.interface)\n\n"
                           "(def val1 1)")]
          c1-content [(str "(ns com.abc.component1.core)")]
          i2-content [(str "(ns com.abc.component2.interface)\n\n"
                           "(def val2 2)")]
          c2-content [(str "(ns com.abc.component2.core\n"
                           "  (:require [com.abc.component1.interface :as component1]))\n\n"
                           "(def ref1 component1/val1)")]
          i3-content [(str "(ns com.abc.component3.interface\n"
                           "  (:require [com.abc.component2.interface :as component2]))\n\n"
                           "(def ref2 (+ 1 component2/val2))")]
          c3-content [(str "(ns com.abc.component3.core)")]
          output     (with-out-str
                       (polylith/polylith nil "create" "w" "ws1" "com.abc")
                       (polylith/polylith project "create" "s" "system1")
                       (polylith/polylith project "create" "c" "component1")
                       (polylith/polylith project "create" "c" "component2")
                       (polylith/polylith project "create" "c" "component3")
                       (file/replace-file! (str ws-dir "/components/component1/src/com/abc/component1/interface.clj") i1-content)
                       (file/replace-file! (str ws-dir "/components/component1/src/com/abc/component1/core.clj") c1-content)
                       (file/replace-file! (str ws-dir "/components/component2/src/com/abc/component2/interface.clj") i2-content)
                       (file/replace-file! (str ws-dir "/components/component2/src/com/abc/component2/core.clj") c2-content)
                       (file/replace-file! (str ws-dir "/components/component3/src/com/abc/component3/interface.clj") i3-content)
                       (file/replace-file! (str ws-dir "/components/component3/src/com/abc/component3/core.clj") c3-content)
                       (polylith/polylith project "deps" "+c")
                       (println "-----------")
                       (polylith/polylith project "deps" "+f"))]
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
          core-content [(str "(ns comp2.core\n"
                             "  (:require [interface1.interface :as interface1]))\n\n"
                             "(defn add-two [x]\n  (interface1/add-two x))")]]
      (polylith/polylith nil "create" "w" "ws1" "" "-git")
      (polylith/polylith project "create" "c" "comp1" "interface1")
      (polylith/polylith project "create" "c" "comp2")
      (file/replace-file! (str ws-dir "/components/comp2/src/comp2/core.clj") core-content)

      (is (= {"comp1" []
              "comp2" ["comp1"]}
             (deps/component-dependencies ws-dir ""))))))
