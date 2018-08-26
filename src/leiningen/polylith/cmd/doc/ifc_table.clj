(ns leiningen.polylith.cmd.doc.ifc-table
  (:require [leiningen.polylith.cmd.doc.shared-doc :as shared-doc]
            [leiningen.polylith.cmd.doc.table :as table]))

(defn ->child [interface]
  {:entity (str interface)
   :type "interface"
   :top true
   :children #{}})

(defn ->entity [entity dependencies all-bases]
  {:entity   entity
   :type     (shared-doc/entity-type entity all-bases)
   :top      false
   :children (mapv ->child dependencies)})

(defn table [ws-path top-dir entity entity-deps all-bases]
  (let [dependencies (sort (set (map str (entity-deps entity))))
        tree (->entity entity dependencies all-bases)]
    (vec (table/calc-table ws-path top-dir 2 tree))))
