(ns leiningen.polylith.cmd.sync-deps-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn entity-content [name type]
  [(str "(defproject com.abc/" name " \"0.1\"\n"
        "  :description \"A " name " " type "\"\n"
        "  :dependencies [[com.abc/interfaces \"1.0\"]\n"
        "                 [org.clojure/clojure \"1.9.8\"]]\n"
        "  :aot :all)")])

(deftest polylith-sync--with--changed-component-and-base-project-file--sync-project-files-to-match-development-library-versions
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "com.abc")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1")
                    (file/replace-file! (str ws-dir "/components/comp1/project.clj")
                                        (entity-content "comp1" "component"))
                    (file/replace-file! (str ws-dir "/bases/system1/project.clj")
                                        (entity-content "system1" "base"))
                    (polylith/polylith project "sync-deps"))]

      (is (= ["  updated: components/comp1/project.clj"
              "  updated: bases/system1/project.clj"]
             (helper/split-lines output)))

      (is (= (helper/component-project-content "comp1" 'com.abc/comp1 'com.abc/interfaces)
             (helper/content ws-dir "components/comp1/project.clj")))

      (is (= (helper/base-project-content "system1" 'com.abc/system1 'com.abc/interfaces)
             (helper/content ws-dir "bases/system1/project.clj"))))))

(defn replace-file! [path project-ns description dependencies]
  (file/replace-file! path ["(defproject " project-ns " \"0.1\"\n"
                            "            :description \"" description "\"\n"
                            "            :dependencies " dependencies "\n"
                            "            :aot :all)"]))

(deftest polylith-sync--update-component-library-versions--system-project-file-is-updated
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "com.abc")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "c" "comp2")
                    (polylith/polylith project "create" "c" "comp3")
                    (polylith/polylith project "create" "s" "system1")
                    (polylith/polylith project "add" "comp1" "system1")
                    (polylith/polylith project "add" "comp2" "system1")
                    (polylith/polylith project "add" "comp3" "system1")
                    (replace-file! (str ws-dir "/components/comp1/project.clj")
                                   "com.abc/comp1" "A comp1 component"
                                   [['com.abc/interfaces "1.0"]
                                    ['org.clojure/clojure "1.9.0"]
                                    ['compojure "1.5.1"]])
                    (replace-file! (str ws-dir "/components/comp2/project.clj")
                                   "com.abc/comp2" "A comp2 component"
                                   [['com.abc/interfaces "1.0"]
                                    ['org.clojure/clojure "1.9.0"]
                                    ['honeysql "0.9.1"]])
                    (replace-file! (str ws-dir "/components/comp3/project.clj")
                                   "com.abc/comp3" "A comp3 component"
                                   [['com.abc/interfaces "1.0"]
                                    ['org.clojure/clojure "1.9.0"]
                                    ['clj-time "0.12.0"]
                                    ['http-kit "2.2.0"]])
                    (replace-file! (str ws-dir "/bases/system1/project.clj")
                                   "com.abc/system1" "A system1 system"
                                   [['com.abc/interfaces "1.0"]
                                    ['org.clojure/clojure "1.9.0"]
                                    ['clj-time "0.12.0"]])
                    (polylith/polylith project "sync-deps"))]

      (is (= ["  updated: environments/development/project.clj"
              "  updated: systems/system1/project.clj"]
             (helper/split-lines output)))

      (is (= [['defproject 'com.abc/system1 "0.1"
               :description "A system1 system."
               :dependencies [['clj-time "0.12.0"]
                              ['compojure "1.5.1"]
                              ['honeysql "0.9.1"]
                              ['http-kit "2.2.0"]
                              ['org.clojure/clojure "1.9.0"]]
               :aot :all
               :main 'com.abc.system1.core]]
             (helper/content ws-dir "systems/system1/project.clj")))

      (is (= [['defproject 'com.abc/development "1.0"
               :description "The main development environment"
               :dependencies [['clj-time "0.12.0"]
                              ['compojure "1.5.1"]
                              ['honeysql "0.9.1"]
                              ['http-kit "2.2.0"]
                              ['org.clojure/clojure "1.9.0"]]]]
             (helper/content ws-dir "environments/development/project.clj"))))))
