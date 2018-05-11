(ns leiningen.polylith.cmd.create.component
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.create.interface :as create-ifc]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn create-dev-links? [ws-path top-dir dev-dir interface]
  (let [dir (str ws-path "/environments/" dev-dir "/sources/src/" top-dir)
        entities (set (file/directory-names dir))]
    (not (contains? entities interface))))

(defn create-dev-links! [ws-path dev-dir component interface-dir component-dir]
  (let [root (str ws-path "/environments/" dev-dir)
        relative-parent-path (shared/relative-parent-path component-dir 3)
        path (str "../../../components/" component)
        relative-component-path (str relative-parent-path "components/" component)]
    (file/create-symlink (str root "/docs/" component "-Readme.md")
                         (str path "/Readme.md"))
    (file/create-symlink (str root "/project-files/components/" component "-project.clj")
                         (str "../" path "/project.clj"))
    (file/create-symlink (str root "/sources/src/" component-dir)
                         (str relative-component-path "/src/" component-dir))
    (file/create-symlink (str root "/tests/test/" component-dir)
                         (str relative-component-path "/test/" component-dir))
    (file/create-symlink (str root "/sources/src/" interface-dir)
                         (str relative-component-path "/src/" interface-dir))
    (file/create-symlink (str root "/resources/" component)
                         (str path "/resources/" component))))

(defn create [ws-path top-dir top-ns clojure-version component interface-name]
  (let [interface (if (str/blank? interface-name) component interface-name)
        interface-dir (shared/full-name top-dir "/" (shared/src-dir-name interface))
        component-dir (shared/full-name top-dir "/" (shared/src-dir-name component))
        comp-root-dir (str ws-path "/components/" component)
        interface-ns-name (shared/full-name top-ns "." interface)
        component-ns-name (shared/full-name top-ns "." component)
        project-ns (shared/full-name top-ns "/" component)
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
        project-content [(str "(defproject " project-ns " \"0.1\"")
                         (str "  :description \"A " component " component\"")
                         (str "  :dependencies [[" interfaces-dependencies " \"1.0\"]")
                         (str "                 " (shared/->dependency "org.clojure/clojure" clojure-version) "]")
                         (str "  :aot :all)")]
        dev-dirs (file/directory-names (str ws-path "/environments"))]

    (file/create-dir comp-root-dir)
    (file/create-dir (str comp-root-dir "/resources"))
    (file/create-file (str comp-root-dir "/resources/.keep") [""])
    (file/create-dir (str comp-root-dir "/resources/" component))
    (file/create-file (str comp-root-dir "/resources/" component "/.keep") [""])
    (shared/create-src-dirs! ws-path (str "components/" component "/src") [interface-dir component-dir])
    (shared/create-src-dirs! ws-path (str "components/" component "/test") [component-dir])
    (file/create-file (str comp-root-dir "/project.clj") project-content)
    (file/create-file (str comp-root-dir "/Readme.md") doc-content)
    (file/create-file (str comp-root-dir "/src/" interface-dir "/interface.clj") delegate-content)
    (file/create-file (str comp-root-dir "/src/" component-dir "/core.clj") core-content)
    (file/create-file (str comp-root-dir "/test/" component-dir "/core_test.clj") test-content)

    (when-not (file/file-exists (str ws-path "/interfaces/src/" interface-dir))
      (create-ifc/create-interface ws-path top-dir top-ns interface))

    (doseq [dev-dir dev-dirs]
      (when (create-dev-links? ws-path top-dir dev-dir interface)
        (create-dev-links! ws-path dev-dir component interface-dir component-dir)))))
