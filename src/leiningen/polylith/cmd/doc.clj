(ns leiningen.polylith.cmd.doc
  (:require [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]
            [selmer.parser :as selmer]
            [leiningen.polylith.file :as file]
            [clojure.java.browse :as browse]))

(defn dependencies [ws-path top-dir system-or-environment]
  (let [used-entities (shared/used-entities ws-path top-dir system-or-environment)
        used-components (set/intersection used-entities (shared/all-components ws-path))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))]
    (cdeps/component-dependencies ws-path top-dir used-components used-bases)))

(defn entity-type [entity all-bases]
  (if (contains? all-bases entity)
    "base"
    "component"))

(defn dependency-tree [entity deps all-bases]
  {:entity entity
   :type (entity-type entity all-bases)
   :children (mapv #(dependency-tree % deps all-bases) (deps entity))})

(defn count-cols [{:keys [_ _ children]}]
  (cond
    (empty? children) 1
    :else (apply + (map count-cols children))))

(defn count-columns [tree]
  (let [sections (count-cols tree)]
    (if (zero? sections)
      0
      (dec (* 2 sections)))))

(defn max-deps [{:keys [_ _ children]} depth]
  (if (empty? children)
    depth
    (apply max (map #(max-deps % (inc depth)) children))))

(defn calc-table
  ([tree]
   (let [maxy (max-deps tree 1)
         result (transient (vec (repeat maxy [])))
         _ (calc-table tree 0 maxy result)
         table (reverse (persistent! result))]
     (map #(interpose {:type "spc"} %) table)))
  ([{:keys [entity type children] :as tree} y maxy result]
   (assoc! result y (conj (get result y) {:entity entity
                                          :type type
                                          :columns (count-columns tree)}))
   (if (empty? children)
     (doseq [yy (range (inc y) maxy)]
       (assoc! result yy (conj (get result yy) {:entity ""
                                                :type "component"
                                                :columns 1})))
     (doseq [child children]
       (calc-table child (inc y) maxy result)))))

(defn calc-system-table [ws-path top-dir system-or-env]
  (let [deps (dependencies ws-path top-dir system-or-env)
        all-bases (shared/all-bases ws-path)
        base (first (set/intersection (set (keys deps)) all-bases))
        tree (dependency-tree base deps all-bases)]
    (calc-table tree)))

(defn execute [ws-path top-dir doc-dir template-dir [template-file]]
  (let [template-filename (or template-file "workspace.html")
        _ (selmer/set-resource-path! template-dir)
        table {:table (calc-system-table ws-path top-dir "development")
               :title "development"}
        content (selmer/render-file template-filename table)
        path (str doc-dir "/development.html")]
    (file/create-file-with-content path content)
    (browse/browse-url (file/url path))))
