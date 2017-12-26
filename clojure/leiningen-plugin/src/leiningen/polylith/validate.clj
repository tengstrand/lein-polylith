(ns leiningen.polylith.validate
  (:require [leiningen.polylith.core :as core]
            [clojure.string :as str]))

(defn delete [root-dir cmd name]
  (cond
    (not (= "c" cmd)) [false "Illegal first argument."]
    (nil? name) [false "Missing name."])
  :else [true])

(defn is-empty? [value]
  (or (nil? value) (str/blank? value)))

(defn create-workspace [name ws-ns]
    (cond
      (is-empty? name) [false "Missing name."]
      (is-empty? ws-ns) [false "Missing root namespace."]
      :else [true]))

(defn create-component [root-dir top-ns name]
  (let [{:keys [changed-components]} (core/info root-dir)]
    (cond
      (is-empty? name) [false "Missing name."]
      (is-empty? top-ns) [false "Missing top-ns."]
      (contains? changed-components name) [false "Component already exists."]
      :else [true])))

(defn create [root-dir top-ns cmd name ws-ns]
  (let [{:keys [changed-components]} (core/info root-dir)]
    (condp = cmd
      "c" (create-component root-dir top-ns name)
      "w" (create-workspace name ws-ns)
      [false (str "Illegal first argument '" cmd "'")])))
