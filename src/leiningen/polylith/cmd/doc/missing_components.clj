(ns leiningen.polylith.cmd.doc.missing-components
  (:require [clojure.set :as set]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.deps :as cdeps]))

(defn deps->names [[_ symbols]]
  (mapv name symbols))

(defn ->interface [interface]
  {:entity interface
   :type "interface"
   :top true
   :children []})

(defn interfaces-with-missing-components [ws-path top-dir used-entities]
  (let [used-components (set/intersection used-entities (shared/all-components ws-path))
        used-interfaces (set (map #(shared/interface-of ws-path top-dir %) used-components))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))
        referenced-interfaces (set (mapcat deps->names (cdeps/interface-dependencies ws-path top-dir used-components used-bases)))
        missing-ifss (set/difference referenced-interfaces used-interfaces)]
    (mapv ->interface missing-ifss)))
