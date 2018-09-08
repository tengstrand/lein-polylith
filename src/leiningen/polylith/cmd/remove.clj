(ns leiningen.polylith.cmd.remove
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn remove-component [ws-path top-dir component system]
  (let [system-dir (str ws-path "/systems/" system)
        resource (str system-dir "/resources/" component)
        interface (shared/interface-of ws-path top-dir component)
        component-dir (shared/full-dir-name top-dir component)
        interface-dir (shared/full-dir-name top-dir interface)
        src-component (str system-dir "/src/" component-dir)
        src-interface (str system-dir "/src/" interface-dir)]
    (file/delete-file! resource)
    (file/delete-file! src-component)
    (if (not= component interface)
      (file/delete-file! src-interface))))

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
