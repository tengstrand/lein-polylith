(ns leiningen.polylith.cmd.add
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]))

(defn system-components [ws-path top-dir system]
  (let [dir (shared/full-name top-dir "/" "")
        components (shared/all-components ws-path)
        directories (file/directories (str ws-path "/systems/" system "/src/" dir))]
    (filterv #(contains? components %) (map #(shared/link->entity ws-path %) directories))))

(defn used-interface [ws-path top-dir system component]
  (let [interface (shared/interface-of ws-path top-dir component)
        components (system-components ws-path top-dir system)
        component-interfaces (map #(vector (shared/interface-of ws-path top-dir %) %) components)]
    (first (filter #(= interface (first %)) component-interfaces))))

(defn validate [ws-path top-dir system component]
  (let [components (shared/all-components ws-path)
        systems (shared/all-systems ws-path)
        [interface comp] (used-interface ws-path top-dir system component)]
    (cond
      (utils/is-empty-str? component) [false "Missing component name."]
      (utils/is-empty-str? system) [false "Missing system name."]
      (not (contains? components component)) [false (str "Component '" component "' not found.")]
      (not (contains? systems system)) [false (str "System '" system "' not found.")]
      interface [false (str "Component " component "'s interface " interface " is already used by " comp " and can't be added to " system ".")]
      :else [true])))

(defn add-component-to-system [ws-path top-dir component system]
  (let [system-dir (shared/full-dir-name top-dir system)
        relative-parent-path (shared/relative-parent-path system-dir)
        relative-component-path (str relative-parent-path "components/" component)
        system-path (str ws-path "/systems/" system)
        interface (shared/interface-of ws-path top-dir component)
        interface-dir (shared/full-dir-name top-dir interface)]
    (file/create-symlink (str system-path "/src/" interface-dir)
                         (str relative-component-path "/src/" interface-dir))
    (file/create-symlink (str system-path "/resources/" interface)
                         (str "../../../components/" component "/resources/" interface))))

(defn execute [ws-path top-dir [component system]]
  (let [[ok? message] (validate ws-path top-dir system component)]
    (if ok?
      (add-component-to-system ws-path top-dir component system)
      (println message))))
