(ns leiningen.polylith.validate
  (:require [leiningen.polylith.core :as core]))

(defn delete [root-dir cmd name]
  (cond
    (not (= "c" cmd)) [false "Illegal first argument."]
    (nil? name) [false "Missing name."])
  :else [true])

(defn new-cmd [root-dir cmd name]
  (let [{:keys [changed-components]} (core/info root-dir)]
    (cond
      (not (= "c" cmd)) [false "Illegal first argument."]
      (nil? name) [false "Missing name."]
        (contains? changed-components name) [false "Component already exists."])
    :else [true]))
