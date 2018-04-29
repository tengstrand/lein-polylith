(ns leiningen.polylith.cmd.create
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.create.component :as component]
            [leiningen.polylith.cmd.create.system :as system]
            [leiningen.polylith.cmd.create.workspace :as workspace]))

(defn validate-component [ws-path top-dir component interface]
  (let [interfaces (shared/all-interfaces ws-path top-dir)
        components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)]
    (cond
      (utils/is-empty-str? component) [false "Missing name."]
      (contains? components component) [false (str "Component '" component "' already exists.")]
      (and (contains? components interface)
           (not (contains? interfaces interface))) [false (str "An interface can't use the name of an existing component (" interface ").")]
      (contains? interfaces component) [false (str "A component can't use the name of an existing interface (" component ").")]
      (contains? bases component) [false (str "A component can't use the name of an existing base (" component ").")]
      (contains? bases interface) [false (str "An interface can't use the name of an existing base (" interface ").")]
      :else [true])))

(defn validate-system [ws-path top-dir name base]
  (let [interfaces (shared/all-interfaces ws-path top-dir)
        components (shared/all-components ws-path)
        systems (shared/all-systems ws-path)]
    (cond
      (utils/is-empty-str? name) [false "Missing name."]
      (contains? components base) [false (str "A base can't use the name of an existing component (" base ").")]
      (contains? interfaces base) [false (str "A base can't use the name of an existing interface (" base ").")]
      (contains? systems name) [false (str "System '" name "' already exists.")]
      :else [true])))

(defn validate-workspace [name ws-ns]
  (let [dir (str (file/current-path) "/" name)]
    (cond
      (file/file-exists dir) [false (str "Workspace '" name "' already exists.")]
      (utils/is-empty-str? name) [false "Missing name."]
      (nil? ws-ns) [false "Missing namespace name."]
      :else [true])))

(defn validate [ws-path top-dir cmd name arg2]
  (condp = cmd
    "c" (validate-component ws-path top-dir name arg2)
    "component" (validate-component top-dir ws-path name arg2)
    "s" (validate-system ws-path top-dir name arg2)
    "system" (validate-system ws-path top-dir name arg2)
    "w" (validate-workspace name arg2)
    "workspace" (validate-workspace name arg2)
    [false (str "Illegal first argument '" cmd "'")]))

(defn ->dir [ws-ns]
  (str/replace ws-ns #"\." "/"))

(defn execute [ws-path top-dir top-ns clojure-version [cmd name argument2]]
  (let [arg2 (if (= "-" argument2) "" argument2)
        [ok? msg] (validate ws-path top-dir cmd name arg2)]
    (if ok?
      (condp = cmd
        "c" (component/create ws-path top-dir top-ns clojure-version name arg2)
        "component" (component/create ws-path top-dir top-ns clojure-version name arg2)
        "s" (system/create ws-path top-dir top-ns clojure-version name arg2)
        "system" (system/create ws-path top-dir top-ns clojure-version name arg2)
        "w" (workspace/create (file/current-path) name arg2 (->dir arg2) clojure-version)
        "workspace" (workspace/create (file/current-path) name arg2 (->dir arg2) clojure-version))
      (println msg))))
