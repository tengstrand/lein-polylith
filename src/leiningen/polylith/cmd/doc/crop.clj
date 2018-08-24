(ns leiningen.polylith.cmd.doc.crop
  (:require [leiningen.polylith.cmd.doc.shared-doc :as shared-doc]
            [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]))

(defn dependencies [ws-path top-dir type system-or-env]
  (let [used-entities (shared/used-entities ws-path top-dir type system-or-env)
        used-components (set/intersection used-entities (shared/all-components ws-path))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))]
    (cdeps/component-dependencies ws-path top-dir used-components used-bases)))

(defn dependency-tree [entity deps all-bases]
  {:entity entity
   :type (shared-doc/entity-type entity all-bases)
   :children (mapv #(dependency-tree % deps all-bases) (deps entity))})

(defn system-or-env-tree [ws-path top-dir all-bases type system-or-env entity]
  (let [deps (dependencies ws-path top-dir type system-or-env)]
    (dependency-tree entity deps all-bases)))

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

(defn crop-branches [x [maxy y {:keys [entity type top children]} usages result]]
  (if (= [y x] (usages entity))
    (assoc result :entity entity
                  :type type
                  :top top
                  :children (if (< y maxy)
                              (vec (map-indexed #(crop-branches (+ x %) [maxy (inc y) %2 usages result]) children))
                              []))
    (assoc result :entity entity
                  :type type
                  :top top
                  :children [])))
