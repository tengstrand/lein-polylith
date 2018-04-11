(ns leiningen.polylith.cmd.create.component
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.create.interface :as create-ifc]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn create-dev-links [ws-path dev-dir component interface-proj-dir proj-dir]
  (let [dir (str ws-path "/environments/" dev-dir)
        parent-src-path (shared/parent-path proj-dir)
        path (str "../../../components/" component)
        src-path (str parent-src-path "components/" component)]
    (file/create-symlink (str dir "/docs/" component "-Readme.md")
                         (str path "/Readme.md"))
    (file/create-symlink (str dir "/project-files/components/" component "-project.clj")
                         (str "../" path "/project.clj"))
    (file/create-symlink (str dir "/src/" proj-dir)
                         (str src-path "/src/" proj-dir))
    (file/create-symlink-if-not-exists (str dir "/test/" proj-dir)
                                       (str src-path "/test/" proj-dir))
    (file/create-symlink-if-not-exists (str dir "/resources/" component)
                                       (str path "/resources/" component))
    (file/create-symlink-if-not-exists (str dir "/src/" interface-proj-dir)
                                       (str src-path "/src/" interface-proj-dir))))

(defn create [ws-path top-dir top-ns clojure-version clojure-spec-version component interface-name]
  (let [interface (if (str/blank? interface-name) component interface-name)
        interface-proj-dir (shared/full-name top-dir "/" interface)
        comp-dir (str ws-path "/components/" component)
        interface-ns-name (shared/full-name top-ns "." interface)
        component-ns-name (shared/full-name top-ns "." component)
        proj-dir (shared/full-name top-dir "/" component)
        proj-ns (shared/full-name top-ns "/" component)
        interfaces-dependencies (shared/full-name top-ns "/" "interfaces")
        delegate-content [(str "(ns " interface-ns-name ".interface")
                          (str "  (:require [" component-ns-name ".core :as core]))")
                          ""
                          ";; delegate to the implementations..."
                          "(defn add-two [x]"
                          "  (core/add-two x))"]
        core-content [(str "(ns " component-ns-name ".core)")
                      ""
                      ";; add your functions here..."
                      "(defn add-two [x]"
                      "  (+ 2 x))"]
        doc-content [(str "# " component " component")
                     ""
                     "add documentation here..."]
        test-content [(str "(ns " component-ns-name ".core-test")
                      (str "  (:require [clojure.test :refer :all]")
                      (str "            [" interface-ns-name ".interface :as interface]))")
                      ""
                      ";; add your tests here..."
                      "(deftest test-add-two"
                      "  (is (= 42 (interface/add-two 40))))"]
        project-content [(str "(defproject " proj-ns " \"0.1\"")
                         (str "  :description \"A " component " component\"")
                         (str "  :dependencies [[" interfaces-dependencies " \"1.0\"]")
                         (str "                 " (shared/->dependency "org.clojure/clojure" clojure-version))
                         (str "                 " (shared/->dependency "org.clojure/spec" clojure-spec-version) "]")
                         (str "  :aot :all)")]
        dev-dirs (file/directory-names (str ws-path "/environments"))]

    (file/create-dir comp-dir)
    (file/create-dir (str comp-dir "/resources"))
    (file/create-dir (str comp-dir "/resources/" component))
    (shared/create-src-dirs! ws-path (str "components/" component "/src") [interface-proj-dir proj-dir])
    (shared/create-src-dirs! ws-path (str "components/" component "/test") [proj-dir])
    (file/create-file (str comp-dir "/project.clj") project-content)
    (file/create-file (str comp-dir "/Readme.md") doc-content)
    (file/create-file (str comp-dir "/src/" interface-proj-dir "/interface.clj") delegate-content)
    (file/create-file (str comp-dir "/src/" proj-dir "/core.clj") core-content)
    (file/create-file (str comp-dir "/test/" proj-dir "/core_test.clj") test-content)

    (when-not (file/file-exists (str ws-path "/interfaces/src/" interface-proj-dir))
      (create-ifc/create-interface ws-path top-dir top-ns interface))

    (doseq [dev-dir dev-dirs]
      (create-dev-links ws-path dev-dir component interface-proj-dir proj-dir))))
