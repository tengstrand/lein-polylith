(ns leiningen.polylith.cmd.doc.shared-doc)

(defn entity-type [entity all-bases]
  (cond
    (contains? all-bases entity) "base"
    :else "component"))
