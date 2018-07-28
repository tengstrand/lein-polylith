(ns leiningen.polylith.cmd.doc
  (:require [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]
            [leiningen.polylith.file :as file]
            [clojure.java.browse :as browse]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.freemarker :as freemarker]))

(defn dependencies [ws-path top-dir system-or-environment]
  (let [used-entities (shared/used-entities ws-path top-dir system-or-environment)
        used-components (set/intersection used-entities (shared/all-components ws-path))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))]
    (cdeps/component-dependencies ws-path top-dir used-components used-bases)))

(defn entity-type [entity all-bases]
  (if (contains? all-bases entity)
    "base"
    "component"))

(defn dependency-tree [entity deps all-bases]
  {:entity entity
   :type (entity-type entity all-bases)
   :children (mapv #(dependency-tree % deps all-bases) (deps entity))})

(defn count-cols [{:keys [_ _ children]}]
  (cond
    (empty? children) 1
    :else (apply + (map count-cols children))))

(defn count-columns [tree]
  (let [sections (count-cols tree)]
    (if (zero? sections)
      0
      (dec (* 2 sections)))))

(defn max-deps [{:keys [_ _ children]} depth]
  (if (empty? children)
    depth
    (apply max (map #(max-deps % (inc depth)) children))))

(defn calc-table
  ([tree]
   (let [maxy (max-deps tree 1)
         result (transient (vec (repeat maxy [])))
         _ (calc-table tree 0 maxy result)
         table (reverse (persistent! result))]
     (map #(interpose {:type "spc"} %) table)))
  ([{:keys [entity type children] :as tree} y maxy result]
   (assoc! result y (conj (get result y) {:entity entity
                                          :type type
                                          :columns (count-columns tree)}))
   (if (empty? children)
     (doseq [yy (range (inc y) maxy)]
       (assoc! result yy (conj (get result yy) {:entity ""
                                                :type "component"
                                                :columns 1})))
     (doseq [child children]
       (calc-table child (inc y) maxy result)))))

(defn environment-base [ws-path top-dir environment]
  (let [dir (shared/full-name top-dir "/" "")
        bases (shared/all-bases ws-path)
        directories (file/directories (str ws-path "/environments/" environment "/src/" dir))]
    (first (filterv #(contains? bases %) (map shared/path->file directories)))))

(defn calc-system-table [ws-path top-dir system-or-env]
  (let [deps (dependencies ws-path top-dir system-or-env)
        all-bases (shared/all-bases ws-path)
        base (environment-base ws-path top-dir "development")]
    (when base
      (calc-table (dependency-tree base deps all-bases)))))

(defn generate-doc [ws-path top-dir template-dir out-path template-file]
  (let [table {"table" (freemarker/->map (calc-system-table ws-path top-dir "development"))
               "title" "development"}
        config (freemarker/configuration template-dir)]
    (freemarker/write-file config template-dir template-file out-path table)))

(defn execute [ws-path top-dir doc-dir template-dir args]
  (if (info/has-circular-dependencies? ws-path top-dir)
    (println (str "  Cannot generate documentation. Circular dependencies detected. "
                  "Run the 'info' command for details."))
    (let [browse? (not (contains? (set args) "-browse"))
          generate? (empty? (set/intersection (set args) #{"-gen" "-generate"}))
          template-file (or (first (filter #(not= "-browse" %) args))
                            "workspace.ftl")
          out-path (str doc-dir "/development.html")]
      (when generate?
        (let [[ok? message] (generate-doc ws-path top-dir template-dir out-path template-file)]
          (when (not ok?)
            (println (str "  " message)))))
      (when (and browse? (file/file-exists out-path))
        (browse/browse-url (file/url out-path))))))
