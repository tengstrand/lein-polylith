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

(defn function-ref? [content alias->ns]
  (try
    (and (list? content)
         (-> content first sequential? not)
         (contains? alias->ns (some-> content first namespace symbol)))
    (catch Exception _ false)))

(defn symbol-ref? [content alias->ns]
  (try
    (and (symbol? content)
         (contains? alias->ns (symbol (namespace content))))
    (catch Exception _
      false)))

(defn function-deps
  ([file interface-ns->interface]
   (let [content (file/read-file (str file))
         alias->ns (into {} (imports content interface-ns->interface))]
     (flatten (function-deps alias->ns content []))))
  ([alias->ns content result]
   (if (sequential? content)
     (if (function-ref? content alias->ns)
       (conj result (symbol (str (alias->ns (symbol (-> content first namespace))))
                            (-> content first name)))
       (filterv (comp not nil?)
                (map #(function-deps alias->ns % result) content)))
     (when (symbol-ref? content alias->ns)
       (conj result (symbol (str (alias->ns (symbol (namespace content))))
                            (name content)))))))

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

(defn ns->entity [nspace levels]
  (nth (str/split (namespace nspace) #"\.") levels))

(defn fn-deps [ws-path top-dir entity-type entity entity-dir interface-ns->interface]
  (let [dir (str ws-path "/" entity-type "/" entity "/src/" (shared/full-dir-name top-dir entity-dir))
        files (filterv #(str/ends-with? % ".clj") (file/files dir))]
    (doall (mapcat #(function-deps % interface-ns->interface) files))))

(defn ns-levels [top-dir]
  (if (= "" top-dir)
    0
    (count (str/split top-dir #"/"))))

(defn interface-deps [fn-dependencies entity levels]
  (set (map #(ns->entity % levels)
            (fn-dependencies entity))))

(defn component-deps [fn-dependencies entity levels ifc->component]
  (set (filter #(not (= entity %))
         (map ifc->component (interface-deps fn-dependencies entity levels)))))

(defn dependencies [fn-dependencies levels ifc->component entities]
  (into {} (map #(vector % (set (component-deps fn-dependencies % levels ifc->component))) entities)))

(defn calc-deps [component comp-deps called-components call-chain]
  (if (contains? called-components component)
    call-chain
    (let [chains (filter identity
                   (mapv #(calc-deps %
                                     comp-deps
                                     (conj called-components component)
                                     (conj call-chain %))
                         (comp-deps component)))]
      (when (-> chains empty? not)
        (first chains)))))

(defn circular-comp-deps [component component-deps]
  (let [chains (calc-deps component component-deps #{} [component])]
    (when (-> chains empty? not)
      (str/join " > " chains))))

(defn unique-interface [ws-path top-dir component]
  (let [interface (shared/interface-of ws-path top-dir component)]
    (when (not= interface component)
      [component interface])))

(defn unique-interfaces [ws-path top-dir components]
  (filter second (map #(unique-interface ws-path top-dir %) components)))

(defn imported-components [content interface->component]
  (let [requires (ffirst (->imports (first content)))]
    (filterv identity
      (map #(-> % first interface->component)
            (filter #(= :as (second %)) requires)))))

(defn imported-comp-deps [file interface-ns->interface]
  (let [content (file/read-file (str file))]
    (imported-components content interface-ns->interface)))

(defn comp-deps [ws-path top-dir entity-type entity entity-dir interface-ns->interface]
  (let [dir (str ws-path "/" entity-type "/" entity "/src/" (shared/full-dir-name top-dir entity-dir))
        files (filterv #(str/ends-with? % ".clj") (file/files dir))]
    (vec (mapcat #(imported-comp-deps % interface-ns->interface) files))))

(defn ->deps [m [entity dependencies]]
  (if (contains? m entity)
    (assoc m entity (concat (m entity) dependencies))
    (assoc m entity dependencies)))

(defn ->comp-deps [[k v] interface->components]
  [k (sort (set (filterv #(not= k %) (mapcat #(interface->components (str %)) v))))])

(defn component-dependencies [ws-path top-dir]
  (let [components (set (shared/all-components ws-path))
        interface->components (shared/interface->components ws-path top-dir)
        bases (shared/all-bases ws-path)
        dir (if (= "" top-dir) "" (str "/" top-dir))
        interfaces-dir (str ws-path "/interfaces/src" dir)
        interface-ns->interface (interface-ns->interface-map interfaces-dir)
        ifc-component-deps (mapv (fn [[component interface]] (vector component
                                                                     (comp-deps ws-path top-dir "components" component interface interface-ns->interface)))
                                 (unique-interfaces ws-path top-dir components))
        component-deps (mapv #(vector % (comp-deps ws-path top-dir "components" % % interface-ns->interface)) components)
        base-component-deps (mapv #(vector % (comp-deps ws-path top-dir "bases" % % interface-ns->interface)) bases)
        interface-deps (reduce ->deps (sorted-map) (concat ifc-component-deps component-deps base-component-deps))]
    (into {} (map #(->comp-deps % interface->components) interface-deps))))

(defn function-dependencies [ws-path top-dir]
  (let [components (set (shared/all-components ws-path))
        bases (shared/all-bases ws-path)
        dir (if (= "" top-dir) "" (str "/" top-dir))
        interfaces-dir (str ws-path "/interfaces/src" dir)
        interface-ns->interface (interface-ns->interface-map interfaces-dir)
        interface-fn-deps (mapv (fn [[component interface]] (vector component
                                                                    (fn-deps ws-path top-dir "components" component interface interface-ns->interface)))
                                (unique-interfaces ws-path top-dir components))
        component-fn-deps (mapv #(vector % (fn-deps ws-path top-dir "components" % % interface-ns->interface)) components)
        base-fn-deps (mapv #(vector % (fn-deps ws-path top-dir "bases" % % interface-ns->interface)) bases)]
    (reduce ->deps (sorted-map) (concat interface-fn-deps component-fn-deps base-fn-deps))))

(defn print-component-dependencies [ws-path top-dir]
  (let [dependencies (component-dependencies ws-path top-dir)]
    (doseq [entity (keys dependencies)]
      (println (str entity ":"))
      (let [components (dependencies entity)]
        (doseq [component components]
          (println " " component))))))

(defn print-function-dependencies [ws-path top-dir]
  (let [dependencies (function-dependencies ws-path top-dir)]
    (doseq [component (keys dependencies)]
      (println (str component ":"))
      (doseq [nspace (dependencies component)]
        (println " " nspace)))))

(defn execute [ws-path top-dir [cmd]]
  (cond
    (shared/function? cmd) (print-function-dependencies ws-path top-dir)
    (shared/component? cmd) (print-component-dependencies ws-path top-dir)
    :else (print-component-dependencies ws-path top-dir)))
