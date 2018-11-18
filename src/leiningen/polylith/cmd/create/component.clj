(ns leiningen.polylith.cmd.create.component
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.create.interface :as create-ifc]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [clojure.set :as set]))

(defn create-dev-links? [ws-path top-dir dev-dir interface]
  (let [dir (str ws-path "/environments/" dev-dir "/src/" top-dir)
        entities (set (file/directory-names dir))]
    (not (contains? entities interface))))

(defn used-by-component [ws-path top-dir dev-dir interface]
  (let [dir (str ws-path "/environments/" dev-dir "/src/" top-dir)
        paths (file/directories dir)
        entities (set (map #(shared/link->entity ws-path %) paths))
        components (set/intersection entities (shared/all-components ws-path))]
    (first (filter #(= interface (shared/interface-of ws-path top-dir %)) components))))

(defn create-dev-links! [ws-path dev-dir component interface interface-dir component-dir]
  (let [root (str ws-path "/environments/" dev-dir)
        relative-parent-path (shared/relative-parent-path component-dir)
        path (str "../../../components/" component)
        relative-component-path (str relative-parent-path "components/" component)]
    (file/create-symlink (str root "/docs/" component "-readme.md")
                         (str path "/readme.md"))
    (file/create-symlink (str root "/project-files/components/" component "-project.clj")
                         (str "../" path "/project.clj"))
    (file/create-symlink (str root "/test/" interface-dir)
                         (str relative-component-path "/test/" interface-dir))
    (file/create-symlink (str root "/src/" interface-dir)
                         (str relative-component-path "/src/" interface-dir))
    (file/create-symlink (str root "/resources/" interface)
                         (str path "/resources/" interface))))

(defn create [ws-path top-dir top-ns clojure-version component interface-name]
  (let [interface               (if (str/blank? interface-name) component interface-name)
        interface-dir           (shared/full-dir-name top-dir interface)
        component-dir           (shared/full-dir-name top-dir component)
        comp-root-dir           (str ws-path "/components/" component)
        interface-ns-name       (shared/full-name top-ns "." interface)
        project-ns              (shared/full-name top-ns "/" component)
        interfaces-dependencies (shared/full-name top-ns "/" "interfaces")
        delegate-content        [(str "(ns " interface-ns-name ".interface")
                                 (str "  (:require [" interface-ns-name ".core :as core]))")
                                 ""
                                 ";; delegate to the implementations..."
                                 "(defn add-two [x]"
                                 "  (core/add-two x))"]
        core-content            [(str "(ns " interface-ns-name ".core)")
                                 ""
                                 ";; add your functions here..."
                                 "(defn add-two [x]"
                                 "  (+ 2 x))"]
        doc-content             [(str "# " component " component")
                                 ""
                                 "add documentation here..."]
        test-content            [(str "(ns " interface-ns-name ".core-test")
                                 (str "  (:require [clojure.test :refer :all]")
                                 (str "            [" interface-ns-name ".interface :as " interface "]))")
                                 ""
                                 ";; add your tests here..."
                                 "(deftest test-add-two"
                                 (str "  (is (= 42 (" interface "/add-two 40))))")]
        project-content         [(str "(defproject " project-ns " \"0.1\"")
                                 (str "  :description \"A " component " component.\"")
                                 (str "  :dependencies [[" interfaces-dependencies " \"1.0\"]")
                                 (str "                 " (shared/->dependency "org.clojure/clojure" clojure-version) "]")
                                 (str "  :aot :all)")]
        dev-dirs                (file/directory-names (str ws-path "/environments"))]

    (file/create-dir comp-root-dir)
    (file/create-dir (str comp-root-dir "/resources"))
    (file/create-file (str comp-root-dir "/resources/.keep") [""])
    (file/create-dir (str comp-root-dir "/resources/" interface))
    (file/create-file (str comp-root-dir "/resources/" interface "/.keep") [""])
    (shared/create-src-dirs! ws-path (str "components/" component "/src") [interface-dir])
    (shared/create-src-dirs! ws-path (str "components/" component "/test") [interface-dir])
    (file/create-file (str comp-root-dir "/project.clj") project-content)
    (file/create-file (str comp-root-dir "/readme.md") doc-content)
    (file/create-file (str comp-root-dir "/src/" interface-dir "/interface.clj") delegate-content)
    (file/create-file (str comp-root-dir "/src/" interface-dir "/core.clj") core-content)
    (file/create-file (str comp-root-dir "/test/" interface-dir "/core_test.clj") test-content)

    (when-not (file/file-exists (str ws-path "/interfaces/src/" interface-dir))
      (create-ifc/create-interface ws-path top-dir top-ns interface))

    (doseq [dev-dir dev-dirs]
      (if (create-dev-links? ws-path top-dir dev-dir interface)
        (create-dev-links! ws-path dev-dir component interface interface-dir component-dir)
        (println (str "FYI: the component " component " was created but not added to " dev-dir
                      " because it's interface " interface " was already "
                      "used by " (used-by-component ws-path top-dir dev-dir interface) "."))))))
