(ns leiningen.polylith.cmd.doc
  (:require [clojure.java.browse :as browse]
            [clojure.set :as set]
            [clojure.string :as str]
            [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.doc.ifc-table :as ifc-table]
            [leiningen.polylith.cmd.doc.missing-components :as missing-ifc]
            [leiningen.polylith.cmd.doc.system :as sys]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.freemarker :as freemarker]
            [leiningen.polylith.cmd.doc.table :as table]))

(defn base-name [ws-path top-dir type-dir environment]
  (let [dir (shared/full-name top-dir "/" "")
        bases (shared/all-bases ws-path)
        directories (file/directories (str ws-path type-dir environment "/src/" dir))]
    (first (filterv #(contains? bases %) (map shared/path->file directories)))))

(defn entity-deps [{:keys [entity _ children]} result]
  (concat (reduce concat (map #(entity-deps % result) children))
          (conj result entity)))

(defn unused->component [ws-path top-dir component]
  {"name" component
   "interface" (shared/interface-of ws-path top-dir component)
   "type" "component"})

(defn system-info [ws-path top-dir all-bases type-dir system]
  (let [base (base-name ws-path top-dir type-dir system)]
    (when base
      (let [large-tree (sys/system-tree ws-path top-dir all-bases system base)
            usages (sys/entity-usages large-tree)
            medium-tree (sys/crop-branches 0 [999 0 large-tree usages {}])
            small-tree (sys/crop-branches 0 [1 0 medium-tree usages {}])
            added-entities (set (shared/used-entities ws-path top-dir "systems" system))
            used-entities (set (entity-deps medium-tree []))
            missing-components (missing-ifc/missing-components ws-path top-dir used-entities)
            unused-entities (set/difference added-entities used-entities)
            large-table (vec (table/calc-table ws-path top-dir large-tree))
            medium-table (vec (table/calc-table ws-path top-dir medium-tree))
            small-table (vec (table/calc-table ws-path top-dir small-tree))
            unreferenced-components (mapv #(unused->component ws-path top-dir %) unused-entities)]
        {"name"        system
         "largetable"   (freemarker/->map large-table)
         "mediumtable" (freemarker/->map medium-table)
         "smalltable"  (freemarker/->map small-table)
         "entities"    (vec (concat unreferenced-components missing-components))}))))

(def sorting {"component" 1
              "base" 2})

(defn ->entity-map [ws-path top-dir all-bases entity-deps entity]
  (let [interface (shared/interface-of ws-path top-dir entity)
        type (if (contains? all-bases entity)
               "base"
               "component")
        table (ifc-table/table ws-path top-dir entity entity-deps all-bases)]
    {"name" entity
     "type" type
     "interface" interface
     "table" (freemarker/->map table)
     "sort-order" (str (sorting type) entity)}))

(defn base-or-component [bases components entity]
  (or (contains? bases entity)
      (contains? components entity)))

(defn pimped-entities [ws-path top-dir all-bases all-components entities]
  (let [entity-deps (cdeps/interface-dependencies ws-path top-dir all-components all-bases)]
    (sort-by #(% "sort-order")
             (mapv #(->entity-map ws-path top-dir all-bases entity-deps %) entities))))

(defn env-entities [ws-path top-dir environment all-bases all-components]
  (let [dir (str ws-path "/environments/" environment "/src/" (shared/full-name top-dir "/" ""))
        entities (sort (filter #(base-or-component all-bases all-components %)
                               (map file/path->dir-name (file/directories dir))))]
    {"name" environment
     "entities" (pimped-entities ws-path top-dir all-bases all-components entities)}))

(defn environments [ws-path top-dir all-bases all-components]
  (mapv #(env-entities ws-path top-dir % all-bases all-components)
       (sort (shared/all-environments ws-path))))

(defn ->lib [[lib version]]
  {"name" lib
   "version" version})

(defn template-data [ws-path top-dir]
  (let [libraries (map ->lib (sort (filter #(not= 'interfaces (first %))
                                           (shared/all-libraries ws-path))))
        interfaces (shared/all-interfaces ws-path top-dir)
        all-bases (shared/all-bases ws-path)
        all-components (shared/all-components ws-path)
        systems (mapv #(system-info ws-path top-dir all-bases "/systems/" %)
                      (sort (shared/all-systems ws-path)))
        components (pimped-entities ws-path top-dir all-bases all-components all-components)
        bases (pimped-entities ws-path top-dir all-bases all-components all-bases)]
    {"workspace"    (last (str/split ws-path #"/"))
     "libraries"    libraries
     "interfaces"   (vec (sort interfaces))
     "components"   components
     "bases"        bases
     "systems"      systems
     "environments" (environments ws-path top-dir all-bases all-components)}))

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
            (println (str "  " message))))))))

(defn browse-file [browse? doc-path]
  (let [out-path (str doc-path "/" (first-html-file))]
    (when (and browse? (file/file-exists out-path))
      (browse/browse-url (file/url out-path)))))

(defn execute [ws-path top-dir args]
  (if (info/has-circular-dependencies? ws-path top-dir)
    (println (str "  Cannot generate documentation. Circular dependencies detected. "
                  "Run the 'info' command for details."))
    (let [browse? (not (shared/has-args? args "-browse"))
          generate? (not (shared/has-args? args "-generate"))
          doc-path (str ws-path "/doc")]
      (when generate?
        (generate-docs doc-path (template-data ws-path top-dir)))
      (browse-file browse? doc-path))))
