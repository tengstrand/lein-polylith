(ns leiningen.polylith.cmd.rename
  (:require [leiningen.polylith.cmd.rename.component :as component])
  (:require [leiningen.polylith.cmd.rename.interface :as interface]
            [leiningen.polylith.cmd.shared :as shared]))

(defn validate [ws-path top-dir cmd from to]
  (let [interfaces (shared/all-interfaces ws-path top-dir)
        components (shared/all-components ws-path)]
    (cond
      (not (shared/component? cmd)) [false "Invalid first argument. Type 'leiningen polylith help rename' for help."]
      (not (contains? components from)) [false (str "'" from "' is not an existing component.")]
      (contains? components to) [false (str "'" to "' already exists.")]
      (contains? interfaces to) [false (str "An interface with the name '" to "' already exists, which is not allowed.")]
      :else [true])))

(defn execute [ws-path top-dir [cmd from to]]
  (let [[ok? message] (validate ws-path top-dir cmd from to)]
    (if (not ok?)
      (println message)
      (cond
        (shared/component? cmd) (component/rename ws-path top-dir from to)))))
