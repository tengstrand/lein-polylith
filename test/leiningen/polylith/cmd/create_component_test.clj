(ns leiningen.polylith.cmd.create-component-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.diff]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.version :as v]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn interfaces-interface-content [ns-name]
  [['ns ns-name]
   ['defn 'add-two ['x]]])

(defn src-core-content [ns-name]
  [['ns ns-name]
   ['defn 'add-two ['x]
    ['+ '2 'x]]])

(defn component-core-test-content [ns-name require-ns]
  [['ns ns-name
    [:require ['clojure.test :refer :all]
     [require-ns :as 'interface]]]
   ['deftest 'test-add-two
    ['is ['= 42 ['interface/add-two 40]]]]])

(defn src-interface-content [ns-name require-ns]
  [['ns ns-name
    [:require [require-ns :as 'core]]]
   ['defn 'add-two ['x]
    ['core/add-two 'x]]])

(defn development-project-content [ns-name]
  [['defproject ns-name "1.0"
    :description "The main development environment"
    :source-paths ["sources/src"]
    :test-paths ["tests/test"]
    :dependencies [['org.clojure/clojure "1.9.0"]]]])

(defn workspace-project-content [ns-name top-ns]
  [['defproject ns-name "1.0"
    :description "A Polylith workspace."
    :plugins [['polylith/lein-polylith v/version]]
    :polylith {:clojure-version      "1.9.0"
               :top-namespace        top-ns}]])

(defn component-project-content [name ns-name interfaces]
  [['defproject ns-name "0.1"
    :description (str "A " name " component")
    :dependencies [[interfaces "1.0"]
                   ['org.clojure/clojure "1.9.0"]]
    :aot
    :all]])

(deftest polylith-create--create-component-twice--returns-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (let [output (with-out-str
                     (polylith/polylith nil "create" "w" "ws1" "my.company")
                     (polylith/polylith (helper/settings ws-dir "my.company")
                                        "create" "c" "comp1")
                     (polylith/polylith (helper/settings ws-dir "my.company")
                                        "create" "c" "comp1"))]
        (is (= "Component 'comp1' already exists.\n"
               output))))))

(deftest polylith-create--create-component--creates-component-with-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith (helper/settings ws-dir "my.company")
                         "create" "c" "comp-1")

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "components"
               "components/comp-1"
               "components/comp-1/Readme.md"
               "components/comp-1/project.clj"
               "components/comp-1/resources"
               "components/comp-1/resources/.keep"
               "components/comp-1/resources/comp-1"
               "components/comp-1/resources/comp-1/.keep"
               "components/comp-1/src"
               "components/comp-1/src/my"
               "components/comp-1/src/my/company"
               "components/comp-1/src/my/company/comp_1"
               "components/comp-1/src/my/company/comp_1/core.clj"
               "components/comp-1/src/my/company/comp_1/interface.clj"
               "components/comp-1/test"
               "components/comp-1/test/my"
               "components/comp-1/test/my/company"
               "components/comp-1/test/my/company/comp_1"
               "components/comp-1/test/my/company/comp_1/core_test.clj"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/comp-1-Readme.md"
               "environments/development/interfaces"
               "environments/development/interfaces/my"
               "environments/development/interfaces/my/company"
               "environments/development/interfaces/my/company/comp_1"
               "environments/development/interfaces/my/company/comp_1/interface.clj"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/components"
               "environments/development/project-files/components/comp-1-project.clj"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/comp-1"
               "environments/development/resources/comp-1/.keep"
               "environments/development/sources"
               "environments/development/sources/src"
               "environments/development/sources/src/my"
               "environments/development/sources/src/my/company"
               "environments/development/sources/src/my/company/comp_1"
               "environments/development/sources/src/my/company/comp_1/core.clj"
               "environments/development/sources/src/my/company/comp_1/interface.clj"
               "environments/development/tests"
               "environments/development/tests/test"
               "environments/development/tests/test/my"
               "environments/development/tests/test/my/company"
               "environments/development/tests/test/my/company/comp_1"
               "environments/development/tests/test/my/company/comp_1/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "interfaces/src/my"
               "interfaces/src/my/company"
               "interfaces/src/my/company/comp_1"
               "interfaces/src/my/company/comp_1/interface.clj"
               "logo.png"
               "project.clj"
               "systems"}
             (set (file/relative-paths ws-dir))))

      (is (= (interfaces-interface-content 'my.company.comp-1.interface)
             (helper/content ws-dir "interfaces/src/my/company/comp_1/interface.clj")))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "interfaces/project.clj")))

      (is (= (src-interface-content 'my.company.comp-1.interface 'my.company.comp-1.core)
             (helper/content ws-dir "components/comp-1/src/my/company/comp_1/interface.clj")))

      (is (= (src-core-content 'my.company.comp-1.core)
             (helper/content ws-dir "components/comp-1/src/my/company/comp_1/core.clj")))

      (is (= (component-core-test-content 'my.company.comp-1.core-test 'my.company.comp-1.interface)
             (helper/content ws-dir "components/comp-1/test/my/company/comp_1/core_test.clj")))

      (is (= (src-interface-content 'my.company.comp-1.interface 'my.company.comp-1.core)
             (helper/content ws-dir "environments/development/sources/src/my/company/comp_1/interface.clj")))

      (is (= (src-core-content 'my.company.comp-1.core)
             (helper/content ws-dir "environments/development/sources/src/my/company/comp_1/core.clj")))

      (is (= (interfaces-interface-content 'my.company.comp-1.interface)
             (helper/content ws-dir "environments/development/interfaces/my/company/comp_1/interface.clj")))

      (is (= (workspace-project-content 'my.company/development "my.company")
             (helper/content ws-dir "environments/development/project-files/workspace-project.clj")))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "environments/development/project-files/interfaces-project.clj")))

      (is (= (component-project-content "comp-1" 'my.company/comp-1 'my.company/interfaces)
             (helper/content ws-dir "environments/development/project-files/components/comp-1-project.clj")))

      (is (= (component-core-test-content 'my.company.comp-1.core-test 'my.company.comp-1.interface)
             (helper/content ws-dir "environments/development/tests/test/my/company/comp_1/core_test.clj")))

      (is (= (development-project-content 'my.company/development)
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= (workspace-project-content 'my.company/development "my.company")
             (helper/content ws-dir "project.clj"))))))

(deftest polylith-create--create-component--creates-component-without-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "")
      (polylith/polylith (helper/settings ws-dir "") "create" "c" "comp-1")

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "components"
               "components/comp-1"
               "components/comp-1/Readme.md"
               "components/comp-1/project.clj"
               "components/comp-1/resources"
               "components/comp-1/resources/.keep"
               "components/comp-1/resources/comp-1"
               "components/comp-1/resources/comp-1/.keep"
               "components/comp-1/src"
               "components/comp-1/src/comp_1"
               "components/comp-1/src/comp_1/core.clj"
               "components/comp-1/src/comp_1/interface.clj"
               "components/comp-1/test"
               "components/comp-1/test/comp_1"
               "components/comp-1/test/comp_1/core_test.clj"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/comp-1-Readme.md"
               "environments/development/interfaces"
               "environments/development/interfaces/comp_1"
               "environments/development/interfaces/comp_1/interface.clj"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/components"
               "environments/development/project-files/components/comp-1-project.clj"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/comp-1"
               "environments/development/resources/comp-1/.keep"
               "environments/development/sources"
               "environments/development/sources/src"
               "environments/development/sources/src/comp_1"
               "environments/development/sources/src/comp_1/core.clj"
               "environments/development/sources/src/comp_1/interface.clj"
               "environments/development/tests"
               "environments/development/tests/test"
               "environments/development/tests/test/comp_1"
               "environments/development/tests/test/comp_1/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "interfaces/src/comp_1"
               "interfaces/src/comp_1/interface.clj"
               "logo.png"
               "project.clj"
               "systems"}
             (set (file/relative-paths ws-dir))))

      (is (= (interfaces-interface-content 'comp-1.interface)
             (helper/content ws-dir "interfaces/src/comp_1/interface.clj")))

      (is (= (helper/interfaces-project-content 'interfaces)
             (helper/content ws-dir "interfaces/project.clj")))

      (is (= (src-interface-content 'comp-1.interface 'comp-1.core)
             (helper/content ws-dir "components/comp-1/src/comp_1/interface.clj")))

      (is (= (src-core-content 'comp-1.core)
             (helper/content ws-dir "components/comp-1/src/comp_1/core.clj")))

      (is (= (component-core-test-content 'comp-1.core-test 'comp-1.interface)
             (helper/content ws-dir "components/comp-1/test/comp_1/core_test.clj")))

      (is (= (src-interface-content 'comp-1.interface 'comp-1.core)
             (helper/content ws-dir "environments/development/sources/src/comp_1/interface.clj")))

      (is (= (src-core-content 'comp-1.core)
             (helper/content ws-dir "environments/development/sources/src/comp_1/core.clj")))

      (is (= (interfaces-interface-content 'comp-1.interface)
             (helper/content ws-dir "environments/development/interfaces/comp_1/interface.clj")))

      (is (= (workspace-project-content 'development "")
             (helper/content ws-dir "environments/development/project-files/workspace-project.clj")))

      (is (= (helper/interfaces-project-content 'interfaces)
             (helper/content ws-dir "environments/development/project-files/interfaces-project.clj")))

      (is (= (component-project-content "comp-1" 'comp-1 'interfaces)
             (helper/content ws-dir "environments/development/project-files/components/comp-1-project.clj")))

      (is (= (component-core-test-content 'comp-1.core-test 'comp-1.interface)
             (helper/content ws-dir "environments/development/tests/test/comp_1/core_test.clj")))

      (is (= (development-project-content 'development)
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= (workspace-project-content 'development "")
             (helper/content ws-dir "project.clj"))))))

(deftest polylith-create--create-component--creates-component-with-namespace-with-different-interface
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith (helper/settings ws-dir "my.company")
                         "create" "c" "log-4j" "logg-ing")

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "components"
               "components/log-4j"
               "components/log-4j/Readme.md"
               "components/log-4j/project.clj"
               "components/log-4j/resources"
               "components/log-4j/resources/.keep"
               "components/log-4j/resources/log-4j"
               "components/log-4j/resources/log-4j/.keep"
               "components/log-4j/src"
               "components/log-4j/src/my"
               "components/log-4j/src/my/company"
               "components/log-4j/src/my/company/log_4j"
               "components/log-4j/src/my/company/log_4j/core.clj"
               "components/log-4j/src/my/company/logg_ing"
               "components/log-4j/src/my/company/logg_ing/interface.clj"
               "components/log-4j/test"
               "components/log-4j/test/my"
               "components/log-4j/test/my/company"
               "components/log-4j/test/my/company/log_4j"
               "components/log-4j/test/my/company/log_4j/core_test.clj"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/log-4j-Readme.md"
               "environments/development/interfaces"
               "environments/development/interfaces/my"
               "environments/development/interfaces/my/company"
               "environments/development/interfaces/my/company/logg_ing"
               "environments/development/interfaces/my/company/logg_ing/interface.clj"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/components"
               "environments/development/project-files/components/log-4j-project.clj"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/log-4j"
               "environments/development/resources/log-4j/.keep"
               "environments/development/sources"
               "environments/development/sources/src"
               "environments/development/sources/src/my"
               "environments/development/sources/src/my/company"
               "environments/development/sources/src/my/company/log_4j"
               "environments/development/sources/src/my/company/log_4j/core.clj"
               "environments/development/sources/src/my/company/logg_ing"
               "environments/development/sources/src/my/company/logg_ing/interface.clj"
               "environments/development/tests"
               "environments/development/tests/test"
               "environments/development/tests/test/my"
               "environments/development/tests/test/my/company"
               "environments/development/tests/test/my/company/log_4j"
               "environments/development/tests/test/my/company/log_4j/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "interfaces/src/my"
               "interfaces/src/my/company"
               "interfaces/src/my/company/logg_ing"
               "interfaces/src/my/company/logg_ing/interface.clj"
               "logo.png"
               "project.clj"
               "systems"}
             (set (file/relative-paths ws-dir))))

      (is (= (interfaces-interface-content 'my.company.logg-ing.interface)
             (helper/content ws-dir "interfaces/src/my/company/logg_ing/interface.clj")))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "interfaces/project.clj")))

      (is (= (src-interface-content 'my.company.logg-ing.interface 'my.company.log-4j.core)
             (helper/content ws-dir "components/log-4j/src/my/company/logg_ing/interface.clj")))

      (is (= (src-core-content 'my.company.log-4j.core)
             (helper/content ws-dir "components/log-4j/src/my/company/log_4j/core.clj")))

      (is (= (component-core-test-content 'my.company.log-4j.core-test 'my.company.logg-ing.interface)
             (helper/content ws-dir "components/log-4j/test/my/company/log_4j/core_test.clj")))

      (is (= (src-interface-content 'my.company.logg-ing.interface 'my.company.log-4j.core)
            (helper/content ws-dir "environments/development/sources/src/my/company/logg_ing/interface.clj")))

      (is (= (src-core-content 'my.company.log-4j.core)
             (helper/content ws-dir "environments/development/sources/src/my/company/log_4j/core.clj")))

      (is (= (interfaces-interface-content 'my.company.logg-ing.interface)
             (helper/content ws-dir "environments/development/interfaces/my/company/logg_ing/interface.clj")))

      (is (= (workspace-project-content 'my.company/development "my.company")
             (helper/content ws-dir "environments/development/project-files/workspace-project.clj")))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "environments/development/project-files/interfaces-project.clj")))

      (is (= (component-project-content "log-4j" 'my.company/log-4j 'my.company/interfaces)
             (helper/content ws-dir "environments/development/project-files/components/log-4j-project.clj")))

      (is (= (component-core-test-content 'my.company.log-4j.core-test 'my.company.logg-ing.interface)
             (helper/content ws-dir "environments/development/tests/test/my/company/log_4j/core_test.clj")))

      (is (= (development-project-content 'my.company/development)
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= (workspace-project-content 'my.company/development "my.company")
             (helper/content ws-dir "project.clj"))))))

(deftest polylith-create--create-two-components-with-the-same-interface--interface-and-components-created-but-only-first-component-added-to-environment
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith (helper/settings ws-dir "my.company")
                         "create" "c" "log4j" "logging")
      (polylith/polylith (helper/settings ws-dir "my.company")
                         "create" "c" "commonslogging" "logging")

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "components"
               "components/commonslogging"
               "components/commonslogging/Readme.md"
               "components/commonslogging/project.clj"
               "components/commonslogging/resources"
               "components/commonslogging/resources/.keep"
               "components/commonslogging/resources/commonslogging"
               "components/commonslogging/resources/commonslogging/.keep"
               "components/commonslogging/src"
               "components/commonslogging/src/my"
               "components/commonslogging/src/my/company"
               "components/commonslogging/src/my/company/commonslogging"
               "components/commonslogging/src/my/company/commonslogging/core.clj"
               "components/commonslogging/src/my/company/logging"
               "components/commonslogging/src/my/company/logging/interface.clj"
               "components/commonslogging/test"
               "components/commonslogging/test/my"
               "components/commonslogging/test/my/company"
               "components/commonslogging/test/my/company/commonslogging"
               "components/commonslogging/test/my/company/commonslogging/core_test.clj"
               "components/log4j"
               "components/log4j/Readme.md"
               "components/log4j/project.clj"
               "components/log4j/resources"
               "components/log4j/resources/.keep"
               "components/log4j/resources/log4j"
               "components/log4j/resources/log4j/.keep"
               "components/log4j/src"
               "components/log4j/src/my"
               "components/log4j/src/my/company"
               "components/log4j/src/my/company/log4j"
               "components/log4j/src/my/company/log4j/core.clj"
               "components/log4j/src/my/company/logging"
               "components/log4j/src/my/company/logging/interface.clj"
               "components/log4j/test"
               "components/log4j/test/my"
               "components/log4j/test/my/company"
               "components/log4j/test/my/company/log4j"
               "components/log4j/test/my/company/log4j/core_test.clj"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/log4j-Readme.md"
               "environments/development/interfaces"
               "environments/development/interfaces/my"
               "environments/development/interfaces/my/company"
               "environments/development/interfaces/my/company/logging"
               "environments/development/interfaces/my/company/logging/interface.clj"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/components"
               "environments/development/project-files/components/log4j-project.clj"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/log4j"
               "environments/development/resources/log4j/.keep"
               "environments/development/sources"
               "environments/development/sources/src"
               "environments/development/sources/src/my"
               "environments/development/sources/src/my/company"
               "environments/development/sources/src/my/company/log4j"
               "environments/development/sources/src/my/company/log4j/core.clj"
               "environments/development/sources/src/my/company/logging"
               "environments/development/sources/src/my/company/logging/interface.clj"
               "environments/development/tests"
               "environments/development/tests/test"
               "environments/development/tests/test/my"
               "environments/development/tests/test/my/company"
               "environments/development/tests/test/my/company/log4j"
               "environments/development/tests/test/my/company/log4j/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "interfaces/src/my"
               "interfaces/src/my/company"
               "interfaces/src/my/company/logging"
               "interfaces/src/my/company/logging/interface.clj"
               "logo.png"
               "project.clj"
               "systems"}
             (set (file/relative-paths ws-dir))))

      (is (= (interfaces-interface-content 'my.company.logging.interface)
             (helper/content ws-dir "interfaces/src/my/company/logging/interface.clj")))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "interfaces/project.clj")))

      (is (= (src-interface-content 'my.company.logging.interface 'my.company.log4j.core)
             (helper/content ws-dir "components/log4j/src/my/company/logging/interface.clj")))

      (is (= (src-core-content 'my.company.log4j.core)
             (helper/content ws-dir "components/log4j/src/my/company/log4j/core.clj")))

      (is (= (component-core-test-content 'my.company.log4j.core-test 'my.company.logging.interface)
             (helper/content ws-dir "components/log4j/test/my/company/log4j/core_test.clj")))

      (is (= (src-interface-content 'my.company.logging.interface 'my.company.log4j.core)
             (helper/content ws-dir "environments/development/sources/src/my/company/logging/interface.clj")))

      (is (= (src-core-content 'my.company.log4j.core)
             (helper/content ws-dir "environments/development/sources/src/my/company/log4j/core.clj")))

      (is (= (interfaces-interface-content 'my.company.logging.interface)
             (helper/content ws-dir "environments/development/interfaces/my/company/logging/interface.clj")))

      (is (= (workspace-project-content 'my.company/development "my.company")
             (helper/content ws-dir "environments/development/project-files/workspace-project.clj")))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "environments/development/project-files/interfaces-project.clj")))

      (is (= (component-project-content "log4j" 'my.company/log4j 'my.company/interfaces)
             (helper/content ws-dir "environments/development/project-files/components/log4j-project.clj")))

      (is (= (component-core-test-content 'my.company.log4j.core-test 'my.company.logging.interface)
             (helper/content ws-dir "environments/development/tests/test/my/company/log4j/core_test.clj")))

      (is (= (development-project-content 'my.company/development)
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= (workspace-project-content 'my.company/development "my.company")
             (helper/content ws-dir "project.clj"))))))
