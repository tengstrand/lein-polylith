(ns leiningen.polylith.cmd.create
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]
            [leiningen.polylith.version :as v]))

(defn validate-workspace [name ws-ns]
  (cond
    (utils/is-empty-str? name) [false "Missing name."]
    (utils/is-empty-str? ws-ns) [false "Missing root namespace."]
    :else [true]))

(defn validate-component [ws-path top-dir top-ns name]
  (let [{:keys [changed-components]} (info/info ws-path)]
    (cond
      (utils/is-empty-str? name) [false "Missing name."]
      (utils/is-empty-str? top-dir) [false "Missing top-dir."]
      (utils/is-empty-str? top-ns) [false "Missing top-ns."]
      (contains? changed-components name) [false "Component already exists."]
      :else [true])))

(defn validate [ws-path top-dir top-ns cmd name ws-ns]
  (let [{:keys [changed-components]} (info/info ws-path)]
    (condp = cmd
      "c" (validate-component ws-path top-dir top-ns name)
      "w" (validate-workspace name ws-ns)
      [false (str "Illegal first argument '" cmd "'")])))

(defn create-dev-links [ws-path dev-dir name top-name]
  (let [dir (str ws-path "/" dev-dir)
        levels (count (str/split dev-dir #"/"))
        src-levels (+ levels (count (str/split top-name #"/")))
        parent-path (str/join (repeat (inc levels) "../"))
        parent-src-path (str/join (repeat src-levels "../"))
        path (str parent-path "components/" name)
        src-path (str parent-src-path "components/" name)]
    (file/create-symlink (str dir "/resources/" name)
                         (str path "/resources/" name))
    (file/create-symlink (str dir "/project-files/" name "-project.clj")
                         (str path "/project.clj"))
    (file/create-symlink (str dir "/src/" top-name)
                         (str src-path "/src/" top-name))
    (file/create-symlink (str dir "/test/" top-name)
                         (str src-path "/test/" top-name))
    (file/create-symlink (str dir "/test-int/" top-name)
                         (str src-path "/test-int/" top-name))))

(defn create-src-dirs! [ws-path top-dir src-dir]
  (file/create-dir (str ws-path "/" src-dir))
  (let [dirs (str/split top-dir #"/")
        new-dirs (map #(str ws-path "/" src-dir "/" (str/join "/" (take % dirs)))
                      (range 1 (-> dirs count inc)))]
    (if (not (zero? (count dirs)))
      (doseq [dir new-dirs]
        (file/create-dir dir)))))

(defn create-workspace [path name ws-ns top-dir]
  (let [ws-path (str path "/" name)
        api-content [(str "(defproject " ws-ns "/apis \"1.0\"")
                     "  :description \"Component apis\""
                     "  :dependencies [[org.clojure/clojure \"1.9.0\"]]"
                     "  :aot :all)"]
        dev-content [(str "(defproject " ws-ns "/development \"1.0\"")
                     "  :description \"The development environment\""
                     (str "  :plugins [[polylith/lein-polylith \"" v/version "\"]]")
                     (str "  :polylith {:top-ns \"" ws-ns "\"")
                     (str "             :top-dir \"" top-dir "\"")
                     (str "             :development-dirs [\"development\"]")
                     (str "             :ignore-tests []}")
                     (str "  :profiles {:dev {:test-paths [\"test\" \"test-int\"]}}")
                     "  :dependencies [[org.clojure/clojure \"1.9.0\"]])"]]
    (file/create-dir ws-path)
    (file/create-dir (str ws-path "/apis"))
    (file/create-dir (str ws-path "/builds"))
    (file/create-dir (str ws-path "/components"))
    (file/create-dir (str ws-path "/development"))
    (file/create-dir (str ws-path "/development/project-files"))
    (file/create-dir (str ws-path "/development/resources"))
    (create-src-dirs! ws-path top-dir "/apis/src")
    (create-src-dirs! ws-path top-dir "/development/src")
    (create-src-dirs! ws-path top-dir "/development/test")
    (create-src-dirs! ws-path top-dir "/development/test-int")
    (file/create-dir (str ws-path "/systems"))
    (file/create-file (str ws-path "/apis/project.clj") api-content)
    (file/create-file (str ws-path "/development/project.clj") dev-content)
    (file/create-symlink (str ws-path "/development/src-apis") "../apis/src")))

(defn create-component [ws-path top-dir top-ns dev-dirs name]
  (let [comp-dir (str ws-path "/components/" name)
        ns-name (if (zero? (count top-ns)) name (str top-ns "." name))
        top-name (if (zero? (count top-dir)) name (str top-dir "/" name))
        api-content [(str "(ns " ns-name ".api)")
                     ""
                     ";; add your functions here..."
                     "(defn myfn [x])"]
        delegate-content [(str "(ns " ns-name ".api")
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
        test-content [(str "(ns " ns-name ".core-test")
                      "  (:require [clojure.test :refer :all]))"
                      (str "            [" ns-name ".core :as core]")
                      ""
                      ";; add your tests here..."
                      "(deftest test-myfn"
                      "  (is (= 42 (core/myfn 40))))"]
        test-int-content [(str "(ns " ns-name ".core-test")
                          "  (:require [clojure.test :refer :all]))"
                          (str "            [" ns-name ".core :as core]")
                          ""
                          ";; add your integration tests here"]
        project-content [(str "(defproject " top-ns "/" name " \"0.1\"")
                         (str "  :description \"A " name " component\"")
                         (str "  :dependencies [[" top-ns "/apis \"1.0\"]")
                         (str "                 [org.clojure/clojure \"1.9.0\"]]")
                         (str "  :aot :all)")]]
    (file/create-dir comp-dir)
    (file/create-dir (str comp-dir "/resources"))
    (file/create-dir (str comp-dir "/resources/" name))
    (create-src-dirs! ws-path top-name "apis/src")
    (create-src-dirs! ws-path top-name (str "components/" name "/src"))
    (create-src-dirs! ws-path top-name (str "components/" name "/test"))
    (create-src-dirs! ws-path top-name (str "components/" name "/test-int"))
    (file/create-file (str comp-dir "/project.clj") project-content)
    (file/create-file (str ws-path "/apis/src/" top-name "/api.clj") api-content)
    (file/create-file (str comp-dir "/src/" top-name "/api.clj") delegate-content)
    (file/create-file (str comp-dir "/src/" top-name "/core.clj") core-content)
    (file/create-file (str comp-dir "/test/" top-name "/core_test.clj") test-content)
    (file/create-file (str comp-dir "/test-int/" top-name "/core_test.clj") test-int-content)
    (doseq [dev-dir dev-dirs]
      (create-dev-links ws-path dev-dir name top-name))))

(defn ->dir [ws-ns top-dir]
  (or top-dir
      (str/replace ws-ns #"\." "/")))

(defn execute [ws-path top-dir top-ns dev-dirs [cmd name ws-ns ws-top-dir]]
  (let [[ok? msg] (validate ws-path top-dir top-ns cmd name ws-ns)]
    (if ok?
      (condp = cmd
        "c" (create-component ws-path top-dir top-ns dev-dirs name)
        "w" (create-workspace (file/current-path) name ws-ns (->dir ws-ns ws-top-dir)))
      (do
        (println msg)
        (help/create)))))
