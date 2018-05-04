(ns leiningen.polylith.cmd.create.base
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn create-base [ws-path top-dir top-ns base clojure-version]
  (let [base-path (str ws-path "/bases/" base)
        base-dir (shared/full-name top-dir "/" (shared/src-dir-name base))
        base-readme-content [(str "# " base)]
        proj-ns (shared/full-name top-ns "/" base)
        ns-name (shared/full-name top-ns "." base)
        interfaces-dependencies (shared/full-name top-ns "/" "interfaces")
        base-project-content [(str "(defproject " proj-ns " \"0.1\"")
                              (str "  :description \"A " base " base\"")
                              (str "  :dependencies [[" interfaces-dependencies " \"1.0\"]")
                              (str "                 " (shared/->dependency "org.clojure/clojure" clojure-version) "]")
                              (str "  :aot :all)")]
        core-content [(str "(ns " ns-name ".core")
                      "  (:gen-class))"
                      ""
                      ";; A stand alone base example. Change to the right type of base."
                      "(defn -main [& args]"
                      "  (println \"Hello world!\"))"]
        test-content [(str "(ns " ns-name ".core-test")
                      (str "  (:require [clojure.test :refer :all]")
                      (str "            [" ns-name ".core :as core]))")
                      ""
                      ";; Add tests here..."
                      "(deftest hello-world-example-test"
                      "  (let [output (with-out-str (core/-main))]"
                      "    (is (= \"Hello world!\\n\""
                      "           output))))"]]
       (file/create-dir base-path)

       (file/create-dir (str base-path "/resources"))
       (file/create-file (str base-path "/resources/.keep") [""])
       (file/create-dir (str base-path "/resources/" base))
       (file/create-file (str base-path "/resources/" base "/.keep") [""])
       (shared/create-src-dirs! ws-path (str "bases/" base "/src") [base-dir])
       (shared/create-src-dirs! ws-path (str "bases/" base "/test") [base-dir])

       (file/create-file (str base-path "/Readme.md") base-readme-content)
       (file/create-file (str base-path "/project.clj") base-project-content)
       (file/create-file (str base-path "/src/" base-dir "/core.clj") core-content)
       (file/create-file (str base-path "/test/" base-dir "/core_test.clj") test-content)))
