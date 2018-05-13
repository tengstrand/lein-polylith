(ns leiningen.polylith.cmd.remove-from
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn remove-component [ws-path top-dir system component]
  (let [system-dir (str ws-path "/systems/" system)
        resource (str system-dir "/resources/" component)
        source (str system-dir "/sources/src/" (shared/full-name top-dir "/" (shared/src-dir-name component)))]
    (file/delete-file! resource)
    (file/delete-file! source)))

(defn validate [ws-path type from entity]
  (let [systems (shared/all-systems ws-path)
        components (shared/all-components ws-path)]
    (cond (not (contains? #{"s" "system"} type)) [false "You can only remove a component from a system"])
    (cond (not (contains? systems from)) [false (str "'" from "' is not an existing system")])
    (cond (not (contains? components entity)) [false (str "'" entity "' is not an existing component")])
    :else [true]))

(defn execute [ws-path top-dir [system component]]
  (let [[ok? message] (validate ws-path type system component)]
    (if ok?
      (remove-component ws-path top-dir system component)
      (println message))))
