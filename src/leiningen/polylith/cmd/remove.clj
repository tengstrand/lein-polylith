(ns leiningen.polylith.cmd.remove
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn remove-component [ws-path top-dir component system]
  (let [system-dir    (str ws-path "/systems/" system)
        resource      (str system-dir "/resources/" component)
        dir           (shared/full-dir-name top-dir component)
        src-component (str system-dir "/src/" dir)]
    (file/delete-file! resource)
    (file/delete-file! src-component)))

(defn validate [ws-path type component system]
  (let [systems    (shared/all-systems ws-path)
        components (shared/all-components ws-path)]
    (cond (not (shared/system? type)) [false "You can only remove a component from a system"])
    (cond (not (contains? systems system)) [false (str "'" system "' is not an existing system")])
    (cond (not (contains? components component)) [false (str "'" component "' is not an existing component")])
    :else [true]))

(defn execute [ws-path top-dir [component system]]
  (let [[ok? message] (validate ws-path type component system)]
    (if ok?
      (remove-component ws-path top-dir component system)
      (println message))))
