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

(deftest polylith-doc--with-system--print-table
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          content ["(ns system1.core"
                   "  (:require [component1.interface :as component1]"
                   "            [component2.interface :as component2])"
                   "  (:gen-class))"
                   "(defn -main [& args]"
                   "  (component1/add-two 1)"
                   "  (component2/add-two 1))"]]
      (polylith/polylith nil "create" "w" "ws1" "" "-git")
      (polylith/polylith project "create" "s" "system1")
      (polylith/polylith project "create" "c" "component1")
      (polylith/polylith project "create" "c" "component2" "interface1")
      (polylith/polylith project "add" "component1" "system1")
      (polylith/polylith project "add" "component2" "system1")
      (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") content)
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
              "<img src=\"../logo.png\" alt=\"Polylith\" style=\"width:200px;\">"
              ""
              "<h1>ws1</h1>"
              ""
              "<h4>libraries:</h4>"
              "<div class=\"library\" title=\"1.0\">interfaces</div>"
              "<div class=\"library\" title=\"1.9.0\">org.clojure/clojure</div>"
              "<p class=\"clear\"/>"
              ""
              "<h4>Interfaces:</h4>"
              "<div class=\"interface\">component1</div>"
              "<div class=\"interface\">interface1</div>"
              "<p class=\"clear\"/>"
              ""
              "<h4>Components:</h4>"
              "  <div class=\"component\">component1</div>"
              "  <div class=\"com-container\">"
              "    <div class=\"com\">component2</div>"
              "    <div class=\"ifc\">interface1</div>"
              "  </div>"
              "<p class=\"clear\"/>"
              ""
              "<h4>Bases:</h4>"
              "<div class=\"base\">system1</div>"
              "<p class=\"clear\"/>"
              ""
              "<h4>development:</h4>"
              "<div class=\"component\">component1</div>"
              "<div class=\"com-container\">"
              "  <div class=\"com\">component2</div>"
              "  <div class=\"ifc\">interface1</div>"
              "</div>"
              "<div class=\"bas\">system1</div>"
              "<p class=\"clear\"/>"
              ""
              "<h4>system1:</h4>"
              " <table class=\"design\">"
              "  <tr>"
              "    <td class=\"comp\">component1</td>"
              "  </tr>"
              "  <tr>"
              "    <td class=\"tbase\">system1</td>"
              "  </tr>"
              "</table>"
              ""
              "</body>"
              "</html>"]
             (helper/split-lines (slurp (str ws-dir "/doc/development.html"))))))))
