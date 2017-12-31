(ns leiningen.polylith.validate
  (:require [leiningen.polylith.core :as core]
            [clojure.string :as str]))

(defn is-empty? [value]
  (or (nil? value) (str/blank? value)))

(defn delete [ws-path top-dir top-ns cmd name]
  (cond
    (not (= "c" cmd)) [false "Illegal first argument."]
    (is-empty? top-dir) [false "Missing top-dir."]
    (is-empty? top-ns) [false "Missing top-ns."]
    (nil? name) [false "Missing name."])
  :else [true])

(defn create-workspace [name ws-ns]
    (cond
      (is-empty? name) [false "Missing name."]
      (is-empty? ws-ns) [false "Missing root namespace."]
      :else [true]))

(defn create-component [ws-path top-dir top-ns name]
  (let [{:keys [changed-components]} (core/info ws-path)]
    (cond
      (is-empty? name) [false "Missing name."]
      (is-empty? top-dir) [false "Missing top-dir."]
      (is-empty? top-ns) [false "Missing top-ns."]
      (contains? changed-components name) [false "Component already exists."]
      :else [true])))

(defn create [ws-path top-dir top-ns cmd name ws-ns]
  (let [{:keys [changed-components]} (core/info ws-path)]
    (condp = cmd
      "c" (create-component ws-path top-dir top-ns name)
      "w" (create-workspace name ws-ns)
      [false (str "Illegal first argument '" cmd "'")])))
