(ns leiningen.polylith.cmd.create
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]
            [leiningen.polylith.cmd.create.component :as component]
            [leiningen.polylith.cmd.create.system :as system]
            [leiningen.polylith.cmd.create.workspace :as workspace]))

(defn validate-component [ws-path name]
  (let [bases (info/all-bases ws-path)
        components (info/all-components ws-path)]
    (cond
      (utils/is-empty-str? name) [false "Missing name."]
      (contains? bases name) [false (str "A base with the same name '" name "' already exists.")]
      (contains? components name) [false (str "Component '" name "' already exists.")]
      :else [true])))

(defn validate-system [ws-path name]
  (let [components (info/all-components ws-path)
        systems (info/all-systems ws-path)]
    (cond
      (utils/is-empty-str? name) [false "Missing name."]
      (contains? components complement) [false (str "A component with the same name '" name "' already exists.")]
      (contains? systems name) [false (str "System '" name "' already exists.")]
      :else [true])))

(defn validate-workspace [name ws-ns]
  (let [dir (str (file/current-path) "/" name)]
    (cond
      (file/file-exists dir) [false (str "Workspace '" name "' already exists.")]
      (utils/is-empty-str? name) [false "Missing name."]
      (nil? ws-ns) [false "Missing namespace name."]
      :else [true])))

(defn validate [ws-path cmd name arg2]
  (condp = cmd
    "c" (validate-component ws-path name)
    "component" (validate-component ws-path name)
    "s" (validate-system ws-path name)
    "system" (validate-system ws-path name)
    "w" (validate-workspace name arg2)
    "workspace" (validate-workspace name arg2)
    [false (str "Illegal first argument '" cmd "'")]))

(defn ->dir [ws-ns top-dir]
  (or top-dir
      (str/replace ws-ns #"\." "/")))

(defn execute [ws-path top-dir top-ns clojure-version clojure-spec-version [cmd name arg2 arg3]]
  (let [[ok? msg] (validate ws-path cmd name arg2)]
    (if ok?
      (condp = cmd
        "c" (component/create ws-path top-dir top-ns clojure-version clojure-spec-version name arg2)
        "component" (component/create ws-path top-dir top-ns clojure-version clojure-spec-version name arg2)
        "s" (system/create ws-path top-dir top-ns clojure-version clojure-spec-version name arg2)
        "system" (system/create ws-path top-dir top-ns clojure-version clojure-spec-version name arg2)
        "w" (workspace/create (file/current-path) name arg2 (->dir arg2 arg3) clojure-version clojure-spec-version)
        "workspace" (workspace/create (file/current-path) name arg2 (->dir arg2 arg3) clojure-version clojure-spec-version))
      (println msg))))
