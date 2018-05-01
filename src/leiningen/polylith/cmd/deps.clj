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

(defn ns->entity [nspace ns-levels]
  (nth (str/split (namespace nspace) #"\.") ns-levels))

(defn fn-deps [ws-path top-dir entity-type entity interface-ns->interface]
  (let [dir (str ws-path "/" entity-type "/" entity "/src/" (shared/full-name top-dir "/" entity))
        files (file/files dir)]
    (mapcat #(function-deps % interface-ns->interface) files)))

(defn ns-levels [top-dir]
  (if (= "" top-dir)
    0
    (count (str/split top-dir #"/"))))

(defn interface-deps [fn-dependencies entity levels]
  (set (map #(ns->entity % levels)
            (fn-dependencies entity))))

(defn component-deps [fn-dependencies entity levels ifc->component]
  (set (map ifc->component (interface-deps fn-dependencies entity levels))))

(defn function-dependencies [ws-path top-dir]
  (let [components (set (shared/all-components ws-path))
        bases (shared/all-bases ws-path)
        dir (if (= "" top-dir) "" (str "/" top-dir))
        interfaces-dir (str ws-path "/interfaces/src" dir)
        interface-ns->interface (interface-ns->interface-map interfaces-dir)
        component-fn-deps (map #(vector % (fn-deps ws-path top-dir "components" % interface-ns->interface)) components)
        base-fn-deps (map #(vector % (fn-deps ws-path top-dir "bases" % interface-ns->interface)) bases)]
    (into (sorted-map) (concat component-fn-deps base-fn-deps))))

(defn print-component-dependencies [dependencies ns-levels interface->components]
  (doseq [entity (keys dependencies)]
    (println (str entity ":"))
    (let [interfaces (interface-deps dependencies entity ns-levels)
          components (sort (flatten (map interface->components interfaces)))]
      (doseq [component components]
        (println " " component)))))

(defn print-interface-dependencies [dependencies ns-levels]
  (doseq [entity (keys dependencies)]
    (println (str entity ":"))
    (let [interfaces (sort (interface-deps dependencies entity ns-levels))]
      (doseq [interface interfaces]
        (println " " interface)))))

(defn print-function-dependencies [dependencies]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (doseq [nspace (dependencies component)]
      (println " " nspace))))

(defn execute [ws-path top-dir [cmd]]
  (let [fn-dependencies (function-dependencies ws-path top-dir)
        levels (ns-levels top-dir)
        interface->components (shared/interface->components ws-path top-dir)]
      (condp = cmd
        "f" (print-function-dependencies fn-dependencies)
        "i" (print-interface-dependencies fn-dependencies levels)
        "c" (print-component-dependencies fn-dependencies levels interface->components)
        (print-component-dependencies fn-dependencies levels interface->components))))
