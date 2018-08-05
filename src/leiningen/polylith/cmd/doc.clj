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

(defn missing->interface [interface]
  {"name" interface
   "type" "interface"})

(defn deps->names [[_ symbols]]
  (mapv name symbols))

(defn system-info [ws-path top-dir all-bases type-dir system]
  (let [deps (dependencies ws-path top-dir system)
        base (base-name ws-path top-dir type-dir system)]
    (when base
      (let [tree (dependency-tree base deps all-bases)
            usages (entity-usages tree)
            cropped-tree (crop-branches 0 [0 tree usages {}])
            added-entities (set (shared/used-entities ws-path top-dir "systems" system))
            used-entities (set (entity-deps tree []))
            used-components (set/intersection used-entities (shared/all-components ws-path))
            used-interfaces (set (map #(shared/interface-of ws-path top-dir %) used-components))
            used-bases (set/intersection used-entities (shared/all-bases ws-path))
            referenced-interfaces (set (mapcat deps->names (cdeps/interface-dependencies ws-path top-dir used-components used-bases)))
            missing-ifss (set/difference referenced-interfaces used-interfaces)
            unused-entities (set/difference added-entities used-entities)
            table (vec (calc-table cropped-tree))
            unused-components (mapv #(unused->component ws-path top-dir %) unused-entities)
            missing-interfaces (mapv missing->interface missing-ifss)]
        {"name" (shared/htmlify system)
         "table" (freemarker/->map table)
         "entities" (vec (concat unused-components missing-interfaces))}))))

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

(defn template-data [ws-path top-dir]
  (let [libraries (map ->lib (sort (filter #(not= 'interfaces (first %))
                                           (shared/all-libraries ws-path))))
        bases (shared/all-bases ws-path)
        components (shared/all-components ws-path)
        systems (mapv #(system-info ws-path top-dir bases "/systems/" %)
                      (sort (shared/all-systems ws-path)))]
    {"workspace"    (shared/htmlify (last (str/split ws-path #"/")))
     "libraries"    libraries
     "interfaces"   (mapv shared/htmlify (sort (shared/all-interfaces ws-path top-dir)))
     "components"   (pimped-entities ws-path top-dir bases components)
     "bases"        (map shared/htmlify (sort bases))
     "systems"      systems
     "environments" (environments ws-path top-dir bases components)}))

(def gen-doc-ok? (atom false))

(def in-out-files [{:template-file "workspace.ftl"
                    :output-file "workspace.html"}])

(defn html-file? [{:keys [output-file]}]
  (or
    (str/ends-with? output-file ".htm")
    (str/ends-with? output-file ".html")))

(defn first-html-file []
  (-> (filter html-file? in-out-files) first :output-file))

(defn generate-docs [doc-path data]
  (let [templates-root-dir (str doc-path "/templates")
        config (freemarker/configuration templates-root-dir)]
    (reset! gen-doc-ok? true)

    (doseq [{:keys [template-file output-file]} in-out-files]
      (when @gen-doc-ok?
        (let [output-path (str doc-path "/" output-file)
              [ok? message] (freemarker/write-file config templates-root-dir template-file output-path data)]
          (when (not ok?)
            (reset! gen-doc-ok? false)
            (println (str "  " message))))))
    @gen-doc-ok?))

(defn browse-file [browse? doc-path]
  (let [out-path (str doc-path "/" (first-html-file))]
    (when (and browse? (file/file-exists out-path))
      (browse/browse-url (file/url out-path)))))

(defn execute [ws-path top-dir doc-path args]
  (if (info/has-circular-dependencies? ws-path top-dir)
    (println (str "  Cannot generate documentation. Circular dependencies detected. "
                  "Run the 'info' command for details."))
    (let [browse? (not (shared/has-args? args "-browse"))
          generate? (not (shared/has-args? args "-generate"))]
      (when generate?
        (generate-docs doc-path (template-data ws-path top-dir)))
      (browse-file browse? doc-path))))
