(ns leiningen.polylith.cmd.doc
  (:require [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]
            [leiningen.polylith.file :as file]
            [clojure.java.browse :as browse]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.freemarker :as freemarker]
            [clojure.string :as str]))

(defn dependencies [ws-path top-dir system]
  (let [used-entities (shared/used-entities ws-path top-dir system)
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
   (assoc! result y (conj (get result y) {:entity (shared/htmlify entity)
                                          :type type
                                          :columns (count-columns tree)}))
   (if (empty? children)
     (doseq [yy (range (inc y) maxy)]
       (assoc! result yy (conj (get result yy) {:entity ""
                                                :type "component"
                                                :columns 1})))
     (doseq [child children]
       (calc-table child (inc y) maxy result)))))

(defn base-name [ws-path top-dir type-dir environment]
  (let [dir (shared/full-name top-dir "/" "")
        bases (shared/all-bases ws-path)
        directories (file/directories (str ws-path type-dir environment "/src/" dir))]
    (first (filterv #(contains? bases %) (map shared/path->file directories)))))

(defn entity-usages
  ([tree]
   (into {} (map (juxt first #(-> % second first second))
                 (group-by first (sort (entity-usages 0 [0 [] tree]))))))
  ([x [y result {:keys [entity children]}]]
   (conj (apply concat
                (map-indexed #(entity-usages % [(inc y)
                                                result
                                                %2])
                             children))
         (conj [entity [y x]]))))

(defn crop-branches [x [y {:keys [entity type children]} usages result]]
  (if (= [y x] (usages entity))
    (assoc result :entity entity
                  :type type
                  :children (vec (map-indexed #(crop-branches % [(inc y) %2 usages result]) children)))
    (assoc result :entity entity
                  :type type
                  :children [])))

(defn entity-deps [{:keys [entity _ children]} result]
  (concat (reduce concat (map #(entity-deps % result) children))
          (conj result entity)))

(defn unused->component [ws-path top-dir component]
  {"name" component
   "interface" (shared/interface-of ws-path top-dir component)
   "type" "component"})

(defn system-info [ws-path top-dir all-bases type-dir system]
  (let [deps (dependencies ws-path top-dir system)
        base (base-name ws-path top-dir type-dir system)]
    (when base
      (let [tree (dependency-tree base deps all-bases)
            usages (entity-usages tree)
            cropped-tree (crop-branches 0 [0 tree usages {}])
            added-entities (set (shared/used-entities ws-path top-dir "systems" system))
            used-entities (set (entity-deps tree []))
            unused-entities (set/difference added-entities used-entities)
            table (vec (calc-table cropped-tree))]
        {"name" (shared/htmlify system)
         "table" (freemarker/->map table)
         "entities" (mapv #(unused->component ws-path top-dir %) unused-entities)}))))

(def sorting {"component" 1
              "base" 2})

(defn ->map [ws-path top-dir bases entity]
  (let [interface (shared/interface-of ws-path top-dir entity)
        type (if (contains? bases entity)
               "base"
               "component")]
    {"name" (shared/htmlify entity)
     "type" type
     "interface" (shared/htmlify interface)
     "sort-order" (str (sorting type) entity)}))

(defn base-or-component [bases components entity]
  (or (contains? bases entity)
      (contains? components entity)))

(defn pimped-entities [ws-path top-dir bases entities]
  (sort-by #(% "sort-order")
           (map #(->map ws-path top-dir bases %) entities)))

(defn env-entities [ws-path top-dir environment bases components]
  (let [dir (str ws-path "/environments/" environment "/src/" (shared/full-name top-dir "/" ""))
        entities (sort (filter #(base-or-component bases components %)
                               (map file/path->dir-name (file/directories dir))))]
    {"name" (shared/htmlify environment)
     "entities" (pimped-entities ws-path top-dir bases entities)}))

(defn environments [ws-path top-dir bases components]
  (mapv #(env-entities ws-path top-dir % bases components)
       (sort (shared/all-environments ws-path))))

(defn ->lib [[lib version]]
  {"name" (shared/htmlify lib)
   "version" version})

(defn generate-doc [ws-path top-dir template-dir out-path template-file]
  (let [libraries (map ->lib (sort (filter #(not= 'interfaces (first %))
                                           (shared/all-libraries ws-path))))
        bases (shared/all-bases ws-path)
        components (shared/all-components ws-path)
        systems (mapv #(system-info ws-path top-dir bases "/systems/" %)
                      (sort (shared/all-systems ws-path)))
        table {"workspace"    (shared/htmlify (last (str/split ws-path #"/")))
               "libraries"    libraries
               "interfaces"   (mapv shared/htmlify (sort (shared/all-interfaces ws-path top-dir)))
               "components"   (pimped-entities ws-path top-dir bases components)
               "bases"        (map shared/htmlify (sort bases))
               "systems"      systems
               "environments" (environments ws-path top-dir bases components)}
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
          out-path (str doc-dir "/output/workspace.html")]
      (when generate?
        (let [[ok? message] (generate-doc ws-path top-dir template-dir out-path template-file)]
          (when (not ok?)
            (println (str "  " message)))))
      (when (and browse? (file/file-exists out-path))
        (browse/browse-url (file/url out-path))))))
