(ns leiningen.polylith.cmd.create-system-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.diff]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

;(defn interfaces-project-content [name]
;  [['defproject name "1.0"
;    :description "Component interfaces"
;    :dependencies [['org.clojure/clojure "1.9.0"]
;                   ['org.clojure/spec.alpha "0.1.143"]]
;    :aot
;    :all]])
;
;(defn interfaces-interface-content [ns-name]
;  [['ns ns-name]
;   ['defn 'myfn ['x]]])
;
;(defn src-core-content [ns-name]
;  [['ns ns-name]
;   ['defn 'myfn ['x]
;    ['+ '2 'x]]])
;
;(defn component-core-test-content [ns-name require-ns]
;  [['ns ns-name
;    [:require ['clojure.test :refer :all]
;     [require-ns :as 'interface]]]
;   ['deftest 'test-myfn
;    ['is ['= 42 ['interface/myfn 40]]]]])
;
;(defn src-interface-content [ns-name require-ns]
;  [['ns ns-name
;    [:require [require-ns :as 'core]]]
;   ['defn 'myfn ['x]
;    ['core/myfn 'x]]])
;
;(defn development-project-content [ns-name]
;  [['defproject ns-name "1.0"
;    :description "The main development environment"
;    :dependencies [['org.clojure/clojure "1.9.0"]
;                   ['org.clojure/spec.alpha "0.1.143"]]]])
;
;(defn workspace-project-content [ns-name top-ns top-dir]
;  [['defproject ns-name "1.0"
;    :description "The workspace"
;    :plugins [['polylith/lein-polylith "0.0.35-alpha"]]
;    :polylith {:build-tool           "leiningen"
;               :clojure-spec-version "org.clojure/spec.alpha 0.1.143"
;               :clojure-version      "1.9.0"
;               :example-hash1        "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1"
;               :example-hash2        "58cd8b3106c942f372a40616fe9155c9d2efd122"
;               :ignored-tests        []
;               :top-ns               top-ns
;               :top-dir              top-dir
;               :vcs                  "git"}]])
;
;(defn component-project-content [name ns-name interfaces]
;  [['defproject ns-name "0.1"
;    :description (str "A " name " component")
;    :dependencies [[interfaces "1.0"]
;                   ['org.clojure/clojure "1.9.0"]
;                   ['org.clojure/spec.alpha "0.1.143"]]
;    :aot
;    :all]])

(deftest polylith-create--create-same-system-twice--returns-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")]
      (let [output (with-out-str
                     (polylith/polylith nil "create" "w" "ws1" "my.company")
                     (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                                        "create" "s" "sys1")
                     (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                                        "create" "s" "sys1"))]
        (is (= "System 'sys1' already exists.\n"
               output))))))

(deftest polylith-create--create-system--creates-system-with-namespace
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")]

      (polylith/polylith nil "create" "w" "ws1" "my.company")
      (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                         "create" "s" "sys1")

      (is (= ["interfaces/src/my/company"
              "interfaces/src/my"
              "interfaces/src"
              "interfaces/project.clj"
              "interfaces"
              "systems/sys1/src/my/company/sys1/core.clj"
              "systems/sys1/src/my/company/sys1"
              "systems/sys1/src/my/company"
              "systems/sys1/src/my"
              "systems/sys1/src"
              "systems/sys1/build.sh"
              "systems/sys1/Readme.md"
              "systems/sys1/resources/sys1"
              "systems/sys1/resources"
              "systems/sys1/project.clj"
              "systems/sys1"
              "systems"
              "components"
              "bases/sys1/src/my/company/sys1/core.clj"
              "bases/sys1/src/my/company/sys1"
              "bases/sys1/src/my/company"
              "bases/sys1/src/my"
              "bases/sys1/src"
              "bases/sys1/Readme.md"
              "bases/sys1/test/my/company/sys1/core_test.clj"
              "bases/sys1/test/my/company/sys1"
              "bases/sys1/test/my/company"
              "bases/sys1/test/my"
              "bases/sys1/test"
              "bases/sys1/project.clj"
              "bases/sys1"
              "bases"
              "environments/development/src/my/company"
              "environments/development/src/my"
              "environments/development/src"
              "environments/development/interfaces/my/company"
              "environments/development/interfaces/my"
              "environments/development/interfaces"
              "environments/development/docs/sys1-Readme.md"
              "environments/development/docs"
              "environments/development/project-files/workspace-project.clj"
              "environments/development/project-files/interfaces-project.clj"
              "environments/development/project-files/systems"
              "environments/development/project-files/components"
              "environments/development/project-files/bases"
              "environments/development/project-files"
              "environments/development/resources"
              "environments/development/test/my/company"
              "environments/development/test/my"
              "environments/development/test"
              "environments/development/project.clj"
              "environments/development"
              "environments"
              "project.clj"]
             (file/files ws-dir)))

      (is (= [['defproject 'my.company/sys1 "0.1"
                :description "A sys1 base"
                :dependencies [['my.company/interfaces "1.0"]
                               ['org.clojure/clojure "1.9.0"]
                               ['org.clojure/spec.alpha "0.1.143"]]
                :aot
                :all]]
             (helper/content ws-dir "bases/sys1/project.clj")))

      ;(is (= [['ns 'my.company.sys1.core]]
      ;       (helper/content ws-dir "bases/sys1/src/my/company/sys1/core.clj")))

      (is (= [['ns 'my.company.sys1.core-test]]
             (helper/content ws-dir "bases/sys1/test/my/company/sys1/core_test.clj")))

      (is (= [['defproject 'my.company/sys1 "0.1"
                :description "A sys1 system."
                :dependencies [['org.clojure/clojure "1.9.0"]
                               ['org.clojure/spec.alpha "0.1.143"]]]]
             (helper/content ws-dir "systems/sys1/project.clj"))))))


    ;; todo: add link to 'base' in environments.
    ;; todo: add one more test with a blank top-dir.
