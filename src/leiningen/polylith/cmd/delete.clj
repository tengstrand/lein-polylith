(ns leiningen.polylith.cmd.delete
  (:require [leiningen.polylith.cmd.delete.component :as delete-component]
            [leiningen.polylith.cmd.delete.base :as delete-base]
            [leiningen.polylith.cmd.delete.system :as delete-system]
            [leiningen.polylith.cmd.shared :as shared]))

(defn validate [ws-path type entity entity2]
  (let [components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)
        systems (shared/all-systems ws-path)]
    (cond
      (not (or (shared/component? type)
               (shared/base? type)
               (shared/system? type))) [false "Illegal first argument."]
      (and (shared/component? type)
           (not (contains? components entity))) [false (str "Component '" entity "' does not exist.")]
      (and (shared/base? type)
           (not (contains? bases entity))) [false (str "Base '" entity "' does not exist.")]
      (and (shared/system? type)
           (not (contains? systems entity))) [false (str "System '" entity "' does not exist.")]
      (and (shared/system? type)
           (-> entity2 nil? not)
           (not (contains? bases entity2))) [false (str "Base '" entity2 "' does not exist.")]
      :else [true])))

(defn execute [ws-path top-dir [type entity entity2]]
  (let [[ok? message] (validate ws-path type entity entity2)]
    (if ok?
      (do
        (when (shared/component? type)
          (delete-component/delete ws-path top-dir entity))
        (when (shared/base? type)
          (delete-base/delete ws-path top-dir entity))
        (when (shared/system? type)
          (delete-system/delete ws-path top-dir entity entity2)))
      (println message))))
