(ns leiningen.polylith.cmd.add-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.add :as add]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-add--add-component-to-system--component-added
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")

          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company" "my/company")
                                      "create" "s" "sys1")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "add" "comp1" "sys1")
                   (polylith/polylith project "info"))]

      (is (= (str "components:\n"
                  "  comp1\n"
                  "bases:\n"
                  "  sys1\n"
                  "systems:\n"
                  "  sys1\n"
                  "    comp1   ?\n"
                  "    sys1    ?\n")
             output))

      (is (= ["interfaces/src/my/company/comp1/interface.clj"
              "interfaces/src/my/company/comp1"
              "interfaces/src/my/company"
              "interfaces/src/my"
              "interfaces/src"
              "interfaces/project.clj"
              "interfaces"
              "systems/sys1/src/my/company/sys1/core.clj"
              "systems/sys1/src/my/company/sys1"
              "systems/sys1/src/my/company/comp1/interface.clj"
              "systems/sys1/src/my/company/comp1/core.clj"
              "systems/sys1/src/my/company/comp1"
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
              "environments/development/src/my/company/sys1/core.clj"
              "environments/development/src/my/company/sys1"
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
              "environments/development/docs/sys1-Readme.md"
              "environments/development/docs"
              "environments/development/project-files/workspace-project.clj"
              "environments/development/project-files/interfaces-project.clj"
              "environments/development/project-files/systems/sys1-project.clj"
              "environments/development/project-files/systems"
              "environments/development/project-files/components/comp1-project.clj"
              "environments/development/project-files/components"
              "environments/development/project-files/bases"
              "environments/development/project-files"
              "environments/development/resources/sys1"
              "environments/development/resources/comp1"
              "environments/development/resources"
              "environments/development/test/my/company/sys1/core_test.clj"
              "environments/development/test/my/company/sys1"
              "environments/development/test/my/company/comp1/core_test.clj"
              "environments/development/test/my/company/comp1"
              "environments/development/test/my/company"
              "environments/development/test/my"
              "environments/development/test"
              "environments/development/project.clj"
              "environments/development"
              "environments"
              "project.clj"]
             (file/files ws-dir))))))



























