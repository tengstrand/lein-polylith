(ns leiningen.polylith.cmd.deps
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [clojure.set :as set]))

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
  (let [requires   (ffirst (->imports (first content)))
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
   (let [content   (file/read-file (str file))
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
  (let [entity     (-> interface-paths ffirst str->entity)
        path->ns   (fn [path] (-> path file/read-file first second))
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
  (let [dir   (str ws-path "/" entity-type "/" entity "/src/" (shared/full-dir-name top-dir entity-dir))
        files (filterv #(str/ends-with? % ".clj") (file/files dir))]
    (doall (mapcat #(function-deps % interface-ns->interface) files))))

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

(defn as? [statement]
  (and (sequential? statement)
       (= :as (second statement))))

(defn imported-interfaces [content interface-ns->interface]
  (let [requires (ffirst (->imports (first content)))]
    (filterv identity
             (map #(-> % first interface-ns->interface)
                  (filter as? requires)))))

(defn imported-comp-deps [file interface-ns->interface]
  (let [content (file/read-file (str file))]
    (imported-interfaces content interface-ns->interface)))

(defn ifc-deps [ws-path top-dir entity-type entity interface-ns->interface]
  (let [dir (str ws-path "/" entity-type "/" entity "/src/" top-dir)
        files (filterv #(str/ends-with? % ".clj") (file/files dir))]
    (vec (mapcat #(imported-comp-deps % interface-ns->interface) files))))

(defn ->deps [m [entity dependencies]]
  (if (contains? m entity)
    (assoc m entity (concat (m entity) dependencies))
    (assoc m entity dependencies)))

(defn ->component-deps [[k v] interface->components]
  [k (sort (set (filterv #(not= k %) (mapcat #(interface->components (str %)) v))))])

(defn interface-dependencies [ws-path top-dir used-components used-bases]
  (let [dir (if (= "" top-dir) "" (str "/" top-dir))
        interfaces-dir (str ws-path "/interfaces/src" dir)
        interface-ns->interface (interface-ns->interface-map interfaces-dir)
        comp-ifc-deps (mapv #(vector % (ifc-deps ws-path top-dir "components" % interface-ns->interface)) used-components)
        base-ifc-deps (mapv #(vector % (ifc-deps ws-path top-dir "bases" % interface-ns->interface)) used-bases)]
    (reduce ->deps (sorted-map) (concat comp-ifc-deps base-ifc-deps))))

(defn component-dependencies
  ([ws-path top-dir]
   (component-dependencies ws-path top-dir (shared/all-components ws-path) (shared/all-bases ws-path)))
  ([ws-path top-dir used-components used-bases]
   (let [interface->components (shared/interface->components ws-path top-dir used-components)
         interface-deps (interface-dependencies ws-path top-dir used-components used-bases)]
     (into {} (map #(->component-deps % interface->components) interface-deps)))))

(defn function-dependencies [ws-path top-dir used-components used-bases]
  (let [dir                     (if (= "" top-dir) "" (str "/" top-dir))
        interfaces-dir          (str ws-path "/interfaces/src" dir)
        interface-ns->interface (interface-ns->interface-map interfaces-dir)
        interface-fn-deps       (mapv (fn [[component interface]] (vector component
                                                                          (fn-deps ws-path top-dir "components" component interface interface-ns->interface)))
                                      (unique-interfaces ws-path top-dir used-components))
        component-fn-deps       (mapv #(vector % (fn-deps ws-path top-dir "components" % % interface-ns->interface)) used-components)
        base-fn-deps            (mapv #(vector % (fn-deps ws-path top-dir "bases" % % interface-ns->interface)) used-bases)]
    (reduce ->deps (sorted-map) (concat interface-fn-deps component-fn-deps base-fn-deps))))

(defn print-interface-dependencies [ws-path top-dir used-components used-bases list-entities]
  (let [dependencies (interface-dependencies ws-path top-dir used-components used-bases)]
    (doseq [entity (sort (set/intersection list-entities (set (keys dependencies))))]
      (println (str entity ":"))
      (doseq [interface (sort (set (dependencies entity)))]
        (println " " interface)))))

(defn print-component-dependencies [ws-path top-dir used-components used-bases list-entities]
  (let [dependencies (component-dependencies ws-path top-dir used-components used-bases)]
    (doseq [entity (sort (set/intersection list-entities (set (keys dependencies))))]
      (println (str entity ":"))
      (let [components (sort (set (dependencies entity)))]
        (doseq [component components]
          (println " " component))))))

(defn print-function-dependencies [ws-path top-dir used-components used-bases list-entities]
  (let [dependencies (function-dependencies ws-path top-dir used-components used-bases)]
    (doseq [component (sort (set/intersection list-entities (set (keys dependencies))))]
      (println (str component ":"))
      (doseq [nspace (sort (set (dependencies component)))]
        (println " " nspace)))))

(defn system-or-environment? [ws-path entity]
  (contains? (set (concat (shared/all-environments ws-path)
                          (shared/all-systems ws-path)))
             entity))

(defn component-or-base? [ws-path entity]
  (contains? (set (concat (shared/all-components ws-path)
                          (shared/all-bases ws-path)))
             entity))

(defn validate [ws-path entity]
  (if (or (nil? entity)
          (system-or-environment? ws-path entity)
          (component-or-base? ws-path entity))
    [true]
    [false (str "Couldn't show dependencies for '" entity "'. It's not a system, environment, base or component.")]))

(defn execute [ws-path top-dir args]
  (let [flags #{"+f" "+function"
                "+c" "+component"}
        entity (first (set/difference (set args) flags))
        flag (first (set/intersection (set args) flags))
        used-entities (if (system-or-environment? ws-path entity)
                        (shared/used-entities ws-path top-dir entity)
                        (shared/used-entities ws-path top-dir))
        list-entities (if (system-or-environment? ws-path entity)
                        used-entities
                        (if entity
                          (set/intersection #{entity} used-entities)
                          used-entities))
        used-components (set/intersection used-entities (shared/all-components ws-path))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))
        [ok? message] (validate ws-path entity)]
    (cond
      (not ok?) (println message)
      (shared/+function? flag) (print-function-dependencies ws-path top-dir used-components used-bases list-entities)
      (shared/+component? flag) (print-component-dependencies ws-path top-dir used-components used-bases list-entities)
      :else (print-interface-dependencies ws-path top-dir used-components used-bases list-entities))))
