(ns leiningen.polylith.cmd.diff-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.time :as time]))

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
                   ;; The file system is updating the timestamp once per second (at least on Mac!)
                   (Thread/sleep 1000)
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "comp2")
                   (polylith/polylith project "diff"))]
      (is (= (str "  interfaces/src/my/company/comp2/interface.clj\n"
                  "  interfaces/src/my/company/comp2\n"
                  "  interfaces/src/my/company\n"
                  "  components/comp2/src/my/company/comp2/interface.clj\n"
                  "  components/comp2/src/my/company/comp2/core.clj\n"
                  "  components/comp2/src/my/company/comp2\n"
                  "  components/comp2/src/my/company\n"
                  "  components/comp2/src/my\n"
                  "  components/comp2/src\n"
                  "  components/comp2/Readme.md\n"
                  "  components/comp2/resources/comp2\n"
                  "  components/comp2/resources\n"
                  "  components/comp2/test/my/company/comp2/core_test.clj\n"
                  "  components/comp2/test/my/company/comp2\n"
                  "  components/comp2/test/my/company\n"
                  "  components/comp2/test/my\n"
                  "  components/comp2/test\n"
                  "  components/comp2/project.clj\n"
                  "  components/comp2\n"
                  "  components\n"
                  "  environments/development/src/my/company/comp2/interface.clj\n"
                  "  environments/development/src/my/company/comp2/core.clj\n"
                  "  environments/development/src/my/company/comp2\n"
                  "  environments/development/src/my/company\n"
                  "  environments/development/interfaces/my/company/comp2/interface.clj\n"
                  "  environments/development/interfaces/my/company/comp2\n"
                  "  environments/development/interfaces/my/company\n"
                  "  environments/development/docs/comp2-Readme.md\n"
                  "  environments/development/docs\n"
                  "  environments/development/project-files/components/comp2-project.clj\n"
                  "  environments/development/project-files/components\n"
                  "  environments/development/resources/comp2\n"
                  "  environments/development/resources\n"
                  "  environments/development/test/my/company/comp2/core_test.clj\n"
                  "  environments/development/test/my/company/comp2\n"
                  "  environments/development/test/my/company\n")
            output)))))
