(ns leiningen.polylith.cmd.create-workspace-fn
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.test-helper :refer [content]]))

(defn create-workspace-with-ns [ws-dir]
  (polylith/polylith nil "create" "w" "ws1" "my.company")

  (is (= ["interfaces/src/my/company"
          "interfaces/src/my"
          "interfaces/src"
          "interfaces/project.clj"
          "interfaces"
          "systems"
          "components"
          "bases"
          "environments/development/src/my/company"
          "environments/development/src/my"
          "environments/development/src"
          "environments/development/interfaces/my/company"
          "environments/development/interfaces/my"
          "environments/development/interfaces"
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

  (is (= [['defproject 'my.company/development "1.0"
           :description "The main development environment"
           :dependencies [['org.clojure/clojure "1.9.0"]
                          ['org.clojure/spec.alpha "0.1.143"]]]]
         (content ws-dir "environments/development/project.clj")))

  (is (= [['defproject 'my.company/development "1.0"
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
                      :vcs                  "git"}]]
         (content ws-dir "environments/development/project-files/workspace-project.clj")))

  (is (= [['defproject 'my.company/interfaces "1.0"
           :description "Component interfaces"
           :dependencies [['org.clojure/clojure "1.9.0"]
                          ['org.clojure/spec.alpha "0.1.143"]]
           :aot
           :all]]
         (content ws-dir "environments/development/project-files/interfaces-project.clj"))))

(defn create-workspace-without-ns [ws-dir]
  (polylith/polylith nil "create" "w" "ws1" "")

  (is (= ["interfaces/src"
          "interfaces/project.clj"
          "interfaces"
          "systems"
          "components"
          "bases"
          "environments/development/src"
          "environments/development/interfaces"
          "environments/development/docs"
          "environments/development/project-files/workspace-project.clj"
          "environments/development/project-files/interfaces-project.clj"
          "environments/development/project-files/systems"
          "environments/development/project-files/components"
          "environments/development/project-files/bases"
          "environments/development/project-files"
          "environments/development/resources"
          "environments/development/test"
          "environments/development/project.clj"
          "environments/development"
          "environments"
          "project.clj"]
         (file/files ws-dir)))

  (is (= [['defproject 'development "1.0"
           :description "The main development environment"
           :dependencies [['org.clojure/clojure "1.9.0"]
                          ['org.clojure/spec.alpha "0.1.143"]]]]
         (content ws-dir "environments/development/project.clj")))

  (is (= [['defproject 'development "1.0"
           :description "The workspace"
           :plugins [['polylith/lein-polylith "0.0.35-alpha"]]
           :polylith {:build-tool           "leiningen"
                      :clojure-spec-version "org.clojure/spec.alpha 0.1.143"
                      :clojure-version      "1.9.0"
                      :example-hash1        "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1"
                      :example-hash2        "58cd8b3106c942f372a40616fe9155c9d2efd122"
                      :ignored-tests        []
                      :top-dir              ""
                      :top-ns               ""
                      :vcs                  "git"}]]
         (content ws-dir "environments/development/project-files/workspace-project.clj")))

  (is (= [['defproject 'interfaces "1.0"
           :description "Component interfaces"
           :dependencies [['org.clojure/clojure "1.9.0"]
                          ['org.clojure/spec.alpha "0.1.143"]]
           :aot
           :all]]
         (content ws-dir "environments/development/project-files/interfaces-project.clj"))))
