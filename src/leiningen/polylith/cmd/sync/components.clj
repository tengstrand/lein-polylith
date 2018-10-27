(ns leiningen.polylith.cmd.sync.components
  (:require [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.add :as add]
            [clojure.string :as str]))

(defn ifc-components [ws-path top-dir all-interfaces all-components interface]
  (filterv #(= interface (shared/interface-of ws-path top-dir % all-interfaces)) all-components))

(defn add-component [ws-path top-dir system component]
  (add/add-component-to-system ws-path top-dir component system)
  (println (str "Added component '" component "' to system '" system "'.")))

(defn missing-components [system components interface]
  (println (str "Missing component in system '" system "' for interface '" interface "'. Suggested components: " (str/join ", " (sort components)) ".")))

(defn add-missing-components-to-system! [ws-path top-dir system]
  (let [system-path (str ws-path "/systems/" system)
        src-path (str system-path "/src/" top-dir)
        entities (set (map #(shared/link->entity ws-path %) (file/directories src-path)))
        all-bases (shared/all-bases ws-path)
        all-components (shared/all-components ws-path)
        all-interfaces (shared/all-interfaces ws-path top-dir)
        ifc-deps (deps/interface-dependencies ws-path top-dir all-components all-bases)
        components (filter all-components entities)
        entity-interfaces (set (map str (flatten (map ifc-deps entities))))
        system-interfaces (set (map #(shared/interface-of ws-path top-dir %) components))
        missing-interfaces (set/difference entity-interfaces system-interfaces)
        missing-component-lists (mapv #(vector % (ifc-components ws-path top-dir all-interfaces all-components %)) missing-interfaces)]

    (doseq [[interface components] missing-component-lists]
      (let [cnt (count components)]
        (cond
          (= cnt 0) nil
          (= cnt 1) (add-component ws-path top-dir system (first components))
          (> cnt 1) (missing-components system components interface))))

    (empty? (filter #(> (-> % second count) 1)
                    missing-component-lists))))

(defn add-missing-components-to-systems! [ws-path top-dir]
  (every? true? (map #(add-missing-components-to-system! ws-path top-dir %)
                     (shared/all-systems ws-path))))
