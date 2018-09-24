(ns leiningen.polylith.cmd.sync.shared)

(defn index-of-lib [libs [lib]]
  (ffirst
    (filter #(= lib (-> % second first))
            (map-indexed vector libs))))

(defn deps-index [content]
  (ffirst
    (filter #(= :dependencies (second %))
            (map-indexed vector content))))

