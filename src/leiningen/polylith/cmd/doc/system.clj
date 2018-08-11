(ns leiningen.polylith.cmd.doc.system
  (:require [leiningen.polylith.cmd.doc.shared-doc :as shared-doc]
            [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]))

(defn dependencies [ws-path top-dir system]
  (let [used-entities (shared/used-entities ws-path top-dir system)
        used-components (set/intersection used-entities (shared/all-components ws-path))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))]
    (cdeps/component-dependencies ws-path top-dir used-components used-bases)))

(defn dependency-tree [entity deps all-bases]
  {:entity entity
   :type (shared-doc/entity-type entity all-bases)
   :children (mapv #(dependency-tree % deps all-bases) (deps entity))})

(defn system-tree [ws-path top-dir all-bases system base]
  (let [deps (dependencies ws-path top-dir system)]
    (dependency-tree base deps all-bases)))

(defn entity-usages
  ([tree]
   (into {} (map (juxt first #(-> % second first second))
                 (group-by first (sort (entity-usages 0 [0 [] tree]))))))
  ([x [y result {:keys [entity children]}]]
   (conj (apply concat
                (map-indexed #(entity-usages % [(inc y)
                                                result
                                                %2])
                             children))
         (conj [entity [y x]]))))

(defn crop-branches [x [maxy y {:keys [entity type children]} usages result]]
  (if (= [y x] (usages entity))
    (assoc result :entity entity
                  :type type
                  :children (if (< y maxy)
                              (vec (map-indexed #(crop-branches % [maxy (inc y) %2 usages result]) children))
                              []))
    (assoc result :entity entity
                  :type type
                  :children [])))

(defn cropped-tree
  ([ws-path top-dir all-bases system base]
   (cropped-tree ws-path top-dir all-bases system base 999))
  ([ws-path top-dir all-bases system base maxy]
   (let [tree (system-tree ws-path top-dir all-bases system base)
         usages (entity-usages tree)]
     (crop-branches 0 [maxy 0 tree usages {}]))))
