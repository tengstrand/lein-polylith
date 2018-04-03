(ns leiningen.polylith.cmd.add
  (:require [leiningen.polylith.utils :as utils]
            [leiningen.polylith.cmd.info :as info]))

(defn validate [ws-path component system]
  (let [components (info/all-components ws-path)
        systems (info/all-systems ws-path)]
    (cond
      (utils/is-empty-str? component) [false "Missing component name"]
      (utils/is-empty-str? system) [false "Missing system name"]
      (not (contains? components component)) [false (str "Component '" component "' not found")]
      (not (contains? systems system)) [false (str "System '" system "' not found")]
      :else [true])))

(defn execute [ws-path top-dir [component system]]
  (let [[ok? message] (validate ws-path component system)]
    (if-not ok?
      (println message)
      (println "component=" component ", system=" system))))
