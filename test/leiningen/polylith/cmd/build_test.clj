(ns leiningen.polylith.cmd.build-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.git :as git]
            [leiningen.polylith.time :as time]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(def time-atom (atom 0))

(defn fake-current-time []
  (swap! time-atom inc)
  (* @time-atom 1200))

(deftest polylith-build--build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn
                time/current-time                fake-current-time]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "s" "system1" "base1")
                    (helper/execute-polylith project "build"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling workspace interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Compiling bases/base1"
              (str "(lein compile :dir " ws-dir "/bases/base1)")
              "Compiling systems/system1"
              (str "(lein compile :dir " ws-dir "/systems/system1)")
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.base1.core-test my.company.comp1.core-test"
              (str "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")
              "set :last-success in .polylith/time.edn"
              ""
              "Execution time: 3.6 seconds"]
             (helper/split-lines output)))
      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-success))))))

(deftest polylith-build--skip-compile-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn
                time/current-time                fake-current-time]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "s" "system1" "base1")
                    (helper/execute-polylith project "build" "-compile"))]
      (is (= [""
              "Changed systems: system1"
              ""
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.base1.core-test my.company.comp1.core-test"
              (str "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")
              "set :last-success in .polylith/time.edn"
              ""
              "Execution time: 2.4 seconds"]
             (helper/split-lines output)))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-success))))))

(deftest polylith-build--skip-test-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn
                time/current-time                fake-current-time]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "s" "system1" "base1")
                    (helper/execute-polylith project "build" "-test"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling workspace interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Compiling bases/base1"
              (str "(lein compile :dir " ws-dir "/bases/base1)")
              "Compiling systems/system1"
              (str "(lein compile :dir " ws-dir "/systems/system1)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")
              "set :last-success in .polylith/time.edn"
              ""
              "Execution time: 2.4 seconds"]
             (helper/split-lines output)))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-success))))))

(deftest polylith-build--skip-success-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn
                time/current-time                fake-current-time]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                    (helper/execute-polylith project "create" "c" "comp1")
                    (helper/execute-polylith project "create" "s" "system1" "base1")
                    (helper/execute-polylith project "build" "-success"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling workspace interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Compiling bases/base1"
              (str "(lein compile :dir " ws-dir "/bases/base1)")
              "Compiling systems/system1"
              (str "(lein compile :dir " ws-dir "/systems/system1)")
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.base1.core-test my.company.comp1.core-test"
              (str "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")
              ""
              "Execution time: 3.6 seconds"]
             (helper/split-lines output)))

      (is (= 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-success))))))

(deftest polylith-build--on-ci-build-changed-systems--print-output
  (try
    (with-redefs [file/current-path (fn [] @helper/root-dir)
                  time/current-time fake-current-time]
      (let [_       (System/setProperty "CI" "CIRCLE")
            ws-dir  (str @helper/root-dir "/ws1")
            project (helper/settings ws-dir "my.company")
            _       (helper/execute-polylith nil "create" "w" "ws1" "my.company")
            sha-1   (-> (helper/content ws-dir ".polylith/git.edn") first :last-success)
            _       (with-out-str (git/set-bookmark! ws-dir :last-success))
            sha-2   (-> (helper/content ws-dir ".polylith/git.edn") first :last-success)
            _       (helper/execute-polylith project "create" "c" "comp1")
            _       (helper/execute-polylith project "create" "s" "system1" "base1")
            _       (helper/execute-polylith project "add" "comp1" "system1")
            _       (shared/sh "git" "add" "." :dir ws-dir)
            _       (shared/sh "git" "commit" "-m" "Created comp1" :dir ws-dir)
            output  (with-out-str
                      (helper/execute-polylith project "build"))
            sha-3   (-> (helper/content ws-dir ".polylith/git.edn") first :last-success)
            _       (System/clearProperty "CI")
            prefix  (if (str/includes? output "/private") "/private" "")]
        (is (= (str "\n"
                    "Changed components: comp1\n"
                    "Changed bases: base1\n"
                    "Changed systems: system1\n"
                    "\n"
                    "Compiling workspace interfaces\n"
                    "Created " prefix ws-dir "/interfaces/target/interfaces-1.0.jar\n"
                    "Wrote " prefix ws-dir "/interfaces/pom.xml\n"
                    "Installed jar and pom into local repo.\n"
                    "\n"
                    "Compiling components/comp1\n"
                    "\n"
                    "Compiling bases/base1\n"
                    "\n"
                    "Compiling systems/system1\n"
                    "\n"
                    "Start execution of tests in 2 namespaces:\n"
                    "lein test my.company.base1.core-test my.company.comp1.core-test\n"
                    "\n"
                    "lein test my.company.base1.core-test\n"
                    "\n"
                    "lein test my.company.comp1.core-test\n"
                    "\n"
                    "Ran 2 tests containing 2 assertions.\n"
                    "0 failures, 0 errors.\n"
                    "\n"
                    "Building systems/system1\n"
                    "Created " prefix ws-dir "/systems/system1/target/system1-0.1.jar\n"
                    "Created " prefix ws-dir "/systems/system1/target/system1-0.1-standalone.jar\n"
                    "\n"
                    "set :last-success in .polylith/git.edn"
                    "\n"
                    "\n"
                    "Execution time: 3.6 seconds"
                    "\n")
               output))

        (is (= 0 (-> (helper/content ws-dir ".polylith/time.edn")
                     first :last-success)))

        (is (nil? sha-1))
        (is (not (nil? sha-2)))
        (is (not (nil? sha-3)))
        (is (not= sha-2 sha-3))))
    (catch Exception _
      (System/clearProperty "CI"))))

(deftest polylith-build--cyclic-dependencies-with-namespace--print-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                time/current-time fake-current-time]
    (let [ws-dir        (str @helper/root-dir "/ws1")
          project       (helper/settings ws-dir "my.company")
          core1-content ["(ns my.company.component1.core"
                         "  (:require [my.company.component3.interface :as component3]))"
                         "(defn add-two [x]"
                         "  (component3/add-two x))"]
          core2-content ["(ns my.company.component2.core"
                         "  (:require [my.company.interface1.interface :as interface1]))"
                         "(defn add-two [x]"
                         "  (interface1/add-two x))"]
          core3-content ["(ns my.company.component3.core"
                         "  (:require [my.company.component2.interface :as component2]))"
                         "(defn add-two [x]"
                         "  (component2/add-two x))"]
          base1-content ["(ns my.company.base1.core"
                         "  (:require [my.company.component2.interface :as component2])"
                         "  (:gen-class))\n\n(defn -main [& args]"
                         "  (component2/add-two 1))"]
          output        (with-out-str
                          (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                          (helper/execute-polylith project "create" "s" "system1" "base1")
                          (helper/execute-polylith project "create" "c" "component1" "interface1")
                          (helper/execute-polylith project "create" "c" "component2")
                          (helper/execute-polylith project "create" "c" "component3")
                          (helper/execute-polylith project "add" "component1" "system1")
                          (helper/execute-polylith project "add" "component2" "system1")
                          (helper/execute-polylith project "add" "component3" "system1")
                          (file/replace-file! (str ws-dir "/components/component1/src/my/company/interface1/core.clj") core1-content)
                          (file/replace-file! (str ws-dir "/components/component2/src/my/company/component2/core.clj") core2-content)
                          (file/replace-file! (str ws-dir "/components/component3/src/my/company/component3/core.clj") core3-content)
                          (file/replace-file! (str ws-dir "/bases/base1/src/my/company/base1/core.clj") base1-content)
                          (helper/execute-polylith project "build"))]

      (is (= ["Cannot compile: circular dependencies detected. Type 'info' for more details."]
             (helper/split-lines output))))))

(deftest polylith-build--cyclic-dependencies-with-namespace-skip-circular-deps--builds-with-circular-deps
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                time/current-time fake-current-time]
    (let [ws-dir        (str @helper/root-dir "/ws1")
          project       (helper/settings ws-dir "my.company")
          core1-content ["(ns my.company.interface1.core"
                         "  (:require [my.company.component3.interface :as component3]))"
                         "(defn add-two [x]"
                         "  (component3/add-two x))"]
          core2-content ["(ns my.company.component2.core"
                         "  (:require [my.company.interface1.interface :as interface1]))"
                         "(defn add-two [x]"
                         "  (interface1/add-two x))"]
          core3-content ["(ns my.company.component3.core"
                         "  (:require [my.company.component2.interface :as component2]))"
                         "(defn add-two [x]"
                         "  (component2/add-two x))"]
          base1-content ["(ns my.company.base1.core"
                         "  (:require [my.company.component2.interface :as component2])"
                         "  (:gen-class))\n\n(defn -main [& args]"
                         "  (component2/add-two 1))"]
          output        (with-out-str
                          (helper/execute-polylith nil "create" "w" "ws1" "my.company" "-git")
                          (helper/execute-polylith project "create" "s" "system1" "base1")
                          (helper/execute-polylith project "create" "c" "component1" "interface1")
                          (helper/execute-polylith project "create" "c" "component2")
                          (helper/execute-polylith project "create" "c" "component3")
                          (helper/execute-polylith project "add" "component1" "system1")
                          (helper/execute-polylith project "add" "component2" "system1")
                          (helper/execute-polylith project "add" "component3" "system1")
                          (file/replace-file! (str ws-dir "/components/component1/src/my/company/interface1/core.clj") core1-content)
                          (file/replace-file! (str ws-dir "/components/component2/src/my/company/component2/core.clj") core2-content)
                          (file/replace-file! (str ws-dir "/components/component3/src/my/company/component3/core.clj") core3-content)
                          (file/replace-file! (str ws-dir "/bases/base1/src/my/company/base1/core.clj") base1-content)
                          (helper/execute-polylith-without-printing-error-messages project "build" "-circular-deps"))
          prefix        (if (str/includes? output "/private") "/private" "")]

      (is (= [""
              "Changed components: component3 component2 component1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling workspace interfaces"
              (str "Created " prefix ws-dir "/interfaces/target/interfaces-1.0.jar")
              (str "Wrote " prefix ws-dir "/interfaces/pom.xml")
              "Installed jar and pom into local repo."
              ""
              "Compiling components/component1"
              ""
              "Compiling components/component2"
              ""
              "Compiling components/component3"
              ""
              "Compiling bases/base1"
              ""
              "Compiling systems/system1"]
             (helper/split-lines output))))))
