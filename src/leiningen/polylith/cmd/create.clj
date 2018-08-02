(ns leiningen.polylith.cmd.create
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.create.component :as component]
            [leiningen.polylith.cmd.create.system :as system]
            [leiningen.polylith.cmd.create.workspace :as workspace]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]))

(defn validate-component [ws-path component]
  (let [components (shared/all-components ws-path)
        bases      (shared/all-bases ws-path)]
    (cond
      (utils/is-empty-str? component) [false "Missing name."]
      (contains? components component) [false (str "Component '" component "' already exists.")]
      (contains? bases component) [false (str "A component can't use the name of an existing base (" component ").")]
      :else [true])))

(defn validate-system [ws-path top-dir name base]
  (let [interfaces   (shared/all-interfaces ws-path top-dir)
        components   (shared/all-components ws-path)
        systems      (shared/all-systems ws-path)
        environments (shared/all-environments ws-path)]
    (cond
      (utils/is-empty-str? name) [false "Missing name."]
      (contains? components base) [false (str "A base can't use the name of an existing component (" base ").")]
      (contains? interfaces base) [false (str "A base can't use the name of an existing interface (" base ").")]
      (contains? systems name) [false (str "System '" name "' already exists.")]
      (contains? environments name) [false (str "An environment with the name '" name "' already exists. Systems and environments are not allowed to have the same name.")]
      :else [true])))

(defn validate-workspace [name ws-ns]
  (let [dir (str (file/current-path) "/" name)]
    (cond
      (file/file-exists dir) [false (str "Workspace '" name "' already exists.")]
      (utils/is-empty-str? name) [false "Missing name."]
      (nil? ws-ns) [false "Missing namespace name."]
      :else [true])))

(defn validate [ws-path top-dir cmd name arg2]
  (cond
    (shared/component? cmd) (validate-component ws-path name)
    (shared/system? cmd) (validate-system ws-path top-dir name arg2)
    (shared/workspace? cmd) (validate-workspace name arg2)
    :else [false (str "Illegal first argument '" cmd "'")]))

(defn ->dir [ws-ns]
  (str/replace ws-ns #"\." "/"))

(defn execute [ws-path top-dir top-ns clojure-version args]
  (let [skip-git? (contains? (set args) "-git")
        [cmd name argument2] (filterv #(not= "-git" %) args)
        arg2      (if (= "-" argument2) "" argument2)
        [ok? msg] (validate ws-path top-dir cmd name arg2)]
    (if ok?
      (cond
        (shared/component? cmd) (component/create ws-path top-dir top-ns clojure-version name arg2)
        (shared/system? cmd) (system/create ws-path top-dir top-ns clojure-version name arg2)
        (shared/workspace? cmd) (workspace/create (file/current-path) name arg2 (->dir arg2) clojure-version skip-git?))
      (println msg))))
