(ns leiningen.polylith.cmd.doc.env-belonging
  (:require [leiningen.polylith.cmd.shared :as shared]))

(defn has-entity? [ws-path top-dir [type-dir environment entity]]
  (contains?
    (set (shared/used-entities ws-path top-dir type-dir environment))
    entity))

(defn ->entity-map [result [type environment entity]]
  (if (contains? result entity)
    (assoc result entity (conj (result entity) [type environment]))
    (assoc result entity [[type environment]])))

(defn entity->environment [ws-path top-dir]
  (let [components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)
        environments (shared/all-environments ws-path)
        systems (shared/all-systems ws-path)
        entities (concat components bases)
        env-entities (for [i ["environments"] j environments k entities] (vector i j k))
        sys-entities (for [i ["systems"] j systems k entities] (vector i j k))
        env-sys-entities (filterv #(has-entity? ws-path top-dir %) (concat env-entities sys-entities))]
    (reduce ->entity-map {} env-sys-entities)))
