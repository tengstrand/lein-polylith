(ns leiningen.polylith.cmd.create-system-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.diff]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.version :as v]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-create--create-system-with-environent-name--returns-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (let [output (with-out-str
                     (polylith/polylith nil "create" "w" "ws1" "my.company")
                     (polylith/polylith (helper/settings ws-dir "my.company")
                                        "create" "s" "development"))]
        (is (= "An environment with the name 'development' already exists. Systems and environments are not allowed to have the same name.\n"
               output))))))

(deftest polylith-create--create-same-system-twice--returns-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (let [output (with-out-str
                     (polylith/polylith nil "create" "w" "ws1" "my.company")
                     (polylith/polylith (helper/settings ws-dir "my.company")
                                        "create" "s" "sys1")
                     (polylith/polylith (helper/settings ws-dir "my.company")
                                        "create" "s" "sys1"))]
        (is (= "System 'sys1' already exists.\n"
               output))))))

(deftest polylith-create--create-system--creates-system-with-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith (helper/settings ws-dir "my.company")
                         "create" "s" "sys-1" "base-1")

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "bases/base-1"
               "bases/base-1/Readme.md"
               "bases/base-1/project.clj"
               "bases/base-1/resources"
               "bases/base-1/resources/.keep"
               "bases/base-1/resources/base-1"
               "bases/base-1/resources/base-1/.keep"
               "bases/base-1/src"
               "bases/base-1/src/my"
               "bases/base-1/src/my/company"
               "bases/base-1/src/my/company/base_1"
               "bases/base-1/src/my/company/base_1/core.clj"
               "bases/base-1/test"
               "bases/base-1/test/my"
               "bases/base-1/test/my/company"
               "bases/base-1/test/my/company/base_1"
               "bases/base-1/test/my/company/base_1/core_test.clj"
               "components"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/base-1-Readme.md"
               "environments/development/docs/sys-1-Readme.md"
               "environments/development/interfaces"
               "environments/development/interfaces/my"
               "environments/development/interfaces/my/company"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/bases/base-1-project.clj"
               "environments/development/project-files/components"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/systems/sys-1-project.clj"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/base-1"
               "environments/development/resources/base-1/.keep"
               "environments/development/src"
               "environments/development/src/my"
               "environments/development/src/my/company"
               "environments/development/src/my/company/base_1"
               "environments/development/src/my/company/base_1/core.clj"
               "environments/development/test"
               "environments/development/test/my"
               "environments/development/test/my/company"
               "environments/development/test/my/company/base_1"
               "environments/development/test/my/company/base_1/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "interfaces/src/my"
               "interfaces/src/my/company"
               "logo.png"
               "project.clj"
               "systems"
               "systems/sys-1"
               "systems/sys-1/Readme.md"
               "systems/sys-1/build.sh"
               "systems/sys-1/project.clj"
               "systems/sys-1/resources"
               "systems/sys-1/resources/.keep"
               "systems/sys-1/resources/base-1"
               "systems/sys-1/resources/base-1/.keep"
               "systems/sys-1/src"
               "systems/sys-1/src/my"
               "systems/sys-1/src/my/company"
               "systems/sys-1/src/my/company/base_1"
               "systems/sys-1/src/my/company/base_1/core.clj"}
             (set (file/relative-paths ws-dir))))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "interfaces/project.clj")))

      (is (= (helper/base-project-content "base-1" 'my.company/base-1 'my.company/interfaces)
             (helper/content ws-dir "bases/base-1/project.clj")))

      (is (= [['ns 'my.company.base-1.core
                [:gen-class]]
              ['defn '-main ['& 'args]
                ['println "Hello world!"]]]
             (helper/content ws-dir "bases/base-1/src/my/company/base_1/core.clj")))

      (is (= [['ns 'my.company.base-1.core-test
                [:require ['clojure.test :refer :all]
                          ['my.company.base-1.core :as 'core]]]
              ['deftest 'hello-world-example-test
                 ['let ['output ['with-out-str ['core/-main]]]
                   ['is
                     ['= "Hello world!\n"
                        'output]]]]]
             (helper/content ws-dir "bases/base-1/test/my/company/base_1/core_test.clj")))

      (is (= [['defproject 'my.company/sys-1 "0.1"
               :description "A sys-1 system."
               :dependencies [['org.clojure/clojure "1.9.0"]]
               :aot :all
               :main 'my.company.base-1.core]]
             (helper/content ws-dir "systems/sys-1/project.clj")))

      (is (= [['defproject 'my.company/development "1.0"
                :description "The main development environment"
                :dependencies [['org.clojure/clojure "1.9.0"]]]]
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= [['defproject 'my.company/development "1.0"
                :description "A Polylith workspace."
                :plugins [['polylith/lein-polylith v/version]]
                :polylith {:clojure-version      "1.9.0"
                           :top-namespace        "my.company"}]]
             (helper/content ws-dir "project.clj"))))))

(deftest polylith-create--create-system--without-ns--creates-system
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "")
      (polylith/polylith (helper/settings ws-dir "")
                         "create" "s" "sys-1" "base-1")

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "bases/base-1"
               "bases/base-1/Readme.md"
               "bases/base-1/project.clj"
               "bases/base-1/resources"
               "bases/base-1/resources/.keep"
               "bases/base-1/resources/base-1"
               "bases/base-1/resources/base-1/.keep"
               "bases/base-1/src"
               "bases/base-1/src/base_1"
               "bases/base-1/src/base_1/core.clj"
               "bases/base-1/test"
               "bases/base-1/test/base_1"
               "bases/base-1/test/base_1/core_test.clj"
               "components"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/base-1-Readme.md"
               "environments/development/docs/sys-1-Readme.md"
               "environments/development/interfaces"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/bases/base-1-project.clj"
               "environments/development/project-files/components"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/systems/sys-1-project.clj"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/base-1"
               "environments/development/resources/base-1/.keep"
               "environments/development/src"
               "environments/development/src/base_1"
               "environments/development/src/base_1/core.clj"
               "environments/development/test"
               "environments/development/test/base_1"
               "environments/development/test/base_1/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "logo.png"
               "project.clj"
               "systems"
               "systems/sys-1"
               "systems/sys-1/Readme.md"
               "systems/sys-1/build.sh"
               "systems/sys-1/project.clj"
               "systems/sys-1/resources"
               "systems/sys-1/resources/.keep"
               "systems/sys-1/resources/base-1"
               "systems/sys-1/resources/base-1/.keep"
               "systems/sys-1/src"
               "systems/sys-1/src/base_1"
               "systems/sys-1/src/base_1/core.clj"}
             (set (file/relative-paths ws-dir))))

      (is (= (helper/interfaces-project-content 'interfaces)
             (helper/content ws-dir "interfaces/project.clj")))

      (is (= [['defproject 'base-1 "0.1"
               :description "A base-1 base"
               :dependencies [['interfaces "1.0"]
                              ['org.clojure/clojure "1.9.0"]]
               :aot
               :all]]
             (helper/content ws-dir "bases/base-1/project.clj")))

      (is (= [['ns 'base-1.core
               [:gen-class
                 ['defn '-main ['& 'args]
                  ['println "Hello world!"
                      (helper/content ws-dir "bases/base-1/src/base_1/core.clj")]]]]]))

      (is (= [['ns 'base-1.core-test
               [:require ['clojure.test :refer :all]
                ['base-1.core :as 'core]]]
              ['deftest 'hello-world-example-test
                ['let ['output ['with-out-str ['core/-main]]]
                  ['is ['= "Hello world!\n"
                        'output]]]]]
             (helper/content ws-dir "bases/base-1/test/base_1/core_test.clj")))

      (is (= [['defproject 'sys-1 "0.1"
               :description "A sys-1 system."
               :dependencies [['org.clojure/clojure "1.9.0"]]
               :aot :all
               :main 'base-1.core]]
             (helper/content ws-dir "systems/sys-1/project.clj")))

      (is (= [['defproject 'development "1.0"
               :description "The main development environment"
               :dependencies [['org.clojure/clojure "1.9.0"]]]]
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= [['defproject 'development "1.0"
               :description "A Polylith workspace."
               :plugins [['polylith/lein-polylith v/version]]
               :polylith {:clojure-version      "1.9.0"
                          :top-namespace        ""}]]
             (helper/content ws-dir "project.clj"))))))
