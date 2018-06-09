(ns leiningen.polylith.cmd.delete
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.delete.component :as delete-component]))

(defn validate [ws-path cmd component]
  (let [components (shared/all-components ws-path)]
    (cond
      (not (contains? #{"c" "component"} cmd)) [false "Illegal first argument."]
      (not (contains? components component)) [false (str "Component '" component "' does not exist.")]
      :else [true])))

(defn execute [ws-path top-dir [cmd component]]
  (let [[ok? message] (validate ws-path cmd component)]
    (if ok?
      (delete-component/delete ws-path top-dir component)
      (println message))))
