(ns leiningen.polylith.cmd.sync-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.sync.shared :as shared]
            [leiningen.polylith.cmd.sync.environments :as env]
            [leiningen.polylith.cmd.sync.entities :as ent]))

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
                    (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "s" "system1" "system1")
                    (file/replace-file! (str ws-dir "/components/comp1/project.clj")
                                        (entity-content "comp1" "component"))
                    (file/replace-file! (str ws-dir "/bases/system1/project.clj")
                                        (entity-content "system1" "base"))
                    (helper/execute-polylith project "sync" "+deps"))]

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
                    (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "c" "comp2")
                    (helper/execute-polylith project "create" "c" "comp3")
                    (helper/execute-polylith project "create" "s" "system1" "system1")
                    (helper/execute-polylith project "add" "comp1" "system1")
                    (helper/execute-polylith project "add" "comp2" "system1")
                    (helper/execute-polylith project "add" "comp3" "system1")

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
                    (helper/execute-polylith project "sync"))]

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
                    (helper/execute-polylith nil "create" "w" "ws1" "-")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "c" "comp2")
                    (helper/execute-polylith project "create" "c" "comp3")
                    (helper/execute-polylith project "create" "s" "system1" "system1")
                    (helper/execute-polylith project "add" "comp1" "system1")
                    (helper/execute-polylith project "add" "comp2" "system1")
                    (helper/execute-polylith project "add" "comp3" "system1")

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
                    (helper/execute-polylith project "sync" "+deps"))]

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
                       (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                       (helper/execute-polylith project "create" "s" "system1" "base1")
                       (helper/execute-polylith project "create" "c" "comp1" "interface1")
                       (helper/execute-polylith project "create" "c" "comp2" "interface1")
                       (file/replace-file! (str ws-dir "/bases/base1/src/com/abc/base1/core.clj") base-core-content)
                       (helper/execute-polylith project "sync" "+deps"))]

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
                       (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                       (helper/execute-polylith project "create" "s" "system1" "base1")
                       (helper/execute-polylith project "create" "c" "comp1" "interface1")
                       (helper/execute-polylith project "create" "c" "comp2" "interface2")
                       (helper/execute-polylith project "create" "c" "comp3" "interface3")
                       (helper/execute-polylith project "add" "comp1" "system1")
                       (file/replace-file! (str ws-dir "/bases/base1/src/com/abc/base1/core.clj") base-core-content)
                       (file/replace-file! (str ws-dir "/components/comp1/src/com/abc/interface1/core.clj") comp1-core-content)
                       (helper/execute-polylith project "sync"))]

      (is (= ["Added component 'comp3' to system 'system1'."
              "Added component 'comp2' to system 'system1'."]
             (helper/split-lines output)))

      (is (= #{"base1"
               "base1/core.clj"
               "interface1"
               "interface1/core.clj"
               "interface1/interface.clj"
               "interface2"
               "interface2/core.clj"
               "interface2/interface.clj"
               "interface3"
               "interface3/core.clj"
               "interface3/interface.clj"}
             (set (file/relative-paths (str ws-dir "/systems/system1/src/com/abc"))))))))

(deftest index-of-lib-test--exisists--returns-position-in-libs
  (let [lib ['a/b "1.2" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= 1
           (shared/index-of-lib libs lib)))))

(deftest index-of-lib-test--does-not-exisist--returns-nil
  (let [lib ['c/c "1.0" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (nil? (shared/index-of-lib libs lib)))))

(deftest updated-dev-lib--existing-lib-same-version--not-replaced
  (let [lib ['a/a "1.0" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (env/updated-dev-lib libs lib)))))

(deftest updated-dev-lib--existing-lib-different-version--not-replaced
  (let [lib ['a/a "2.2"]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (env/updated-dev-lib libs lib)))))

(deftest updated-dev-lib--new-lib--lib-added
  (let [lib ['c/c "2.2" :exclusions ['b/c 'b/d]]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]
            ['c/c "2.2" :exclusions ['b/c 'b/d]]]
           (env/updated-dev-lib libs lib)))))

(deftest updated-dev-lib--interfaces-lib--lib-not-added
  (let [lib ['a/interfaces "1.0"]
        libs [['a/a "1.0"]
              ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (env/updated-dev-lib libs lib)))))

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
           (env/updated-dev-libs dev-libs libs)))))

(deftest updated-entity-lib--existing-lib-same-version--replaced
  (let [dev-lib ['a/a "1.0" :exclusions ['b/c 'b/d]]
        entity-libs [['a/a "1.0"]
                     ['a/b "1.1"]]]
    (is (= [['a/a "1.0" :exclusions ['b/c 'b/d]]
            ['a/b "1.1"]]
           (ent/updated-entity-lib entity-libs dev-lib)))))

(deftest updated-entity-lib--existing-lib-different-version--replaced
  (let [dev-lib ['a/a "2.2"]
        entity-libs [['a/a "1.0"]
                     ['a/b "1.1"]]]
    (is (= [['a/a "2.2"]
            ['a/b "1.1"]]
           (ent/updated-entity-lib entity-libs dev-lib)))))

(deftest updated-entity-lib--new-lib--lib-not-added
  (let [dev-lib ['c/c "2.2" :exclusions ['b/c 'b/d]]
        entity-libs [['a/a "1.0"]
                     ['a/b "1.1"]]]
    (is (= [['a/a "1.0"]
            ['a/b "1.1"]]
           (ent/updated-entity-lib entity-libs dev-lib)))))

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
           (ent/updated-entity-libs entity-libs dev-libs)))))

(defn ws-interface [ns]
  [(str "(ns com.abc." ns ")\n\n")
   "(def var1 123)\n\n"
   "(defmacro macro2 [pred a b])\n\n"
   "(defn func2 [])\n"])

(defn comp-ns [ns]
  [(str "(ns com.abc." ns ")\n")])

(defn comp-ifc [ns]
  [(str "(ns com.abc." ns "\n")
   "  (:require [com.abc.comp1.core :as core]))\n\n"
   "(def var1 123)\n\n"
   "(def var2 \"data\")\n\n"
   "(defmacro macro1 [pred a b]\n"
   "  `~a)\n\n"
   "(defmacro macro2 [pred a b]\n"
   "  `(if (not ~pred) ~a ~b))\n\n"
   "(defn func1 [a]\n"
   "  (println \"a=\" a))\n\n"
   "(defn func2 []\n"
   "  (println \"Hello\"))\n\n"
   "(defn func3\n"
   "  ([a] (func3 \"hello\" a))\n"
   "  ([a b] (println b a)))\n"])

(deftest polylith-sync--missing-component-namespace--returns-error
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          ws-comp2-v2-path (str ws-dir "/interfaces/src/com/abc/comp2/v2/interface.clj")
          output (with-out-str
                   (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                   (helper/execute-polylith project "create" "c" "comp2")
                   (file/create-dir (str ws-dir "/interfaces/src/com/abc/comp2/v2"))
                   (file/replace-file! ws-comp2-v2-path (ws-interface "comp2.v2.interface"))
                   (helper/execute-polylith project "sync"))]

      (is (= ["Expected to find interface 'components/comp2/src/com/abc/comp2/v2/interface.clj'."]
             (helper/split-lines output))))))

(deftest polylith-sync--with-different-function-arities-in-component-interfaces--return-error-messages
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          ws-ifc1-content [(str "(ns com.abc.ifc1.interface)\n\n")
                           "(defn func [])\n"]
          comp1-ifc1-content [(str "(ns com.abc.ifc1.interface)\n\n")
                              "(defn func [x])\n"]
          ws-ifc1-path (str ws-dir "/interfaces/src/com/abc/ifc1/interface.clj")
          ws-comp1-path (str ws-dir "/components/comp1/src/com/abc/ifc1/interface.clj")
          output (with-out-str
                   (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                   (helper/execute-polylith project "create" "c" "comp1" "ifc1")
                   (file/replace-file! ws-ifc1-path ws-ifc1-content)
                   (file/replace-file! ws-comp1-path comp1-ifc1-content)
                   (helper/execute-polylith project "sync"))]

      (is (= ["Workspace interfaces are out of sync in 'interfaces/src/com/abc/ifc1/interface.clj': \"function 'func' with arity 1 must be added manually.\""]
             (helper/split-lines output))))))

(deftest polylith-sync--with-new-definitions-in-component-interfaces--add-missing-definitions-to-the-workspace-interface
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          ws-ifc1-path (str ws-dir "/interfaces/src/com/abc/ifc_1/interface.clj")
          ws-comp2-v1-path (str ws-dir "/interfaces/src/com/abc/comp_2/interface.clj")
          ws-comp2-v2-path (str ws-dir "/interfaces/src/com/abc/comp_2/v2/interface.clj")
          output (with-out-str
                   (helper/execute-polylith nil "create" "w" "ws1" "com.abc")
                   (helper/execute-polylith project "create" "c" "comp-1" "ifc-1")
                   (helper/execute-polylith project "create" "c" "comp-2")
                   (helper/execute-polylith project "create" "c" "comp-2b" "comp-2")
                   (file/create-dir (str ws-dir "/interfaces/src/com/abc/comp_2/v2"))
                   (file/create-dir (str ws-dir "/components/comp-2/src/com/abc/comp_2/v2"))
                   (file/create-dir (str ws-dir "/components/comp-2b/src/com/abc/comp_2/v2"))
                   (file/replace-file! ws-ifc1-path (ws-interface "ifc-1.interface"))
                   (file/replace-file! ws-comp2-v1-path (ws-interface "comp-2.interface"))
                   (file/replace-file! ws-comp2-v2-path (ws-interface "comp-2.v2.interface"))
                   (file/replace-file! (str ws-dir "/components/comp-1/src/com/abc/ifc_1/interface.clj") (comp-ifc "ifc-1.interface"))
                   (file/replace-file! (str ws-dir "/components/comp-2/src/com/abc/comp_2/interface.clj") (comp-ifc "comp-2.interface"))
                   (file/replace-file! (str ws-dir "/components/comp-2b/src/com/abc/comp_2/interface.clj") (comp-ifc "comp-2.interface"))
                   (file/replace-file! (str ws-dir "/components/comp-2/src/com/abc/comp_2/v2/interface.clj") (comp-ifc "comp-2.v2.interface"))
                   (file/replace-file! (str ws-dir "/components/comp-2b/src/com/abc/comp_2/v2/interface.clj") (comp-ifc "comp-2.v2.interface"))
                   (file/replace-file! (str ws-dir "/components/comp-1/src/com/abc/ifc_1/core.clj") (comp-ns "comp-1.core"))
                   (file/replace-file! (str ws-dir "/components/comp-2/src/com/abc/comp_2/core.clj") (comp-ns "comp-2.core"))
                   (file/replace-file! (str ws-dir "/components/comp-2b/src/com/abc/comp_2/core.clj") (comp-ns "comp-2b.core"))
                   (helper/execute-polylith project "sync"))]

      (is (= ["FYI: the component comp-2b was created but not added to development because it's interface comp-2 was already used by comp-2."
              "Added these definitions to 'interfaces/src/com/abc/comp_2/interface.clj':"
              "  (def var2)"
              "  (defmacro macro1 [pred a b])"
              "  (defn func1 [a])"
              "  (defn func3 [a])"
              "  (defn func3 [a b])"
              "Added these definitions to 'interfaces/src/com/abc/comp_2/v2/interface.clj':"
              "  (def var2)"
              "  (defmacro macro1 [pred a b])"
              "  (defn func1 [a])"
              "  (defn func3 [a])"
              "  (defn func3 [a b])"
              "Added these definitions to 'interfaces/src/com/abc/ifc_1/interface.clj':"
              "  (def var2)"
              "  (defmacro macro1 [pred a b])"
              "  (defn func1 [a])"
              "  (defn func3 [a])"
              "  (defn func3 [a b])"]
             (helper/split-lines output)))

      (is (= [['ns 'com.abc.ifc-1.interface]
              ['def 'var1 123]
              ['defmacro 'macro2 ['pred 'a 'b]]
              ['defn 'func2 []]
              ['def 'var2]
              ['defmacro 'macro1 ['pred 'a 'b]]
              ['defn 'func1 ['a]]
              ['defn 'func3 ['a]]
              ['defn 'func3 ['a 'b]]]
             (file/read-file ws-ifc1-path))))))
