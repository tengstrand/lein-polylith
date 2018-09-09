(ns leiningen.polylith.cmd.sync-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.sync :as sync]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn entity-content [name type]
  [(str "(defproject com.abc/" name " \"0.1\"\n"
        "  :description \"A " name " " type ".\"\n"
        "  :dependencies [[com.abc/interfaces \"1.0\"]\n"
        "                 [org.clojure/clojure \"1.9.8\"]]\n"
        "  :aot :all)")])

(deftest polylith-sync--with-changed-component-and-base-project-file--sync-project-files-to-match-development-library-versions
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "com.abc")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "system1")
                    (file/replace-file! (str ws-dir "/components/comp1/project.clj")
                                        (entity-content "comp1" "component"))
                    (file/replace-file! (str ws-dir "/bases/system1/project.clj")
                                        (entity-content "system1" "base"))
                    (polylith/polylith project "sync" "deps"))]

      (is (= ["  updated: components/comp1/project.clj"
              "  updated: bases/system1/project.clj"]
             (helper/split-lines output)))

      (is (= (helper/component-project-content "comp1" 'com.abc/comp1 'com.abc/interfaces)
             (helper/content ws-dir "components/comp1/project.clj")))

      (is (= (helper/base-project-content "system1" 'com.abc/system1 'com.abc/interfaces)
             (helper/content ws-dir "bases/system1/project.clj"))))))

(defn replace-file! [path project-ns description dependencies]
  (file/replace-file! path [(str "(defproject " project-ns " \"0.1\"")
                            (str "  :description \"" description "\"")
                            (str "  :dependencies " dependencies "")
                            (str "  :aot :all)")]))

(deftest polylith-sync--update-component-library-versions--system-project-file-is-updated
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "com.abc")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "c" "comp2")
                    (polylith/polylith project "create" "c" "comp3")
                    (polylith/polylith project "create" "s" "system1" "system1")
                    (polylith/polylith project "add" "comp1" "system1")
                    (polylith/polylith project "add" "comp2" "system1")
                    (polylith/polylith project "add" "comp3" "system1")

                    (replace-file! (str ws-dir "/environments/development/project.clj")
                                   "com.abc/development" "The main development environment."
                                   [['org.clojure/clojure "1.9.0"]
                                    ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                                    ['clj-http "3.7.0"]])
                    (replace-file! (str ws-dir "/components/comp1/project.clj")
                                   "com.abc/comp1" "A comp1 component"
                                   [['com.abc/interfaces "1.0"]
                                    ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                                    ['org.clojure/clojure "1.9.0"]])
                    (replace-file! (str ws-dir "/components/comp2/project.clj")
                                   "com.abc/comp2" "A comp2 component"
                                   [['com.abc/interfaces "1.0"]
                                    ['honeysql "0.9.1"]
                                    ['org.clojure/clojure "1.9.0"]])
                    (replace-file! (str ws-dir "/components/comp3/project.clj")
                                   "com.abc/comp3" "A comp3 component"
                                   [['clj-time "0.12.0"]
                                    ['com.abc/interfaces "1.0"]
                                    ['http-kit "2.2.0"]
                                    ['org.clojure/clojure "1.9.0"]])
                    (replace-file! (str ws-dir "/bases/system1/project.clj")
                                   "com.abc/system1" "A system1 system"
                                   [['com.abc/interfaces "1.0"]
                                    ['org.clojure/clojure "1.9.0"]
                                    ['clj-time "0.12.0"]])
                    (polylith/polylith project "sync"))]

      (is (= ["updated: environments/development/project.clj"
              "updated: components/comp1/project.clj"
              "updated: components/comp2/project.clj"
              "updated: components/comp3/project.clj"
              "updated: bases/system1/project.clj"
              "updated: systems/system1/project.clj"]
             (helper/split-lines output)))

      (is (= [['defproject 'com.abc/system1 "0.1"
               :description "A system1 system."
               :dependencies [['clj-time "0.12.0"]
                              ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                              ['honeysql "0.9.1"]
                              ['http-kit "2.2.0"]
                              ['org.clojure/clojure "1.9.0"]]
               :aot :all
               :main 'com.abc.system1.core]]
             (helper/content ws-dir "systems/system1/project.clj")))

      (is (= [['defproject 'com.abc/development "0.1"
                :description "The main development environment."
                :dependencies [['clj-http "3.7.0"]
                               ['clj-time "0.12.0"]
                               ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                               ['honeysql "0.9.1"]
                               ['http-kit "2.2.0"]
                               ['org.clojure/clojure "1.9.0"]]
                :aot
                :all]]
             (helper/content ws-dir "environments/development/project.clj"))))))

(deftest polylith-sync--update-component-library-versions-empty-top-dir--system-project-file-is-updated
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "-")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "c" "comp2")
                    (polylith/polylith project "create" "c" "comp3")
                    (polylith/polylith project "create" "s" "system1" "system1")
                    (polylith/polylith project "add" "comp1" "system1")
                    (polylith/polylith project "add" "comp2" "system1")
                    (polylith/polylith project "add" "comp3" "system1")

                    (replace-file! (str ws-dir "/environments/development/project.clj")
                                   "development" "The main development environment."
                                   [['org.clojure/clojure "1.9.0"]
                                    ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                                    ['clj-http "3.7.0"]])
                    (replace-file! (str ws-dir "/components/comp1/project.clj")
                                   "comp1" "A comp1 component"
                                   [['interfaces "1.0"]
                                    ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                                    ['org.clojure/clojure "1.9.0"]])
                    (replace-file! (str ws-dir "/components/comp2/project.clj")
                                   "comp2" "A comp2 component"
                                   [['interfaces "1.0"]
                                    ['honeysql "0.9.1"]
                                    ['org.clojure/clojure "1.9.0"]])
                    (replace-file! (str ws-dir "/components/comp3/project.clj")
                                   "comp3" "A comp3 component"
                                   [['clj-time "0.12.0"]
                                    ['interfaces "1.0"]
                                    ['http-kit "2.2.0"]
                                    ['org.clojure/clojure "1.9.0"]])
                    (replace-file! (str ws-dir "/bases/system1/project.clj")
                                   "system1" "A system1 system"
                                   [['interfaces "1.0"]
                                    ['org.clojure/clojure "1.9.0"]
                                    ['clj-time "0.12.0"]])
                    (polylith/polylith project "sync" "deps"))]

      (is (= ["updated: environments/development/project.clj"
              "updated: components/comp1/project.clj"
              "updated: components/comp2/project.clj"
              "updated: components/comp3/project.clj"
              "updated: bases/system1/project.clj"
              "updated: systems/system1/project.clj"]
             (helper/split-lines output)))

      (is (= [['defproject 'system1 "0.1"
               :description "A system1 system."
               :dependencies [['clj-time "0.12.0"]
                              ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                              ['honeysql "0.9.1"]
                              ['http-kit "2.2.0"]
                              ['org.clojure/clojure "1.9.0"]]
               :aot :all
               :main 'system1.core]]
             (helper/content ws-dir "systems/system1/project.clj")))

      (is (= [['defproject 'development "0.1"
               :description "The main development environment."
               :dependencies [['clj-http "3.7.0"]
                              ['clj-time "0.12.0"]
                              ['compojure "1.5.1" :exclusions ['com.a/b 'com.a/c]]
                              ['honeysql "0.9.1"]
                              ['http-kit "2.2.0"]
                              ['org.clojure/clojure "1.9.0"]]
               :aot
               :all]]
             (helper/content ws-dir "environments/development/project.clj"))))))

(deftest polylith-sync--system-with-missing-dependencies-that-cant-be-fixed--print-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          base-core-content ["(ns com.abc.base1.core\n"
                             "  (:require [com.abc.interface1.interface :as interface1])\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& args]\n"
                             "  (println \"Hello world!\"))\n"]
          output     (with-out-str
                       (polylith/polylith nil "create" "w" "ws1" "com.abc")
                       (polylith/polylith project "create" "s" "system1" "base1")
                       (polylith/polylith project "create" "c" "comp1" "interface1")
                       (polylith/polylith project "create" "c" "comp2" "interface1")
                       (file/replace-file! (str ws-dir "/bases/base1/src/com/abc/base1/core.clj") base-core-content)
                       (polylith/polylith project "sync" "deps"))]

      (is (= ["FYI: the component comp2 was created but not added to development because it's interface interface1 was already used by comp1."
              "Missing component in system 'system1' for interface 'interface1'. Suggested components: comp1, comp2."]
             (helper/split-lines output))))))

(deftest polylith-sync--with-changed-component-and-base-project-file--sync-project-files-to-match-development-library-versions
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          base-core-content ["(ns com.abc.base1.core\n"
                             "  (:require [com.abc.interface2.interface :as interface2])\n"
                             "  (:gen-class))\n\n"
                             "(defn -main [& args]\n"
                             "  (println \"Hello world!\"))\n"]
          comp1-core-content ["(ns com.abc.comp1.core\n"
                              "  (:require [com.abc.interface3.interface :as interface3]))\n\n"
                              "(defn add-two [x]\n  (+ 2 x))\n"]
          output     (with-out-str
                       (polylith/polylith nil "create" "w" "ws1" "com.abc")
                       (polylith/polylith project "create" "s" "system1" "base1")
                       (polylith/polylith project "create" "c" "comp1" "interface1")
                       (polylith/polylith project "create" "c" "comp2" "interface2")
                       (polylith/polylith project "create" "c" "comp3" "interface3")
                       (polylith/polylith project "add" "comp1" "system1")
                       (file/replace-file! (str ws-dir "/bases/base1/src/com/abc/base1/core.clj") base-core-content)
                       (file/replace-file! (str ws-dir "/components/comp1/src/com/abc/comp1/core.clj") comp1-core-content)
                       (polylith/polylith project "sync" "deps"))]

      ;; se till att build (och eventuellt något kommando till) stoppar i fallet synkningen failar.
      ;; todo: kolla även output + lägg till fler test för fallet när man inte har exakt en implementation av interfacet.

      (is (= ["Added component 'comp3' to system 'system1'."
              "Added component 'comp2' to system 'system1'."]
             (helper/split-lines output)))

      (is (= #{"base1"
               "base1/core.clj"
               "comp1"
               "comp1/core.clj"
               "comp2"
               "comp2/core.clj"
               "comp3"
               "comp3/core.clj"
               "interface1"
               "interface1/interface.clj"
               "interface2"
               "interface2/interface.clj"
               "interface3"
               "interface3/interface.clj"}
             (set (file/relative-paths (str ws-dir "/systems/system1/src/com/abc"))))))))

(deftest index-of-lib-test--exisists--returns-position-in-libs
  (let [lib ['a/b "1.2" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= 1
           (sync/index-of-lib libs lib)))))

(deftest index-of-lib-test--does-not-exisist--returns-nil
  (let [lib ['c/c "1.0" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (nil? (sync/index-of-lib libs lib)))))

(deftest updated-dev-lib--existing-lib-same-version--not-replaced
  (let [lib ['a/a "1.0" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (sync/updated-dev-lib libs lib)))))

(deftest updated-dev-lib--existing-lib-different-version--not-replaced
  (let [lib ['a/a "2.2"]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (sync/updated-dev-lib libs lib)))))

(deftest updated-dev-lib--new-lib--lib-added
  (let [lib ['c/c "2.2" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]
            ['c/c "2.2" :exclusions ['b/c 'b/d]]]
           (sync/updated-dev-lib libs lib)))))

(deftest updated-dev-lib--interfaces-lib--lib-not-added
  (let [lib ['a/interfaces "1.0"]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (sync/updated-dev-lib libs lib)))))

(deftest updated-entity-lib--existing-lib-same-version--replaced
  (let [dev-lib ['a/a "1.0" :exclusions ['b/c 'b/d]]
        entity-libs [['a/a "1.0"]
                     ['a/b "1.1"]]]
    (is (= [['a/a "1.0" :exclusions ['b/c 'b/d]]
            ['a/b "1.1"]]
           (sync/updated-entity-lib entity-libs dev-lib)))))

(deftest updated-entity-lib--existing-lib-different-version--replaced
  (let [dev-lib ['a/a "2.2"]
        entity-libs [['a/a "1.0"]
                     ['a/b "1.1"]]]
    (is (= [['a/a "2.2"]
            ['a/b "1.1"]]
           (sync/updated-entity-lib entity-libs dev-lib)))))

(deftest updated-entity-lib--new-lib--lib-not-added
  (let [dev-lib ['c/c "2.2" :exclusions ['b/c 'b/d]]
        entity-libs [['a/a "1.0"]
                     ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (sync/updated-entity-lib entity-libs dev-lib)))))

(deftest updated-dev-libs--mixed-libs--only-add-new-libs
  (let [libs [['a/b "1.1" :exclusions ['x/x]]
              ['a/a "2.0"]
              ['c/c "2.2" :exclusions ['b/c 'b/d]]
              ['x/interfaces "1.0"]]
        dev-libs [['a/b "1.1"]
                  ['a/c "1.2"]
                  ['a/a "1.0"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]
            ['a/c "1.2"]
            ['c/c "2.2" :exclusions ['b/c 'b/d]]]
           (sync/updated-dev-libs dev-libs libs)))))

(deftest updated-entity-libs--mixed-libs--ignore-new-libs-and-update-existing
  (let [dev-libs [['a/b "1.1" :exclusions ['x/x]]
                  ['a/a "2.0"]
                  ['c/c "2.2" :exclusions ['b/c 'b/d]]]
        entity-libs [['a/b "1.1"]
                     ['a/c "1.2"]
                     ['a/a "1.0"]]]
    (is (= [['a/a "2.0"]
            ['a/b "1.1" :exclusions ['x/x]]
            ['a/c "1.2"]]
           (sync/updated-entity-libs entity-libs dev-libs)))))
