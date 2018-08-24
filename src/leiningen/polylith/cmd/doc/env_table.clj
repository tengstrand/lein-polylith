(ns leiningen.polylith.cmd.doc.env-table
  (:require [leiningen.polylith.cmd.doc.table :as table]
            [clojure.set :as set]
            [leiningen.polylith.freemarker :as freemarker]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.doc.crop :as sys]
            [leiningen.polylith.cmd.doc.ifc-table :as ifc-table]))

(defn entity-deps [{:keys [entity _ children]} result]
  (concat (reduce concat (map #(entity-deps % result) children))
          (conj result entity)))

(defn unused->component [ws-path top-dir component]
  {"name" component
   "interface" (shared/interface-of ws-path top-dir component)
   "type" "component"})

(defn tables [ws-path top-dir all-bases type system-or-env ifc-entity-deps entity]
  (let [tree (sys/system-or-env-tree ws-path top-dir all-bases type system-or-env entity)
        used-entities (set (entity-deps tree []))
        usages (sys/entity-usages tree)
        medium-tree (sys/crop-branches 0 [999 0 tree usages {}])
        small-tree (sys/crop-branches 0 [1 0 medium-tree usages {}])
        added-entities (set (shared/used-entities ws-path top-dir type system-or-env))
        unused-entities (set/difference added-entities used-entities)
        expanded-table (vec (table/calc-table ws-path top-dir medium-tree))
        collapsed-table (vec (table/calc-table ws-path top-dir small-tree))
        interface-table (ifc-table/table ws-path top-dir entity ifc-entity-deps all-bases)
        unreferenced-components (if (contains? all-bases entity)
                                  (mapv #(unused->component ws-path top-dir %) unused-entities)
                                  [])]
    {"pureTable" (freemarker/->map interface-table)
     "expandedTable" (freemarker/->map expanded-table)
     "collapsedTable" (freemarker/->map collapsed-table)
     "unreferencedComponents" unreferenced-components}))

(defn system-and-env-tables [ws-path top-dir all-bases entity->env ifc-entity-deps entity]
  (mapv (fn [[type system-or-env]] (vector [type system-or-env]
                                           (tables ws-path top-dir all-bases type system-or-env ifc-entity-deps entity)))
        (entity->env entity)))
