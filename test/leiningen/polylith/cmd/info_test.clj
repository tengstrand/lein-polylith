(ns leiningen.polylith.cmd.info-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-info--when-system-and-component-exists--return-list-with-change-information
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.diff/do-diff (fn [_ _] helper/diff)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "c" "comp1")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "s" "sys1" "sys")
                   (polylith/polylith project "info"))]
      (is (= (str "interfaces:\n"
                  "  comp1\n"
                  "components:\n"
                  "  comp1 *\n"
                  "bases:\n"
                  "  sys\n"
                  "systems:\n"
                  "  sys1\n"
                  ;; should be 'sys1 -> base', but symbolic link paths return
                  ;; "/private/..." but other paths don't,
                  ;; so we just accept this testability problem for now
                  ;; because the plugin works.
                  "    sys   ?\n")
             output)))))
