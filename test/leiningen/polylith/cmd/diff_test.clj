(ns leiningen.polylith.cmd.diff-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.git :as git]
            [leiningen.polylith.time :as time]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-diff--create-comp1-and-wait-1-sec-then-create-comp2--returns-files-modified-or-created-after-comp1
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "sys1" "base1")
                    (polylith/polylith project "add" "comp1" "sys1")
                    (time/set-bookmark! ws-dir :last-successful-build)
                    ;; The file system changes the timestamp once per second (at least on Mac!)
                    (Thread/sleep 1000)
                    (polylith/polylith project "create" "c" "comp2")
                    (polylith/polylith project "add" "comp2" "sys1")
                    (polylith/polylith project "diff"))]
      (is (= #{"  components/comp2/readme.md"
               "  components/comp2/project.clj"
               "  components/comp2/src/my/company/comp2/core.clj"
               "  components/comp2/src/my/company/comp2/interface.clj"
               "  components/comp2/test/my/company/comp2/core_test.clj"
               "  environments/development/docs/comp2-readme.md"
               "  environments/development/interfaces/my/company/comp2/interface.clj"
               "  environments/development/project-files/components/comp2-project.clj"
               "  environments/development/src/my/company/comp2/core.clj"
               "  environments/development/src/my/company/comp2/interface.clj"
               "  environments/development/test/my/company/comp2/core_test.clj"
               "  interfaces/src/my/company/comp2/interface.clj"
               "  systems/sys1/src/my/company/comp2/core.clj"
               "  systems/sys1/src/my/company/comp2/interface.clj"
               "  Set :last-successful-build in time.edn"}
             (set (str/split output #"\n")))))))

(deftest polylith-diff--with-ci-create-comp1-and-wait-1-sec-then-create-comp2--returns-files-modified-or-created-after-comp1
  (try
    (with-redefs [file/current-path (fn [] @helper/root-dir)]
      (let [_       (System/setProperty "CI" "CIRCLE")
            ws-dir  (str @helper/root-dir "/ws1")
            project (helper/settings ws-dir "my.company")
            _       (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
            _       (polylith/polylith project "create" "c" "comp1")
            _       (polylith/polylith project "create" "s" "sys1" "base1")
            _       (polylith/polylith project "add" "comp1" "sys1")
            _       (shared/sh "git" "init" :dir ws-dir)
            _       (shared/sh "git" "add" "." :dir ws-dir)
            _       (shared/sh "git" "commit" "-m" "Initial Commit" :dir ws-dir)
            _       (with-out-str (git/set-bookmark! ws-dir :last-successful-build))
            _       (Thread/sleep 1000)
            _       (polylith/polylith project "create" "c" "comp2")
            _       (polylith/polylith project "add" "comp2" "sys1")
            _       (shared/sh "git" "add" "." :dir ws-dir)
            _       (shared/sh "git" "commit" "-m" "Created comp2" :dir ws-dir)
            output  (with-out-str
                      (polylith/polylith project "diff"))
            _       (System/clearProperty "CI")]
        (is (= #{"  components/comp2/readme.md"
                 "  components/comp2/project.clj"
                 "  components/comp2/src/my/company/comp2/core.clj"
                 "  components/comp2/src/my/company/comp2/interface.clj"
                 "  components/comp2/test/my/company/comp2/core_test.clj"
                 "  environments/development/docs/comp2-readme.md"
                 "  environments/development/project-files/components/comp2-project.clj"
                 "  environments/development/resources/comp2"
                 "  environments/development/src/my/company/comp2"
                 "  environments/development/test/my/company/comp2"
                 "  interfaces/src/my/company/comp2/interface.clj"
                 "  systems/sys1/resources/comp2"
                 "  systems/sys1/src/my/company/comp2"}
               (set (str/split output #"\n"))))))
    (catch Exception _
      (System/clearProperty "CI"))))

(deftest polylith-diff--with-ci-and-bookmark-create-comp1-and-wait-1-sec-then-create-comp2--returns-files-modified-or-created-after-comp1
  (try
    (with-redefs [file/current-path (fn [] @helper/root-dir)]
      (let [_       (System/setProperty "CI" "CIRCLE")
            ws-dir  (str @helper/root-dir "/ws1")
            project (helper/settings ws-dir "my.company")
            _       (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
            _       (polylith/polylith project "create" "c" "comp1")
            _       (polylith/polylith project "create" "s" "sys1" "base1")
            _       (polylith/polylith project "add" "comp1" "sys1")
            _       (shared/sh "git" "init" :dir ws-dir)
            _       (shared/sh "git" "add" "." :dir ws-dir)
            _       (shared/sh "git" "commit" "-m" "Initial Commit" :dir ws-dir)
            _       (with-out-str (git/set-bookmark! ws-dir :last-successful-build))
            _       (Thread/sleep 1000)
            _       (polylith/polylith project "create" "c" "comp2")
            _       (polylith/polylith project "add" "comp2" "sys1")
            _       (shared/sh "git" "add" "." :dir ws-dir)
            _       (shared/sh "git" "commit" "-m" "Created comp2" :dir ws-dir)
            output  (with-out-str
                      (polylith/polylith project "diff" "my-bookmark"))
            _       (System/clearProperty "CI")]
        (is (= #{"  readme.md"
                 "  bases/base1/readme.md"
                 "  bases/base1/project.clj"
                 "  bases/base1/src/my/company/base1/core.clj"
                 "  bases/base1/test/my/company/base1/core_test.clj"
                 "  components/comp1/readme.md"
                 "  components/comp1/project.clj"
                 "  components/comp1/src/my/company/comp1/core.clj"
                 "  components/comp1/src/my/company/comp1/interface.clj"
                 "  components/comp1/test/my/company/comp1/core_test.clj"
                 "  components/comp2/readme.md"
                 "  components/comp2/project.clj"
                 "  components/comp2/src/my/company/comp2/core.clj"
                 "  components/comp2/src/my/company/comp2/interface.clj"
                 "  components/comp2/test/my/company/comp2/core_test.clj"
                 "  environments/development/docs/base1-readme.md"
                 "  environments/development/docs/comp1-readme.md"
                 "  environments/development/docs/comp2-readme.md"
                 "  environments/development/docs/sys1-readme.md"
                 "  environments/development/interfaces"
                 "  environments/development/project-files/bases/base1-project.clj"
                 "  environments/development/project-files/components/comp1-project.clj"
                 "  environments/development/project-files/components/comp2-project.clj"
                 "  environments/development/project-files/interfaces-project.clj"
                 "  environments/development/project-files/systems/sys1-project.clj"
                 "  environments/development/project-files/workspace-project.clj"
                 "  environments/development/project.clj"
                 "  environments/development/resources/base1"
                 "  environments/development/resources/comp1"
                 "  environments/development/resources/comp2"
                 "  environments/development/src/my/company/base1"
                 "  environments/development/src/my/company/comp1"
                 "  environments/development/src/my/company/comp2"
                 "  environments/development/test/my/company/base1"
                 "  environments/development/test/my/company/comp1"
                 "  environments/development/test/my/company/comp2"
                 "  interfaces/project.clj"
                 "  interfaces/src/my/company/comp1/interface.clj"
                 "  interfaces/src/my/company/comp2/interface.clj"
                 "  logo.png"
                 "  project.clj"
                 "  systems/sys1/readme.md"
                 "  systems/sys1/build.sh"
                 "  systems/sys1/project.clj"
                 "  systems/sys1/resources/base1"
                 "  systems/sys1/resources/comp1"
                 "  systems/sys1/resources/comp2"
                 "  systems/sys1/src/my/company/base1"
                 "  systems/sys1/src/my/company/comp1"
                 "  systems/sys1/src/my/company/comp2"
                 ""}
               (set (str/split output #"\n"))))))
    (catch Exception _
      (System/clearProperty "CI"))))
