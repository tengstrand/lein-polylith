(ns leiningen.polylith.cmd.doc-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :as pp]
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

(deftest polylith-doc--with-system--print-table
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          sys1-content ["(ns system1.core"
                        "  (:require [comp-one.interface :as comp-one]"
                        "            [interface1.interface :as component2]"
                        "            [logger.interface :as logger]"
                        "            [notadded.interface :as notadded])"
                        "  (:gen-class))"
                        "(defn -main [& args]"
                        "  (comp-one/add-two 10)"
                        "  (component2/add-two 10)"
                        "  (logger/add-two 10)"
                        "  (notadded/add-two 10)"
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
      (polylith/polylith project "create" "c" "notadded")
      (polylith/polylith project "add" "comp-one" "system1")
      (polylith/polylith project "add" "component2" "system1")
      (polylith/polylith project "add" "logger" "system1")
      (polylith/polylith project "add" "email" "system1")
      (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
      (file/replace-file! (str ws-dir "/components/comp-one/src/comp_one/core.clj") comp1-content)
      (polylith/polylith project "doc" "-browse")

      (pp/pprint (helper/split-lines (slurp (str ws-dir "/doc/workspace.html"))))

      (is (= [""
              ""
              "<!DOCTYPE html>"
              "<html>"
              "<head>"
              "<title>ws1 (workspace)</title>"
              ""
              "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">"
              ""
              "</head>"
              "<body>"
              ""
              "<script>"
              ""
              "function toggleTableSize(system) {"
              "    var element = document.getElementById(system + \"-medium\");"
              "    if (element.style.display === \"none\") {"
              "        document.getElementById(system + \"-small\").style.display = \"none\";"
              "        document.getElementById(system + \"-medium\").style.display = \"block\";"
              "        document.getElementById(system + \"-ref\").innerHTML = \">-<\";"
              "    } else {"
              "        document.getElementById(system + \"-medium\").style.display = \"none\";"
              "        document.getElementById(system + \"-small\").style.display = \"block\";"
              "        document.getElementById(system + \"-ref\").innerHTML = \"<->\";"
              "    }"
              "}"
              "</script>"
              ""
              "<img src=\"../logo.png\" alt=\"Polylith\" style=\"width:200px;\">"
              ""
              "<p class=\"clear\"/>"
              "    <h3>ws1</h3>"
              "  <div style=\"margin-left: 10px;\">A Polylith workspace.<br></div>"
              "  <p class=\"tiny-clear\"/>"
              ""
              "<h1>Libraries</h1>"
              "<table class=\"entity-table\">"
              "  <tr>"
              "    <td/>"
              "    <td class=\"library-header\"><span class=\"vertical-text\">org.clojure/clojure&nbsp;&nbsp;1.9.0</div></td>"
              "  </tr>"
              "    <tr>"
              "      <td class=\"component-header\" title=\"A comp-one component.\">comp&#8209;one</td>"
              "      <td class=\"center component-row\">&#10003;"
              "</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"component-header\" title=\"A component2 component.\">component2</td>"
              "      <td class=\"center component-row\">&#10003;"
              "</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"component-header\" title=\"A email component.\">email</td>"
              "      <td class=\"center component-row\">&#10003;"
              "</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"component-header\" title=\"A logger component.\">logger</td>"
              "      <td class=\"center component-row\">&#10003;"
              "</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"component-header\" title=\"A notadded component.\">notadded</td>"
              "      <td class=\"center component-row\">&#10003;"
              "</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"base-header\" title=\"A system1 base.\">system1</td>"
              "      <td class=\"center base-row\">&#10003;"
              "</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"environment-header\" title=\"The main development environment.\">development</td>"
              "      <td class=\"center environment-row\">&#10003;"
              "</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"system-header\" title=\"A system1 system.\">system1</td>"
              "      <td class=\"center system-row\">&#10003;"
              "</td>"
              "    </tr>"
              "</table>"
              ""
              ""
              "</body>"
              "</html>"]
             (helper/split-lines (slurp (str ws-dir "/doc/workspace.html"))))))))
