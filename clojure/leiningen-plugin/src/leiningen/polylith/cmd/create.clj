(ns leiningen.polylith.cmd.create
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]
            [leiningen.polylith.version :as v]))

(defn validate-workspace [name ws-ns]
  (let [dir (str (file/current-path) "/" name)]
    (cond
      (file/file-exists dir) [false (str "Workspace '" name "' already exists.")]
      (utils/is-empty-str? name) [false "Missing name."]
      (nil? ws-ns) [false "Missing workspace namespace."]
      :else [true])))

(defn validate-component [ws-path top-dir top-ns name]
  (let [bases (info/all-bases ws-path)
        components (info/all-components ws-path)]
    (cond
      (utils/is-empty-str? name) [false "Missing name."]
      (nil?  top-dir) [false "Missing top-dir."]
      (nil?  top-ns) [false "Missing top-ns."]
      (contains? bases name) [false (str "Base '" name "' already exists.")]
      (contains? components name) [false (str "Component '" name "' already exists.")]
      :else [true])))

(defn validate [ws-path top-dir top-ns cmd name ws-ns]
  (condp = cmd
    "c" (validate-component ws-path top-dir top-ns name)
    "w" (validate-workspace name ws-ns)
    [false (str "Illegal first argument '" cmd "'")]))

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

(defn create-src-dirs! [ws-path top-dir src-dir]
  (file/create-dir (str ws-path "/" src-dir))
  (let [dirs (str/split top-dir #"/")
        new-dirs (map #(str ws-path "/" src-dir "/" (str/join "/" (take % dirs)))
                      (range 1 (-> dirs count inc)))]
    (if (not (zero? (count dirs)))
      (doseq [dir new-dirs]
        (file/create-dir dir)))))

(defn ->dependency [library lib-and-version]
  "lib-and-version can either be a single library version number
   or 'library (space) library-version'"
  (let [[lib ver] (str/split lib-and-version #" ")]
    (if ver
      (str "[" lib " \"" ver "\"]")
      (str "[" library " \"" lib "\"]"))))

(defn create-workspace [path name ws-ns top-dir clojure-version clojure-spec-version]
  (let [ws-path (str path "/" name)
        ws-name (if (str/blank? ws-ns) "" (str ws-ns "/"))
        interface-content [(str "(defproject " ws-name "interfaces \"1.0\"")
                           (str "  :description \"Component interfaces\"")
                           (str "  :dependencies [" (->dependency "org.clojure/clojure" clojure-version))
                           (str "                 " (->dependency "org.clojure/spec.alpha" clojure-spec-version) "]")
                           (str "  :aot :all)")]
        ws-content [(str "(defproject " ws-name "development \"1.0\"")
                    (str "  :description \"The workspace\"")
                    (str "  :plugins [[polylith/lein-polylith \"" v/version "\"]]")
                    (str "  :polylith {:vcs \"git\"")
                    (str "             :build-tool \"leiningen\"")
                    (str "             :top-ns \"" ws-ns "\"")
                    (str "             :top-dir \"" top-dir "\"")
                    (str "             :ignored-tests []")
                    (str "             :clojure-version \"1.9.0\"")
                    (str "             :clojure-spec-version \"org.clojure/spec.alpha 0.1.143\"")
                    (str "             :example-hash1 \"2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1\"")
                    (str "             :example-hash2 \"58cd8b3106c942f372a40616fe9155c9d2efd122\"})")]
        dev-content [(str "(defproject " ws-name "development \"1.0\"")
                     (str "  :description \"The main development environment\"")
                     (str "  :dependencies [" (->dependency "org.clojure/clojure" clojure-version))
                     (str "                 " (->dependency "org.clojure/spec.alpha" clojure-spec-version) "])")]]
    (file/create-dir ws-path)
    (file/create-dir (str ws-path "/interfaces"))
    (file/create-dir (str ws-path "/systems"))
    (file/create-dir (str ws-path "/components"))
    (file/create-dir (str ws-path "/environments"))
    (file/create-dir (str ws-path "/environments/development"))
    (file/create-dir (str ws-path "/environments/development/docs"))
    (file/create-dir (str ws-path "/environments/development/project-files"))
    (file/create-dir (str ws-path "/environments/development/project-files/bases"))
    (file/create-dir (str ws-path "/environments/development/project-files/components"))
    (file/create-dir (str ws-path "/environments/development/project-files/systems"))
    (file/create-dir (str ws-path "/environments/development/resources"))
    (create-src-dirs! ws-path top-dir "/interfaces/src")
    (create-src-dirs! ws-path top-dir "/environments/development/src")
    (create-src-dirs! ws-path top-dir "/environments/development/test")
    (file/create-dir (str ws-path "/bases"))
    (file/create-file (str ws-path "/interfaces/project.clj") interface-content)
    (file/create-file (str ws-path "/project.clj") ws-content)
    (file/create-file (str ws-path "/environments/development/project.clj") dev-content)
    (file/create-symlink (str ws-path "/environments/development/project-files/interfaces-project.clj") "../../../interfaces/project.clj")
    (file/create-symlink (str ws-path "/environments/development/project-files/workspace-project.clj") "../../../project.clj")
    (file/create-symlink (str ws-path "/environments/development/interfaces") "../../interfaces/src")))

(defn full-name [top separator name]
  (if (zero? (count top)) name (str top separator name)))

(defn create-component [ws-path top-dir top-ns clojure-version clojure-spec-version name]
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
                         (str "                 " (->dependency "org.clojure/clojure" clojure-version))
                         (str "                 " (->dependency "org.clojure/spec" clojure-spec-version) "]")
                         (str "  :aot :all)")]
        dev-dirs (file/directory-names (str ws-path "/environments"))]
    (file/create-dir comp-dir)
    (file/create-dir (str comp-dir "/resources"))
    (file/create-dir (str comp-dir "/resources/" name))
    (create-src-dirs! ws-path proj-dir "interfaces/src")
    (create-src-dirs! ws-path proj-dir (str "components/" name "/src"))
    (create-src-dirs! ws-path proj-dir (str "components/" name "/test"))
    (file/create-file (str comp-dir "/project.clj") project-content)
    (file/create-file (str ws-path "/interfaces/src/" proj-dir "/interface.clj") interface-content)
    (file/create-file (str comp-dir "/Readme.md") doc-content)
    (file/create-file (str comp-dir "/src/" proj-dir "/interface.clj") delegate-content)
    (file/create-file (str comp-dir "/src/" proj-dir "/core.clj") core-content)
    (file/create-file (str comp-dir "/test/" proj-dir "/core_test.clj") test-content)

    (doseq [dev-dir dev-dirs]
      (create-dev-links ws-path dev-dir name proj-dir))))

(defn ->dir [ws-ns top-dir]
  (or top-dir
      (str/replace ws-ns #"\." "/")))

(defn execute [ws-path top-dir top-ns clojure-version clojure-spec-version [cmd name ws-ns ws-top-dir]]
  (let [[ok? msg] (validate ws-path top-dir top-ns cmd name ws-ns)]
    (if ok?
      (condp = cmd
        "c" (create-component ws-path top-dir top-ns clojure-version clojure-spec-version name)
        "w" (create-workspace (file/current-path) name ws-ns (->dir ws-ns ws-top-dir) clojure-version clojure-spec-version))
      (do
        (println msg)))))
