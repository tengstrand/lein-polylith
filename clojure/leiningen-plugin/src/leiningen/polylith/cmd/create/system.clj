(ns leiningen.polylith.cmd.create.system
  (:require [leiningen.polylith.cmd.create.shared :as shared]
            [leiningen.polylith.cmd.create.base :as create-base]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn create [ws-path top-dir top-ns clojure-version clojure-spec-version system base-name]
  (let [base (if (str/blank? base-name) system base-name)
        ns-name (shared/full-name top-ns "." base)
        proj-dir (shared/full-name top-dir "/" base)]
    (when-not (file/file-exists (str ws-path "/bases/" proj-dir))
      (create-base/create-base ws-path top-dir top-ns base clojure-version clojure-spec-version))))



    ;(when-not interface-already-created?
    ;  (create-ifc/create-interface ws-path top-dir top-ns base))

    ;      interface-ns-name (shared/full-name top-ns "." base)
    ;      component-ns-name (shared/full-name top-ns "." system)
    ;      proj-dir (shared/full-name top-dir "/" system)
    ;      proj-ns (shared/full-name top-ns "/" system)
    ;      interfaces-dependencies (shared/full-name top-ns "/" "interfaces")
    ;      delegate-content [(str "(ns " interface-ns-name ".interface")
    ;                        (str "  (:require [" component-ns-name ".core :as core]))")
    ;                        ""
    ;                        ";; delegate to the implementations..."
    ;                        "(defn myfn [x]"
    ;                        "  (core/myfn x))"]
    ;      core-content [(str "(ns " component-ns-name ".core)")
    ;                    ""
    ;                    ";; add your functions here..."
    ;                    "(defn myfn [x]"
    ;                    "  (+ 2 x))"]
    ;      doc-content [(str "# " system " component")
    ;                   ""
    ;                   "add documentation here..."]
    ;      test-content [(str "(ns " component-ns-name ".core-test")
    ;                    (str "  (:require [clojure.test :refer :all]")
    ;                    (str "            [" interface-ns-name ".interface :as interface]))")
    ;                    ""
    ;                    ";; add your tests here..."
    ;                    "(deftest test-myfn"
    ;                    "  (is (= 42 (interface/myfn 40))))"]
    ;      project-content [(str "(defproject " proj-ns " \"0.1\"")
    ;                       (str "  :description \"A " system " component\"")
    ;                       (str "  :dependencies [[" interfaces-dependencies " \"1.0\"]")
    ;                       (str "                 " (shared/->dependency "org.clojure/clojure" clojure-version))
    ;                       (str "                 " (shared/->dependency "org.clojure/spec" clojure-spec-version) "]")
    ;                       (str "  :aot :all)")]
    ;      dev-dirs (file/directory-names (str ws-path "/environments"))]
    ;  (file/create-dir comp-dir)
    ;  (file/create-dir (str comp-dir "/resources"))
    ;  (file/create-dir (str comp-dir "/resources/" base))
    ;  (shared/create-src-dirs! ws-path (str "components/" system "/src") [interface-proj-dir proj-dir])
    ;  (shared/create-src-dirs! ws-path (str "components/" system "/test") [proj-dir])

    ;  (file/create-file (str comp-dir "/Readme.md") doc-content)
    ;  (file/create-file (str comp-dir "/src/" interface-proj-dir "/interface.clj") delegate-content)
    ;  (file/create-file (str comp-dir "/src/" proj-dir "/core.clj") core-content)
    ;  (file/create-file (str comp-dir "/test/" proj-dir "/core_test.clj") test-content)
