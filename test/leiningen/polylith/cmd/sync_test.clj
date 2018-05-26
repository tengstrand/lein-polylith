(ns leiningen.polylith.cmd.sync-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)


(defn entity-content [name type]
  [(str "(defproject com.abc/" name " \"0.1\"\n"
        "  :description \"A " name " " type "\"\n"
        "  :dependencies [[com.abc/interfaces \"1.0\"]\n"
        "                 [org.clojure/clojure \"1.9.8\"]]\n"
        "  :aot :all)")])

(deftest polylith-sync--with--changed-component-and-base-project-file--sync-project-files-to-match-development-library-versions
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "com.abc")]
      (polylith/polylith nil "create" "w" "ws1" "com.abc")
      (polylith/polylith project "create" "c" "comp1")
      (polylith/polylith project "create" "s" "system1")
      (file/replace-file! (str ws-dir "/components/comp1/project.clj")
                          (entity-content "comp1" "component"))
      (file/replace-file! (str ws-dir "/bases/system1/project.clj")
                          (entity-content "system1" "base"))
      (polylith/polylith project "sync")

      (is (= (helper/component-project-content "comp1" 'com.abc/comp1 'com.abc/interfaces)
             (helper/content ws-dir "components/comp1/project.clj")))

      (is (= (helper/base-project-content "system1" 'com.abc/system1 'com.abc/interfaces)
             (helper/content ws-dir "bases/system1/project.clj"))))))
