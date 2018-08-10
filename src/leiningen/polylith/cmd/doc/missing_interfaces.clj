(ns leiningen.polylith.cmd.doc.missing-interfaces
  (:require [clojure.set :as set]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.deps :as cdeps]))

(defn deps->names [[_ symbols]]
  (mapv name symbols))

(defn missing->interface [interface]
  {"name" interface
   "type" "interface"})

(defn missing-interfaces [ws-path top-dir used-entities]
  (let [used-components (set/intersection used-entities (shared/all-components ws-path))
        used-interfaces (set (map #(shared/interface-of ws-path top-dir %) used-components))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))
        referenced-interfaces (set (mapcat deps->names (cdeps/interface-dependencies ws-path top-dir used-components used-bases)))
        missing-ifss (set/difference referenced-interfaces used-interfaces)]
    (mapv missing->interface missing-ifss)))
