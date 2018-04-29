(ns leiningen.polylith.cmd.deps
  (:require [clojure.string :as str]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.shared :as shared]))

(defn- ->imports
  ([imports]
   (->imports imports []))
  ([imports result]
   (when (sequential? imports)
     (if (= :require (first imports))
       (conj result (rest imports))
       (filter (comp not nil?)
               (map ->imports imports))))))

(defn imports [content interface->component]
  (let [requires (ffirst (->imports (first content)))
        ns-imports (map (juxt last first)
                        (filter #(= :as (second %)) requires))]
    (filter #(interface->component (second %)) ns-imports)))

(defn component? [content alias->ns]
  (and (list? content)
       (-> content first sequential? not)
       (contains? alias->ns (some-> content first namespace symbol))))

(defn function-deps
  ([file interface-ns->interface]
   (let [content (file/read-file (str file))
         alias->ns (into {} (imports content interface-ns->interface))]
     (flatten (function-deps alias->ns content []))))
  ([alias->ns content result]
   (when (sequential? content)
     (if (component? content alias->ns)
       (conj result (symbol (str (alias->ns (symbol (-> content first namespace))))
                            (-> content first name)))
       (filter (comp not nil?)
               (map #(function-deps alias->ns % result) content))))))

(defn str->entity [name]
  (symbol (str/replace name #"_" "-")))

(defn ns-interfaces [interface-paths]
  (let [entity (-> interface-paths ffirst str->entity)
        path->ns (fn [path] (-> path file/read-file first second))
        namespaces (map #(-> % second path->ns) interface-paths)]
    (map #(vector % entity) namespaces)))

(defn interface-ns->interface-map [dir]
  (into {}
        (reduce into []
                (map ns-interfaces
                     (partition-by first (file/paths-in-dir dir))))))

(defn ns->component [nspace ns-levels]
  (nth (str/split (namespace nspace) #"\.") ns-levels))

(defn fn-deps [ws-path top-dir entity-type entity interface-ns->interface]
  (let [dir (shared/full-name top-dir "/" entity)
        files (file/files (str ws-path "/" entity-type "/" entity "/src/" dir))]
    (mapcat #(function-deps % interface-ns->interface) files)))

(defn function-dependencies [ws-path top-dir]
  (let [components (set (shared/all-components ws-path))
        bases (shared/all-bases ws-path)
        dir (if (= "" top-dir) "" (str "/" top-dir))
        interfaces-dir (str ws-path "/interfaces/src" dir)
        interface-ns->interface (interface-ns->interface-map interfaces-dir)
        component-fn-deps (map #(vector % (fn-deps ws-path top-dir "components" % interface-ns->interface)) components)
        base-fn-deps (map #(vector % (fn-deps ws-path top-dir "bases" % interface-ns->interface)) bases)]
    (into (sorted-map) (concat component-fn-deps base-fn-deps))))

(defn print-entity-dependencies [dependencies ns-levels]
  (doseq [entity (keys dependencies)]
    (println (str entity ":"))
    (let [interfaces (sort (set (map #(ns->component % ns-levels)
                                     (dependencies entity))))]
      (doseq [interface interfaces]
        (println " " interface)))))

(defn print-function-dependencies [dependencies]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (doseq [nspace (dependencies component)]
      (println " " nspace))))

(defn execute [ws-path top-dir [arg]]
  (let [dependencies (function-dependencies ws-path top-dir)
        ns-levels (if (= "" top-dir) 0 (count (str/split top-dir #"/")))]
    (condp = arg
      "f" (print-function-dependencies dependencies)
      "i" (print-entity-dependencies dependencies ns-levels)
      (print-entity-dependencies dependencies ns-levels))))
