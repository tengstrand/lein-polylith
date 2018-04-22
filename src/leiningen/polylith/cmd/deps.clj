(ns leiningen.polylith.cmd.deps
  (:require [clojure.string :as str]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.shared :as shared]))

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

(defn interface-ns->component [dir components]
  (into {}
        (filterv #(contains? components (-> % second name))
          (reduce into []
                  (map ns-components
                       (partition-by first (file/paths-in-dir dir)))))))

(defn all-dependencies [ws-path top-dir]
  (let [dir (if (= "" top-dir) "" (str "/" top-dir))
        development-dir (str ws-path "/environments/development/src" dir)
        components (set (shared/all-components ws-path))
        interface->component (interface-ns->component (str ws-path "/interfaces/src" dir) components)
        all-paths (partition-by first (file/paths-in-dir development-dir))]
    (into (sorted-map) (map #(component-dependencies % interface->component) all-paths))))

(defn ns->component [nspace ns-levels]
  (nth (str/split (namespace nspace) #"\.") ns-levels))

(defn print-component-deps [dependencies ns-levels]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (let [interfaces (sort (set (map #(ns->component % ns-levels)
                                     (dependencies component))))]
      (doseq [interface interfaces]
        (println " " interface)))))

(defn print-interface-deps [dependencies]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (doseq [nspace (dependencies component)]
      (println " " nspace))))

(defn execute [ws-path top-dir [arg]]
  (let [dependencies (all-dependencies ws-path top-dir)
        ns-levels (if (= "" top-dir) 0 (count (str/split top-dir #"/")))]
    (if (= "f" arg)
      (print-interface-deps dependencies)
      (print-component-deps dependencies ns-levels))))
