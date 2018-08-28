(ns leiningen.polylith.cmd.doc-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :as pp]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith :as polylith]
            [clojure.string :as str]))

(use-fixtures :each helper/test-setup-and-tear-down)

;(deftest polylith-doc--with-an-empty-workspace--do-nothing
;  (with-redefs [file/current-path (fn [] @helper/root-dir)]
;    (let [ws-dir (str @helper/root-dir "/ws1")
;          project (helper/settings ws-dir "")
;          output (with-out-str
;                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
;                   (polylith/polylith project "doc" "-browse"))])))

      ;(is (= [""]
      ;       (helper/split-lines output))))))

;(deftest polylith-doc--with-system--print-table
;  (with-redefs [file/current-path (fn [] @helper/root-dir)]
;    (let [ws-dir (str @helper/root-dir "/ws1")
;          project (helper/settings ws-dir "")
;          sys1-content ["(ns system1.core"
;                        "  (:require [comp-one.interface :as comp-one]"
;                        "            [interface1.interface :as component2]"
;                        "            [logger.interface :as logger]"
;                        "            [notadded.interface :as notadded])"
;                        "  (:gen-class))"
;                        "(defn -main [& args]"
;                        "  (comp-one/add-two 10)"
;                        "  (component2/add-two 10)"
;                        "  (logger/add-two 10)"
;                        "  (notadded/add-two 10)"
;                        "  (println \"Hello world!\"))"]
;
;          comp1-content ["(ns comp-one.core"
;                         "  (:require [logger.interface :as logger]))"
;                         "(defn add-two [x]\n  (logger/add-two x))"]]
;      (polylith/polylith nil "create" "w" "ws1" "" "-git")
;      (polylith/polylith project "create" "s" "system1")
;      (polylith/polylith project "create" "c" "comp-one")
;      (polylith/polylith project "create" "c" "component2" "interface1")
;      (polylith/polylith project "create" "c" "logger")
;      (polylith/polylith project "create" "c" "email")
;      (polylith/polylith project "create" "c" "notadded")
;      (polylith/polylith project "add" "comp-one" "system1")
;      (polylith/polylith project "add" "component2" "system1")
;      (polylith/polylith project "add" "logger" "system1")
;      (polylith/polylith project "add" "email" "system1")
;      (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
;      (file/replace-file! (str ws-dir "/components/comp-one/src/comp_one/core.clj") comp1-content)
;      (polylith/polylith project "doc" "-browse"))))

      ;(pp/pprint (helper/split-lines (slurp (str ws-dir "/doc/workspace.html")))))))

      ;(is (= []
      ;       (helper/split-lines (slurp (str ws-dir "/doc/workspace.html"))))))))
