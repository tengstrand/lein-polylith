(ns leiningen.polylith.cmd.doc.ifc-table
  (:require [leiningen.polylith.cmd.doc.shared-doc :as shared-doc]
            [leiningen.polylith.cmd.doc.table :as table]))

(defn ->child [interface]
  {:entity (str interface)
   :type "interface"
   :top true
   :bottom false
   :children #{}})

(defn ->entity [entity dependencies all-bases]
  {:entity   entity
   :type     (shared-doc/entity-type entity all-bases)
   :top      false
   :bottom   true
   :children (mapv ->child dependencies)})

(defn entity-ifc-table [ws-path top-dir entity entity-deps all-bases]
  (let [dependencies (set (map str (entity-deps entity)))
        tree (->entity entity dependencies all-bases)
        table (vec (table/calc-table ws-path top-dir 2 tree))]
    table))
