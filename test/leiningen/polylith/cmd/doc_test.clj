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

(comment
  (deftest polylith-doc--with-system--print-table
    (with-redefs [file/current-path (fn [] @helper/root-dir)]
      (let [ws-dir (str @helper/root-dir "/ws1")
            project (helper/settings ws-dir "")
            sys1-content ["(ns system1.core"
                          "  (:require [comp-one.interface :as comp-one]"
                          "            [interface1.interface :as component2]"
                          "            [logger.interface :as logger])"
                          "  (:gen-class))"
                          "(defn -main [& args]"
                          "  (comp-one/add-two 10)"
                          "  (component2/add-two 10)"
                          "  (logger/add-two 10)"
                          "  (println \"Hello world!\"))"]

            comp1-content ["(ns comp-one.core"
                           "  (:require [logger.interface :as logger]))"
                           "(defn add-two [x]\n  (logger/add-two x))"]]
        (polylith/polylith nil "create" "w" "ws1" "" "-git")
        (polylith/polylith project "create" "s" "system1")
        (polylith/polylith project "create" "c" "comp-one")
        (polylith/polylith project "create" "c" "component2" "interface1")
        (polylith/polylith project "create" "c" "logger")
        (polylith/polylith project "create" "c" "email")
        (polylith/polylith project "add" "comp-one" "system1")
        (polylith/polylith project "add" "component2" "system1")
        (polylith/polylith project "add" "logger" "system1")
        (polylith/polylith project "add" "email" "system1")
        (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
        (file/replace-file! (str ws-dir "/components/comp-one/src/comp_one/core.clj") comp1-content)
        (polylith/polylith project "doc" "-browse")

        (is (= ["<!DOCTYPE html>"
                "<html>"
                "<head>"
                "<title>ws1</title>"
                ""
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">"
                ""
                "</head>"
                "<body>"
                ""
                "<img src=\"../../logo.png\" alt=\"Polylith\" style=\"width:200px;\">"
                ""
                "<h1>ws1</h1>"
                ""
                "<h4>Libraries:</h4>"
                "<div class=\"library\" title=\"1.9.0\">org.clojure/clojure</div>"
                "<p class=\"clear\"/>"
                ""
                "<h4>Interfaces:</h4>"
                "<div class=\"interface\">comp&#8209;one</div>"
                "<div class=\"interface\">email</div>"
                "<div class=\"interface\">interface1</div>"
                "<div class=\"interface\">logger</div>"
                "<p class=\"clear\"/>"
                ""
                "<h4>Components:</h4>"
                "  <div class=\"component\">comp&#8209;one</div>"
                "  <div class=\"com-container\">"
                "    <div class=\"com\">component2</div>"
                "    <div class=\"ifc\">interface1</div>"
                "  </div>"
                "  <div class=\"component\">email</div>"
                "  <div class=\"component\">logger</div>"
                "<p class=\"clear\"/>"
                ""
                "<h4>Bases:</h4>"
                "<div class=\"base\">system1</div>"
                "<p class=\"clear\"/>"
                ""
                "<h4>development:</h4>"
                "<div class=\"component\">comp&#8209;one</div>"
                "<div class=\"com-container\">"
                "  <div class=\"com\">component2</div>"
                "  <div class=\"ifc\">interface1</div>"
                "</div>"
                "<div class=\"component\">email</div>"
                "<div class=\"component\">logger</div>"
                "<div class=\"bas\">system1</div>"
                "<p class=\"clear\"/>"
                ""
                "<h4>system1:</h4>"
                " <table class=\"deps-table\">"
                "  <tr>"
                "    <td class=\"comp\">logger</td>"
                "    <td class=\"spc\"></td>"
                "    <td class=\"comp\"></td>"
                "    <td class=\"spc\"></td>"
                "    <td class=\"comp\"></td>"
                "  </tr>"
                "  <tr>"
                "    <td class=\"comp\">comp&#8209;one</td>"
                "    <td class=\"spc\"></td>"
                "    <td class=\"comp\">component2</td>"
                "    <td class=\"spc\"></td>"
                "    <td class=\"comp\">logger</td>"
                "  </tr>"
                "  <tr>"
                "    <td class=\"tbase\" colspan=5>system1</td>"
                "    <td class=\"spc\"></td>"
                "    <td class=\"comp\">email</td>"
                "  </tr>"
                "</table>"
                ""
                "</body>"
                "</html>"]
               (helper/split-lines (slurp (str ws-dir "/doc/output/workspace.html")))))))))
