(ns leiningen.polylith.cmd.doc-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith :as polylith]
            [clojure.string :as str]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-doc--with-an-empty-workspace--do-nothing
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
                   (polylith/polylith project "doc" "-browse"))]

      (is (= [""]
             (helper/split-lines output))))))

(deftest polylith-doc--with-missing-template--print-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
                   (polylith/polylith project "doc" "x" "-browse"))]

      (is (str/starts-with? (first (helper/split-lines output))
                            "  Could not find template 'x' in directory")))))

(deftest polylith-doc--with-missing-template--print-error-message
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          content ["(ns system1.core"
                   "  (:require [component1.interface :as component1]"
                   "            [component2.interface :as component2])"
                   "  (:gen-class))"
                   "(defn -main [& args]"
                   "  (component1/add-two 1)"
                   "  (component2/add-two 1))"]
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
                   (polylith/polylith project "create" "s" "system1")
                   (polylith/polylith project "create" "c" "component1")
                   (polylith/polylith project "create" "c" "component2")
                   (polylith/polylith project "add" "component1" "system1")
                   (polylith/polylith project "add" "component2" "system1")
                   (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") content)
                   (polylith/polylith project "doc" "-browse"))]

      (is (= ["<!DOCTYPE html>"
              "<html>"
              "<head>"
              "<title>development</title>"
              ""
              "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">"
              ""
              "</head>"
              "<body>"
              ""
              "<table class=\"polylithTable\">"
              "  <tr>"
              "    <td>component1</td>"
              "    <td class=\"spc\"></td>"
              "    <td>component2</td>"
              "  </tr>"
              "  <tr>"
              "    <td class=\"base\" colspan=3>system1</td>"
              "  </tr>"
              "</table>"
              ""
              "</body>"
              "</html>"]
             (helper/split-lines (slurp (str ws-dir "/doc/development.html"))))))))
