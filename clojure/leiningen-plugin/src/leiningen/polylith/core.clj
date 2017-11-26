(ns leiningen.polylith.core
  (:require [clojure.pprint :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]
            [clojure.java.shell :as shell]))

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
                     (partition-by first (file/paths-in-dir "apis"))))))

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

(defn replace-ns [function alias->ns]
  (let [fn-name (name function)
        fn-ns-name (name (alias->ns (-> function namespace symbol)))]
    (symbol fn-ns-name fn-name)))

(defn file-dependencies
  ([filename api->component]
   (let [content (file/read-file filename)
         alias->ns (into {} (imports content api->component))
         functions (flatten (file-dependencies alias->ns content []))]
     (set (map #(replace-ns % alias->ns) functions))))
  ([alias->ns content result]
   (when (sequential? content)
     (if (component? content alias->ns)
       (conj result (first content))
       (filter (comp not nil?)
               (map #(file-dependencies alias->ns % result) content))))))

(defn component-dependencies [component-paths api->component]
  (let [component (-> component-paths ffirst symbol)
        files (map second component-paths)
        dependencies (sort (into #{} (mapcat #(file-dependencies % api->component) files)))]
    [component (vec dependencies)]))

(defn all-dependencies []
  (let [api->component (api-ns->component)
        all-paths (partition-by first (file/paths-in-dir "src"))]
    (into (sorted-map) (map #(component-dependencies % api->component) all-paths))))

(defn create-dependency-file! [[component functions] file-separator]
  (let [path (str "dependencies" file-separator component ".edn")]
    (file/create-file path functions)))

(defn matching-dir? [path dir]
  (and
    (str/starts-with? path (str dir "/"))
    (not (= path (str dir "/parent.clj")))))

(defn git-changes [root-dir dir last-success-sha1 current-sha1]
  (let [diff (:out (shell/sh "git" "diff" "--name-only" last-success-sha1 current-sha1 :dir root-dir))
        files (str/split diff #"\n")
        f (condp = dir
            "all" #(or (matching-dir? % "components")
                       (matching-dir? % "systems"))
            "components" #(matching-dir? % dir)
            "systems" #(matching-dir? % dir)
            (fn [x] false))]
    (vec (sort (set (map #(second (str/split % #"/"))
                         (filter f files)))))))
