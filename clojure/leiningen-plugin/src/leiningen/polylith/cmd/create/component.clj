(ns leiningen.polylith.cmd.create.component
  (:require [leiningen.polylith.cmd.create.shared :as shared]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn create-dev-links [ws-path dev-dir name top-name]
  (let [dir (str ws-path "/environments/" dev-dir)
        levels (+ 2 (count (str/split top-name #"/")))
        parent-src-path (str/join (repeat levels "../"))
        path (str "../../../components/" name)
        src-path (str parent-src-path "components/" name)]
    (file/create-symlink (str dir "/docs/" name "-Readme.md")
                         (str path "/Readme.md"))
    (file/create-symlink (str dir "/resources/" name)
                         (str path "/resources/" name))
    (file/create-symlink (str dir "/project-files/components/" name "-project.clj")
                         (str "../" path "/project.clj"))
    (file/create-symlink (str dir "/src/" top-name)
                         (str src-path "/src/" top-name))
    (file/create-symlink (str dir "/test/" top-name)
                         (str src-path "/test/" top-name))))

(defn full-name [top separator name]
  (if (zero? (count top)) name (str top separator name)))

(defn create [ws-path top-dir top-ns clojure-version clojure-spec-version name]
  (let [comp-dir (str ws-path "/components/" name)
        ns-name (full-name top-ns "." name)
        proj-dir (full-name top-dir "/" name)
        proj-ns (full-name top-ns "/" name)
        interfaces-dep (full-name top-ns "/" "interfaces")
        interface-content [(str "(ns " ns-name ".interface)")
                           ""
                           ";; add your functions here..."
                           "(defn myfn [x])"]
        delegate-content [(str "(ns " ns-name ".interface")
                          (str "  (:require [" ns-name ".core :as core]))")
                          ""
                          ";; delegate to the implementations..."
                          "(defn myfn [x]"
                          "  (core/myfn x))"]
        core-content [(str "(ns " ns-name ".core)")
                      ""
                      ";; add your functions here..."
                      "(defn myfn [x]"
                      "  (+ 2 x))"]
        doc-content [(str "# " name " component")
                     ""
                     "add documentation here..."]
        test-content [(str "(ns " ns-name ".core-test")
                      (str "  (:require [clojure.test :refer :all]")
                      (str "            [" ns-name ".core :as core]))")
                      ""
                      ";; add your tests here..."
                      "(deftest test-myfn"
                      "  (is (= 42 (core/myfn 40))))"]
        project-content [(str "(defproject " proj-ns " \"0.1\"")
                         (str "  :description \"A " name " component\"")
                         (str "  :dependencies [[" interfaces-dep " \"1.0\"]")
                         (str "                 " (shared/->dependency "org.clojure/clojure" clojure-version))
                         (str "                 " (shared/->dependency "org.clojure/spec" clojure-spec-version) "]")
                         (str "  :aot :all)")]
        dev-dirs (file/directory-names (str ws-path "/environments"))]
    (file/create-dir comp-dir)
    (file/create-dir (str comp-dir "/resources"))
    (file/create-dir (str comp-dir "/resources/" name))
    (shared/create-src-dirs! ws-path proj-dir "interfaces/src")
    (shared/create-src-dirs! ws-path proj-dir (str "components/" name "/src"))
    (shared/create-src-dirs! ws-path proj-dir (str "components/" name "/test"))
    (file/create-file (str comp-dir "/project.clj") project-content)
    (file/create-file (str ws-path "/interfaces/src/" proj-dir "/interface.clj") interface-content)
    (file/create-file (str comp-dir "/Readme.md") doc-content)
    (file/create-file (str comp-dir "/src/" proj-dir "/interface.clj") delegate-content)
    (file/create-file (str comp-dir "/src/" proj-dir "/core.clj") core-content)
    (file/create-file (str comp-dir "/test/" proj-dir "/core_test.clj") test-content)

    (doseq [dev-dir dev-dirs]
      (create-dev-links ws-path dev-dir name proj-dir))))
