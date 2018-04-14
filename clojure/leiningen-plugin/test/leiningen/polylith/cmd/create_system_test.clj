(ns leiningen.polylith.cmd.create-system-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.diff]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.version :as v]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-create--create-same-system-twice--returns-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
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
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith (helper/settings ws-dir "my.company")
                         "create" "s" "sys1" "base-1")

      (is (= ["interfaces/src/my/company"
              "interfaces/src/my"
              "interfaces/src"
              "interfaces/project.clj"
              "interfaces"
              "systems/sys1/src/my/company/base_1/core.clj"
              "systems/sys1/src/my/company/base_1"
              "systems/sys1/src/my/company"
              "systems/sys1/src/my"
              "systems/sys1/src"
              "systems/sys1/build.sh"
              "systems/sys1/Readme.md"
              "systems/sys1/resources/base-1"
              "systems/sys1/resources"
              "systems/sys1/project.clj"
              "systems/sys1"
              "systems"
              "components"
              "bases/base-1/src/my/company/base_1/core.clj"
              "bases/base-1/src/my/company/base_1"
              "bases/base-1/src/my/company"
              "bases/base-1/src/my"
              "bases/base-1/src"
              "bases/base-1/Readme.md"
              "bases/base-1/resources/base-1"
              "bases/base-1/resources"
              "bases/base-1/test/my/company/base_1/core_test.clj"
              "bases/base-1/test/my/company/base_1"
              "bases/base-1/test/my/company"
              "bases/base-1/test/my"
              "bases/base-1/test"
              "bases/base-1/project.clj"
              "bases/base-1"
              "bases"
              "environments/development/src/my/company/base_1/core.clj"
              "environments/development/src/my/company/base_1"
              "environments/development/src/my/company"
              "environments/development/src/my"
              "environments/development/src"
              "environments/development/interfaces/my/company"
              "environments/development/interfaces/my"
              "environments/development/interfaces"
              "environments/development/docs/base-1-Readme.md"
              "environments/development/docs/sys1-Readme.md"
              "environments/development/docs"
              "environments/development/project-files/workspace-project.clj"
              "environments/development/project-files/interfaces-project.clj"
              "environments/development/project-files/systems/sys1-project.clj"
              "environments/development/project-files/systems"
              "environments/development/project-files/components"
              "environments/development/project-files/bases/base-1-project.clj"
              "environments/development/project-files/bases"
              "environments/development/project-files"
              "environments/development/resources/base-1"
              "environments/development/resources"
              "environments/development/test/my/company/base_1/core_test.clj"
              "environments/development/test/my/company/base_1"
              "environments/development/test/my/company"
              "environments/development/test/my"
              "environments/development/test"
              "environments/development/project.clj"
              "environments/development"
              "environments"
              "project.clj"]
             (file/relative-paths ws-dir)))

      (is (= (helper/interfaces-project-content 'my.company/interfaces)
             (helper/content ws-dir "interfaces/project.clj")))

      (is (= [['defproject 'my.company/base-1 "0.1"
               :description "A base-1 base"
               :dependencies [['my.company/interfaces "1.0"]
                              ['org.clojure/clojure "1.9.0"]]
               :aot
               :all]]
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

      (is (= [['defproject 'my.company/sys1 "0.1"
                :description "A sys1 system."
                :dependencies [['org.clojure/clojure "1.9.0"]]
                :aot :all
                :main 'my.company.sys.core]]))
      (helper/content ws-dir "systems/sys1/project.clj")

      (is (= [['defproject 'my.company/development "1.0"
                :description "The main development environment"
                :dependencies [['org.clojure/clojure "1.9.0"]]]]
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= [['defproject 'my.company/development "1.0"
                :description "The workspace"
                :plugins [['polylith/lein-polylith "0.0.35-alpha"]]
                :polylith {:build-tool           "leiningen"
                           :clojure-version      "1.9.0"
                           :example-hash1        "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1"
                           :example-hash2        "58cd8b3106c942f372a40616fe9155c9d2efd122"
                           :ignored-tests        []
                           :top-ns               "my.company"
                           :vcs                  "git"}]]
             (helper/content ws-dir "project.clj"))))))

(deftest polylith-create--create-system--without-ns--creates-system
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (polylith/polylith nil "create" "w" "ws1" "")
      (polylith/polylith (helper/settings ws-dir "")
                         "create" "s" "sys1" "base-1")

      (is (= ["interfaces/src"
              "interfaces/project.clj"
              "interfaces"
              "systems/sys1/src/base_1/core.clj"
              "systems/sys1/src/base_1"
              "systems/sys1/src"
              "systems/sys1/build.sh"
              "systems/sys1/Readme.md"
              "systems/sys1/resources/base-1"
              "systems/sys1/resources"
              "systems/sys1/project.clj"
              "systems/sys1"
              "systems"
              "components"
              "bases/base-1/src/base_1/core.clj"
              "bases/base-1/src/base_1"
              "bases/base-1/src"
              "bases/base-1/Readme.md"
              "bases/base-1/resources/base-1"
              "bases/base-1/resources"
              "bases/base-1/test/base_1/core_test.clj"
              "bases/base-1/test/base_1"
              "bases/base-1/test"
              "bases/base-1/project.clj"
              "bases/base-1"
              "bases"
              "environments/development/src/base_1/core.clj"
              "environments/development/src/base_1"
              "environments/development/src"
              "environments/development/interfaces"
              "environments/development/docs/base-1-Readme.md"
              "environments/development/docs/sys1-Readme.md"
              "environments/development/docs"
              "environments/development/project-files/workspace-project.clj"
              "environments/development/project-files/interfaces-project.clj"
              "environments/development/project-files/systems/sys1-project.clj"
              "environments/development/project-files/systems"
              "environments/development/project-files/components"
              "environments/development/project-files/bases/base-1-project.clj"
              "environments/development/project-files/bases"
              "environments/development/project-files"
              "environments/development/resources/base-1"
              "environments/development/resources"
              "environments/development/test/base_1/core_test.clj"
              "environments/development/test/base_1"
              "environments/development/test"
              "environments/development/project.clj"
              "environments/development"
              "environments"
              "project.clj"]
             (file/relative-paths ws-dir)))

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

      (is (= [['defproject 'sys1 "0.1"
               :description "A sys1 system."
               :dependencies [['org.clojure/clojure "1.9.0"]]
               :aot :all
               :main 'base-1.core]]
             (helper/content ws-dir "systems/sys1/project.clj")))

      (is (= [['defproject 'development "1.0"
               :description "The main development environment"
               :dependencies [['org.clojure/clojure "1.9.0"]]]]
             (helper/content ws-dir "environments/development/project.clj")))

      (is (= [['defproject 'development "1.0"
               :description "The workspace"
               :plugins [['polylith/lein-polylith "0.0.35-alpha"]]
               :polylith {:build-tool           "leiningen"
                          :clojure-version      "1.9.0"
                          :example-hash1        "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1"
                          :example-hash2        "58cd8b3106c942f372a40616fe9155c9d2efd122"
                          :ignored-tests        []
                          :top-ns               ""
                          :vcs                  "git"}]]
             (helper/content ws-dir "project.clj"))))))
