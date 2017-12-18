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

(defn new-cmd [root-dir top-ns cmd name]
  (let [{:keys [changed-components]} (core/info root-dir)]
    (cond
      (not (= "c" cmd)) [false "Illegal first argument."]
      (is-empty? name) [false "Missing name."]
      (is-empty? top-ns) [false "Missing top-ns."]
      (contains? changed-components name) [false "Component already exists."])
    :else [true]))
