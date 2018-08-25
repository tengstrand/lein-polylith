(ns leiningen.polylith.cmd.doc.env-table
  (:require [leiningen.polylith.cmd.doc.table :as table]
            [clojure.set :as set]
            [leiningen.polylith.freemarker :as freemarker]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.doc.crop :as sys]
            [leiningen.polylith.cmd.doc.ifc-table :as ifc-table]
            [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.doc.env-belonging :as belonging]))

(defn entity-deps [{:keys [entity _ children]} result]
  (concat (reduce concat (map #(entity-deps % result) children))
          (conj result entity)))

(defn unused->component [ws-path top-dir component]
  {"name" component
   "interface" (shared/interface-of ws-path top-dir component)
   "type" "component"})

(defn drop-last-char [string]
  (subs string 0 (-> string count dec)))

(defn key-table [entity type name state table]
  {"info" {"id"  (str entity "/" (drop-last-char type) "/" name "/" state)
           "type" (drop-last-char type)
           "expanded" (= state "expanded")
           "name" name}
   "table" (freemarker/->map table)})

(defn entity-table [ws-path top-dir all-bases ifc-entity-deps type entity]
  (let [table (ifc-table/table ws-path top-dir entity ifc-entity-deps all-bases)]
    (key-table entity (str type "-") entity "" table)))

(defn env-tables [ws-path top-dir all-bases type system-or-env entity]
  (let [tree (sys/system-or-env-tree ws-path top-dir all-bases type system-or-env entity)
        used-entities (set (entity-deps tree []))
        usages (sys/entity-usages tree)
        medium-tree (sys/crop-branches 0 [999 0 tree usages {}])
        small-tree (sys/crop-branches 0 [1 0 medium-tree usages {}])
        added-entities (set (shared/used-entities ws-path top-dir type system-or-env))
        unused-entities (set/difference added-entities used-entities)
        expanded-table (vec (table/calc-table ws-path top-dir medium-tree))
        collapsed-table (vec (table/calc-table ws-path top-dir small-tree))]
        ;; todo: add these later.
        ;unreferenced-components (if (contains? all-bases entity)
        ;                          (mapv #(unused->component ws-path top-dir %) unused-entities)
        ;                          [])]
    [(key-table entity type system-or-env "expanded" expanded-table)
     (key-table entity type system-or-env "collapsed" collapsed-table)]))

(defn table-defs [ws-path top-dir all-bases entity->env ifc-entity-deps type entity]
  (let [table (entity-table ws-path top-dir all-bases ifc-entity-deps type entity)
        tables (mapcat (fn [[type system-or-env]] (env-tables ws-path top-dir all-bases type system-or-env entity))
                       (entity->env entity))]
    (conj tables table)))
