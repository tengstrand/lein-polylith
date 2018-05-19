(ns leiningen.polylith.cmd.compile-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(deftest polylith-compile--with-print-argument--print-tests
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith project "create" "c" "comp1")
                   (polylith/polylith project "compile"))]
      (is (= (str "\n"
                  "Changed components: comp1\n"
                  "Changed bases:\n"
                  "Changed systems:\n"
                  "\n"
                  "Compiling interfaces\n"
                  "(lein install :dir " ws-dir "/interfaces)\n"
                  "Compiling components/comp1\n"
                  "(lein compile :dir " ws-dir "/components/comp1)\n")
             output)))))

(deftest polylith-compile--cyclic-dependencies-with-namespace--print-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
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
          exception (atom nil)
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
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
                     (polylith/polylith project "compile")
                     (catch Exception e
                       (swap! exception conj e))))]

      (is (= (str "\n"
                  "Changed components: component3 component2 component1\n"
                  "Changed bases: base1\n"
                  "Changed systems: system1\n"
                  "\n"
                  "Cannot compile: circular dependencies detected.\n"
                  "\n"
                  "interfaces:\n"
                  "  component2 *\n"
                  "  component3 *\n"
                  "  interface1 *\n"
                  "components:\n"
                  "  component1 *   > interface1\n"
                  "  component2 *\n"
                  "  component3 *\n"
                  "bases:\n"
                  "  base1 *\n"
                  "systems:\n"
                  "  system1 *\n"
                  "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)\n"
                  "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)\n"
                  "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)\n"
                  "    base1 *        -> base       (circular deps: base1 > component2 > component1 > component3 > component2)\n"
                  "environments:\n"
                  "  development\n"
                  "    component1 *   -> component  (circular deps: component1 > component3 > component2 > component1)\n"
                  "    component2 *   -> component  (circular deps: component2 > component1 > component3 > component2)\n"
                  "    component3 *   -> component  (circular deps: component3 > component2 > component1 > component3)\n"
                  "    base1 *        -> base       (circular deps: base1 > component2 > component1 > component3 > component2)\n")
             output))

      (is (= "Cannot compile: circular dependencies detected." (-> @exception first .getLocalizedMessage))))))
