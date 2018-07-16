(ns leiningen.polylith.cmd.build-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.git :as git]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-build--build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling interfaces"
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
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))
      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-compile-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build" "-compile"))]
      (is (= [""
              "Changed systems: system1"
              ""
              "Start execution of tests in 2 namespaces:"
              "lein test my.company.base1.core-test my.company.comp1.core-test"
              (str "(lein test my.company.base1.core-test my.company.comp1.core-test :dir " ws-dir "/environments/development)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-test-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build" "-test"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              "Compiling bases/base1"
              (str "(lein compile :dir " ws-dir "/bases/base1)")
              "Compiling systems/system1"
              (str "(lein compile :dir " ws-dir "/systems/system1)")
              "Building systems/system1"
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))

      (is (< 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-build--skip-success-and-build-changed-systems--print-output
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "create" "s" "system1" "base1")
                    (polylith/polylith project "build" "-success"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling interfaces"
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
              (str "(./build.sh :dir " ws-dir "/systems/system1)")]
             (helper/split-lines output)))

      (is (= 0 (-> (helper/content ws-dir ".polylith/time.edn")
                   first :last-successful-build))))))

(deftest polylith-build--on-ci-build-changed-systems--print-output
  (try
    (with-redefs [file/current-path (fn [] @helper/root-dir)]
      (let [_       (System/setProperty "CI" "CIRCLE")
            ws-dir  (str @helper/root-dir "/ws1")
            project (helper/settings ws-dir "my.company")
            _       (polylith/polylith nil "create" "w" "ws1" "my.company")
            sha-1   (-> (helper/content ws-dir ".polylith/git.edn") first :last-successful-build)
            _       (git/set-bookmark! ws-dir :last-successful-build)
            sha-2   (-> (helper/content ws-dir ".polylith/git.edn") first :last-successful-build)
            _       (polylith/polylith project "create" "c" "comp1")
            _       (polylith/polylith project "create" "s" "system1" "base1")
            _       (polylith/polylith project "add" "comp1" "system1")
            _       (shared/sh "git" "add" "." :dir ws-dir)
            _       (shared/sh "git" "commit" "-m" "Created comp1" :dir ws-dir)
            output  (with-out-str
                      (polylith/polylith project "build"))
            sha-3   (-> (helper/content ws-dir ".polylith/git.edn") first :last-successful-build)
            _       (System/clearProperty "CI")
            prefix  (if (str/includes? output "/private") "/private" "")]
        (is (= (str "\n"
                    "Changed components: comp1\n"
                    "Changed bases: base1\n"
                    "Changed systems: system1\n"
                    "\n"
                    "Compiling interfaces\n"
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
                    "\n")
               output))

        (is (= 0 (-> (helper/content ws-dir ".polylith/time.edn")
                     first :last-successful-build)))

        (is (nil? sha-1))
        (is (not (nil? sha-2)))
        (is (not (nil? sha-3)))
        (is (not= sha-2 sha-3))))
    (catch Exception _
      (System/clearProperty "CI"))))

(deftest polylith-build--cyclic-dependencies-with-namespace--print-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir        (str @helper/root-dir "/ws1")
          project       (helper/settings ws-dir "my.company")
          core1-content [(str "(ns my.company.component1.core\n"
                              "  (:require [my.company.component3.interface :as component3]))\n\n"
                              "(defn add-two [x]\n"
                              "  (component3/add-two x))")]
          core2-content [(str "(ns my.company.component2.core\n"
                              "  (:require [my.company.interface1.interface :as interface1]))\n\n"
                              "(defn add-two [x]\n"
                              "  (interface1/add-two x))")]
          core3-content [(str "(ns my.company.component3.core\n"
                              "  (:require [my.company.component2.interface :as component2]))\n\n"
                              "(defn add-two [x]\n"
                              "  (component2/add-two x))")]
          base1-content [(str "(ns my.company.base1.core\n"
                              "  (:require [my.company.component2.interface :as component2])\n"
                              "  (:gen-class))\n\n(defn -main [& args]\n"
                              "  (component2/add-two 1))\n")]
          exception     (atom nil)
          output        (with-out-str
                          (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                          (polylith/polylith project "create" "s" "system1" "base1")
                          (polylith/polylith project "create" "c" "component1" "interface1")
                          (polylith/polylith project "create" "c" "component2")
                          (polylith/polylith project "create" "c" "component3")
                          (polylith/polylith project "add" "component1" "system1")
                          (polylith/polylith project "add" "component2" "system1")
                          (polylith/polylith project "add" "component3" "system1")
                          (file/replace-file! (str ws-dir "/components/component1/src/my/company/component1/core.clj") core1-content)
                          (file/replace-file! (str ws-dir "/components/component2/src/my/company/component2/core.clj") core2-content)
                          (file/replace-file! (str ws-dir "/components/component3/src/my/company/component3/core.clj") core3-content)
                          (file/replace-file! (str ws-dir "/bases/base1/src/my/company/base1/core.clj") base1-content)
                          (try
                            (polylith/polylith project "build")
                            (catch Exception e
                              (swap! exception conj e))))]

      (is (= ["Cannot compile: circular dependencies detected."
              ""
              "interfaces:"
              "  component2 *"
              "  component3 *"
              "  interface1 *"
              "components:"
              "  component1 *   > interface1"
              "  component2 *"
              "  component3 *"
              "bases:"
              "  base1 *"
              "systems:"
              "  system1 *"
              "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)"
              "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)"
              "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)"
              "    base1 *        -> base       (circular deps: base1 > component2 > component1 > component3 > component2)"
              "environments:"
              "  development"
              "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)"
              "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)"
              "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)"
              "    base1 *        -> base       (circular deps: base1 > component2 > component1 > component3 > component2)"]
             (helper/split-lines output)))

      (is (= "Cannot compile: circular dependencies detected." (-> @exception first .getLocalizedMessage))))))

(deftest polylith-build--cyclic-dependencies-with-namespace-skip-circular-deps--builds-with-circular-deps
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir        (str @helper/root-dir "/ws1")
          project       (helper/settings ws-dir "my.company")
          core1-content [(str "(ns my.company.component1.core\n"
                              "  (:require [my.company.component3.interface :as component3]))\n\n"
                              "(defn add-two [x]\n"
                              "  (component3/add-two x))")]
          core2-content [(str "(ns my.company.component2.core\n"
                              "  (:require [my.company.interface1.interface :as interface1]))\n\n"
                              "(defn add-two [x]\n"
                              "  (interface1/add-two x))")]
          core3-content [(str "(ns my.company.component3.core\n"
                              "  (:require [my.company.component2.interface :as component2]))\n\n"
                              "(defn add-two [x]\n"
                              "  (component2/add-two x))")]
          base1-content [(str "(ns my.company.base1.core\n"
                              "  (:require [my.company.component2.interface :as component2])\n"
                              "  (:gen-class))\n\n(defn -main [& args]\n"
                              "  (component2/add-two 1))\n")]
          exception     (atom nil)
          output        (with-out-str
                          (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                          (polylith/polylith project "create" "s" "system1" "base1")
                          (polylith/polylith project "create" "c" "component1" "interface1")
                          (polylith/polylith project "create" "c" "component2")
                          (polylith/polylith project "create" "c" "component3")
                          (polylith/polylith project "add" "component1" "system1")
                          (polylith/polylith project "add" "component2" "system1")
                          (polylith/polylith project "add" "component3" "system1")
                          (file/replace-file! (str ws-dir "/components/component1/src/my/company/component1/core.clj") core1-content)
                          (file/replace-file! (str ws-dir "/components/component2/src/my/company/component2/core.clj") core2-content)
                          (file/replace-file! (str ws-dir "/components/component3/src/my/company/component3/core.clj") core3-content)
                          (file/replace-file! (str ws-dir "/bases/base1/src/my/company/base1/core.clj") base1-content)
                          (try
                            (polylith/polylith project "build" "-circular-deps")
                            (catch Exception e
                              (swap! exception conj e))))
          prefix  (if (str/includes? output "/private") "/private" "")]

      (is (= [""
              "Changed components: component3 component2 component1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Compiling interfaces"
              (str "Created " prefix ws-dir "/interfaces/target/interfaces-1.0.jar")
              (str "Wrote " prefix ws-dir "/interfaces/pom.xml")
              "Installed jar and pom into local repo."
              ""
              "Compiling components/component3"
              ""
              "Compiling components/component2"
              ""
              "Compiling components/component1"
              ""
              "Compiling bases/base1"
              ""
              "Compiling systems/system1"]
             (helper/split-lines output)))

      (is (str/starts-with? (-> @exception first .getLocalizedMessage) "Shell Err: Compiling my.company.component2.core")))))
