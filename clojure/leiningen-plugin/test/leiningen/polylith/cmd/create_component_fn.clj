(ns leiningen.polylith.cmd.create-component-fn
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.test-helper :refer [content]]))

(defn settings [ws-dir]
  {:root ws-dir
   :polylith {:vcs "git"
              :build-tool "leiningen"
              :top-dir "my/company"
              :top-ns "my.company"
              :clojure-version "1.9.0"
              :clojure-spec-version "org.clojure/spec.alpha 0.1.143"
              :ignored-tests []
              :example-hash1 "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1"
              :example-hash2 "58cd8b3106c942f372a40616fe9155c9d2efd122"}
   :top-ns "my.company"
   :top-dir "my/company"
   :clojure-version "1.9.0"
   :clojure-spec-version "org.clojure/spec.alpha 0.1.143"
   :sha1 "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1"
   :sha2 "58cd8b3106c942f372a40616fe9155c9d2efd122"})

(def interfaces-project-content
  [['defproject 'my.company/interfaces "1.0"
      :description "Component interfaces"
      :dependencies [['org.clojure/clojure "1.9.0"]
                     ['org.clojure/spec.alpha "0.1.143"]]
      :aot
      :all]])

(def interfaces-interface-content
  [['ns 'my.company.comp1.interface]
   ['defn 'myfn ['x]]])

(def src-core-content
  [['ns 'my.company.comp1.core]
   ['defn 'myfn ['x]
    ['+ '2 'x]]])

(def comp1-core-test
  [['ns 'my.company.comp1.core-test
    [:require ['clojure.test :refer :all]
     ['my.company.comp1.core :as 'core]]]
   ['deftest 'test-myfn
    ['is ['= 42 ['core/myfn 40]]]]])

(def src-interface-content
  [['ns 'my.company.comp1.interface
    [:require ['my.company.comp1.core :as 'core]]]
   ['defn 'myfn ['x]
    ['core/myfn 'x]]])

(def workspace-project-content
  [['defproject 'my.company/development "1.0"
     :description "The workspace"
     :plugins [['polylith/lein-polylith "0.0.35-alpha"]]
     :polylith {:build-tool           "leiningen"
                :clojure-spec-version "org.clojure/spec.alpha 0.1.143"
                :clojure-version      "1.9.0"
                :example-hash1        "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1"
                :example-hash2        "58cd8b3106c942f372a40616fe9155c9d2efd122"
                :ignored-tests        []
                :top-dir              "my/company"
                :top-ns               "my.company"
                :vcs                  "git"}]])

(def comp1-project-content
  [['defproject 'my.company/comp1 "0.1"
     :description "A comp1 component"
     :dependencies [['my.company/interfaces "1.0"]
                    ['org.clojure/clojure "1.9.0"]
                    ['org.clojure/spec.alpha "0.1.143"]]
     :aot
     :all]])

(defn create-component-with-ns [ws-dir]
  (polylith/polylith nil "create" "w" "ws1" "my.company")
  (polylith/polylith (settings ws-dir) "create" "c" "comp1")

  (is (= ["interfaces/src/my/company/comp1/interface.clj"
          "interfaces/src/my/company/comp1"
          "interfaces/src/my/company"
          "interfaces/src/my"
          "interfaces/src"
          "interfaces/project.clj"
          "interfaces"
          "systems"
          "components/comp1/src/my/company/comp1/interface.clj"
          "components/comp1/src/my/company/comp1/core.clj"
          "components/comp1/src/my/company/comp1"
          "components/comp1/src/my/company"
          "components/comp1/src/my"
          "components/comp1/src"
          "components/comp1/Readme.md"
          "components/comp1/resources/comp1"
          "components/comp1/resources"
          "components/comp1/test/my/company/comp1/core_test.clj"
          "components/comp1/test/my/company/comp1"
          "components/comp1/test/my/company"
          "components/comp1/test/my"
          "components/comp1/test"
          "components/comp1/project.clj"
          "components/comp1"
          "components"
          "bases"
          "environments/development/src/my/company/comp1/interface.clj"
          "environments/development/src/my/company/comp1/core.clj"
          "environments/development/src/my/company/comp1"
          "environments/development/src/my/company"
          "environments/development/src/my"
          "environments/development/src"
          "environments/development/interfaces/my/company/comp1/interface.clj"
          "environments/development/interfaces/my/company/comp1"
          "environments/development/interfaces/my/company"
          "environments/development/interfaces/my"
          "environments/development/interfaces"
          "environments/development/docs/comp1-Readme.md"
          "environments/development/docs"
          "environments/development/project-files/workspace-project.clj"
          "environments/development/project-files/interfaces-project.clj"
          "environments/development/project-files/systems"
          "environments/development/project-files/components/comp1-project.clj"
          "environments/development/project-files/components"
          "environments/development/project-files/bases"
          "environments/development/project-files"
          "environments/development/resources/comp1"
          "environments/development/resources"
          "environments/development/test/my/company/comp1/core_test.clj"
          "environments/development/test/my/company/comp1"
          "environments/development/test/my/company"
          "environments/development/test/my"
          "environments/development/test"
          "environments/development/project.clj"
          "environments/development"
          "environments"
          "project.clj"]
         (file/files ws-dir)))

  (is (= interfaces-interface-content
         (content ws-dir "interfaces/src/my/company/comp1/interface.clj")))

  (is (= interfaces-project-content
         (content ws-dir "interfaces/project.clj")))

  (is (= src-interface-content
         (content ws-dir "components/comp1/src/my/company/comp1/interface.clj")))

  (is (= src-core-content
         (content ws-dir "components/comp1/src/my/company/comp1/core.clj")))

  (is (= comp1-core-test
         (content ws-dir "components/comp1/test/my/company/comp1/core_test.clj")))

  (is (= src-interface-content
         (content ws-dir "environments/development/src/my/company/comp1/interface.clj")))

  (is (= src-core-content
         (content ws-dir "environments/development/src/my/company/comp1/core.clj")))

  (is (= interfaces-interface-content
         (content ws-dir "environments/development/interfaces/my/company/comp1/interface.clj")))

  (is (= workspace-project-content
         (content ws-dir "environments/development/project-files/workspace-project.clj")))

  (is (= interfaces-project-content
         (content ws-dir "environments/development/project-files/interfaces-project.clj")))

  (is (= comp1-project-content
         (content ws-dir "environments/development/project-files/components/comp1-project.clj")))

  (is (= comp1-core-test
         (content ws-dir "environments/development/test/my/company/comp1/core_test.clj")))

  (is (= [['defproject 'my.company/development "1.0"
            :description "The main development environment"
            :dependencies [['org.clojure/clojure "1.9.0"]
                           ['org.clojure/spec.alpha "0.1.143"]]]]
         (content ws-dir "environments/development/project.clj")))

  (is (= workspace-project-content
         (content ws-dir "project.clj"))))
