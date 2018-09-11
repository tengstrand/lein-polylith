(ns leiningen.polylith.cmd.compile-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]))

(use-fixtures :each helper/test-setup-and-tear-down)

(defn fake-fn [& args]
  args)

(def time-atom (atom 0))

(defn fake-current-time []
  (swap! time-atom inc)
  (* @time-atom 1200))


(deftest polylith-compile--with-print-argument--print-tests
  (with-redefs [file/current-path                (fn [] @helper/root-dir)
                leiningen.polylith.cmd.shared/sh fake-fn
                time/current-time                fake-current-time]
    (let [ws-dir  (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output  (with-out-str
                    (polylith/polylith nil "create" "w" "ws1" "my.company" "-git")
                    (polylith/polylith project "create" "c" "comp1")
                    (polylith/polylith project "compile"))]
      (is (= [""
              "Changed components: comp1"
              "Changed bases:"
              "Changed systems:"
              ""
              "Compiling workspace interfaces"
              (str "(lein install :dir " ws-dir "/interfaces)")
              "Compiling components/comp1"
              (str "(lein compile :dir " ws-dir "/components/comp1)")
              ""
              "Execution time: 1 seconds 200 milliseconds"]
             (helper/split-lines output))))))

(deftest polylith-compile--cyclic-dependencies-with-namespace--print-info
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                time/current-time                fake-current-time]
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
                            (polylith/polylith project "compile")
                            (catch Exception e
                              (swap! exception conj e))))]

      (is (= [""
              "Changed components: component3 component2 component1"
              "Changed bases: base1"
              "Changed systems: system1"
              ""
              "Cannot compile: circular dependencies detected."
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
