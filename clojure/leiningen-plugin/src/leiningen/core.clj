(ns polylith.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [polylith.file :as file]))

(defn str->component [name]
  (symbol (str/replace name #"_" "-")))

(defn ns-components [component-paths]
  (let [component (-> component-paths ffirst str->component)
        path->ns (fn [path] (-> path file/read-file first second))
        namespaces (map #(-> % second path->ns) component-paths)]
    (map #(vector % component) namespaces)))

(defn api-ns->component []
  (into {}
    (reduce into []
      (map ns-components
           (partition-by first (file/paths-in-dir "proxy"))))))

(defn- ->imports
  ([imports]
   (->imports imports []))
  ([imports result]
   (when (sequential? imports)
     (if (= :require (first imports))
       (conj result (rest imports))
       (filter (comp not nil?)
               (map ->imports imports))))))

(defn imports [content api->component]
  (let [requires (ffirst (->imports (first content)))
        ns-imports (map (juxt last first)
                        (filter #(= :as (second %)) requires))]
    (filter #(api->component (second %)) ns-imports)))

(defn component? [content alias->ns]
  (and (list? content)
       (-> content first sequential? not)
       (contains? alias->ns (some-> content first namespace symbol))))

(defn file-dependencies
  ([filename api->component]
   (let [content (file/read-file filename)
         alias->ns (into {} (imports content api->component))
         functions (flatten (file-dependencies alias->ns content []))
         fn->ns (fn [function] (alias->ns (-> function namespace symbol)))]
     (set (map fn->ns functions))))
  ([alias->ns content result]
   (when (sequential? content)
     (if (component? content alias->ns)
       (conj result (first content))
       (filter (comp not nil?)
               (map #(file-dependencies alias->ns % result) content))))))

(defn component-dependencies [component-paths api->component]
  (let [component (ffirst component-paths)
        files (map second component-paths)
        dependencies (into #{} (mapcat #(file-dependencies % api->component) files))]
    [component dependencies]))

;(def content (file/read-file "src/api/core.clj"))
;
;(def api->component (api-ns->component))
;(def all-paths (partition-by first (file/paths-in-dir "src")))
;(def component-paths (first all-paths))
;(def component (ffirst component-paths))
;(def files (map second component-paths))

;(component-dependencies component-paths api->component)
;(map #(component-dependencies % api->component) all-paths)
