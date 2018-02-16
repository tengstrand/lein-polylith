(ns leiningen.polylith.cmd.deps
  (:require [clojure.string :as str]
            [leiningen.polylith.file :as file]))

(defn replace-ns [function alias->ns]
  (let [fn-name (name function)
        fn-ns-name (name (alias->ns (-> function namespace symbol)))]
    (symbol fn-ns-name fn-name)))

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

(defn file-dependencies
  ([filename interface->component]
   (let [content (file/read-file filename)
         alias->ns (into {} (imports content interface->component))
         functions (flatten (file-dependencies alias->ns content []))]
     (set (map #(replace-ns % alias->ns) functions))))
  ([alias->ns content result]
   (when (sequential? content)
     (if (component? content alias->ns)
       (conj result (first content))
       (filter (comp not nil?)
               (map #(file-dependencies alias->ns % result) content))))))

(defn component-dependencies [component-paths interface->component]
  (let [component (-> component-paths ffirst symbol)
        files (map second component-paths)
        dependencies (sort (into #{} (mapcat #(file-dependencies % interface->component) files)))]
    [component (vec dependencies)]))

(defn str->component [name]
  (symbol (str/replace name #"_" "-")))

(defn ns-components [component-paths]
  (let [component (-> component-paths ffirst str->component)
        path->ns (fn [path] (-> path file/read-file first second))
        namespaces (map #(-> % second path->ns) component-paths)]
    (map #(vector % component) namespaces)))

(defn interface-ns->component [ws-path]
  (into {}
        (reduce into []
                (map ns-components
                     (partition-by first (file/paths-in-dir (str ws-path "/interfaces/src")))))))

(defn all-dependencies [ws-path]
  (let [development-dir (str ws-path "/environments/development/src")
        interface->component (interface-ns->component ws-path)
        all-paths (partition-by first (file/paths-in-dir development-dir))]
    (into (sorted-map) (map #(component-dependencies % interface->component) all-paths))))

(defn ns->component [nspace]
  (first (str/split (namespace nspace) #"\.")))

(defn print-component-deps [dependencies]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (let [interfaces (sort (set (map ns->component (dependencies component))))]
      (doseq [interface interfaces]
        (println " " interface)))))

(defn print-interface-deps [dependencies]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (doseq [nspace (dependencies component)]
      (println " " nspace))))

(defn execute [ws-path [arg]]
  (let [dependencies (all-dependencies ws-path)]
    (if (= "f" arg)
      (print-interface-deps dependencies)
      (print-component-deps dependencies))))
