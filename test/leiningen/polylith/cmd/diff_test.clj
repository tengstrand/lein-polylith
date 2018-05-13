(ns leiningen.polylith.cmd.diff-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.time :as time]
            [clojure.string :as str]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-diff--create-comp1-and-wait-1-sec-then-create-comp2--returns-files-modified-or-created-after-comp1
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "comp1")
                   (time/set-last-successful-build! ws-dir)
                   ;; The file system updated the timestamp once per second (at least on Mac!)
                   (Thread/sleep 1000)
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "comp2")
                   (polylith/polylith project "diff"))]
      (is (= #{"  components"
               "  components/comp2"
               "  components/comp2/Readme.md"
               "  components/comp2/project.clj"
               "  components/comp2/resources"
               "  components/comp2/resources/comp2"
               "  components/comp2/src"
               "  components/comp2/src/my"
               "  components/comp2/src/my/company"
               "  components/comp2/src/my/company/comp2"
               "  components/comp2/src/my/company/comp2/core.clj"
               "  components/comp2/src/my/company/comp2/interface.clj"
               "  components/comp2/test"
               "  components/comp2/test/my"
               "  components/comp2/test/my/company"
               "  components/comp2/test/my/company/comp2"
               "  components/comp2/test/my/company/comp2/core_test.clj"
               "  environments/development/docs"
               "  environments/development/docs/comp2-Readme.md"
               "  environments/development/interfaces/my/company"
               "  environments/development/interfaces/my/company/comp2"
               "  environments/development/interfaces/my/company/comp2/interface.clj"
               "  environments/development/project-files/components"
               "  environments/development/project-files/components/comp2-project.clj"
               "  environments/development/resources"
               "  environments/development/resources/comp2"
               "  environments/development/sources/src/my/company"
               "  environments/development/sources/src/my/company/comp2"
               "  environments/development/sources/src/my/company/comp2/core.clj"
               "  environments/development/sources/src/my/company/comp2/interface.clj"
               "  environments/development/tests/test/my/company"
               "  environments/development/tests/test/my/company/comp2"
               "  environments/development/tests/test/my/company/comp2/core_test.clj"
               "  interfaces/src/my/company"
               "  interfaces/src/my/company/comp2"
               "  interfaces/src/my/company/comp2/interface.clj"}
             (set (str/split output #"\n")))))))
