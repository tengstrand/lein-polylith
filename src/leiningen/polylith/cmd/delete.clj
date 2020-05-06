(ns leiningen.polylith.cmd.delete
  (:require [leiningen.polylith.cmd.delete.component :as delete-component]
            [leiningen.polylith.cmd.delete.system :as delete-system]
            [leiningen.polylith.cmd.shared :as shared]))

(defn validate [ws-path type entity]
  (let [components (shared/all-components ws-path)
        systems (shared/all-systems ws-path)]
    (cond
      (not (or (shared/component? type)
               (shared/system? type))) [false "Illegal first argument."]
      (and (shared/component? type)
           (not (contains? components entity))) [false (str "Component '" entity "' does not exist.")]
      (and (shared/system? type)
           (not (contains? systems entity))) [false (str "System '" entity "' does not exist.")]
      :else [true])))

(defn execute [ws-path top-dir [type entity]]
  (let [[ok? message] (validate ws-path type entity)]
    (if ok?
      (if (shared/component? type)
        (delete-component/delete ws-path top-dir entity))
        ;(delete-system/delete ws-path top-dir system base))
      (println message))))
