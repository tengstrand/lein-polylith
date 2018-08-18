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
            [leiningen.polylith.cmd.doc.table :as table]
            [clojure.java.io :as io]))

(defn project-description
  ([ws-path entity-dir entity]
   (project-description (str ws-path "/" entity-dir "/" entity)))
  ([path]
   (let [content (read-string (slurp (str path "/project.clj")))
         index (ffirst
                 (filter #(= :description (second %))
                         (map-indexed vector content)))]
     (if index
       (nth content (inc index))
       "*** Couldn't find the :description key in project.clj ***"))))

(defn ->lib [[lib version]]
  {"name" lib
   "version" version})

(defn ->libs [libraries]
  (mapv ->lib (sort (filter #(not= "interfaces" (-> % first name)) libraries))))

(defn entity-libs [ws-path type entity]
  (->libs (shared/libs ws-path (str "/" type "s/") entity)))

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

(defn ->name [name]
  {"name" name})

(defn system-info [ws-path top-dir all-bases type-dir system]
  (let [base (base-name ws-path top-dir type-dir system)]
    (when base
      (let [tree (sys/system-tree ws-path top-dir all-bases system base)
            used-entities (set (entity-deps tree []))
            usages (sys/entity-usages tree)
            interfaces (missing-ifc/interfaces-with-missing-components ws-path top-dir used-entities)
            large-tree (assoc tree :children (concat (:children tree) interfaces))
            medium-tree (sys/crop-branches 0 [999 0 large-tree usages {}])
            small-tree (sys/crop-branches 0 [1 0 medium-tree usages {}])
            added-entities (set (shared/used-entities ws-path top-dir "systems" system))
            unused-entities (set/difference added-entities used-entities)
            large-table (vec (table/calc-table ws-path top-dir large-tree))
            medium-table (vec (table/calc-table ws-path top-dir medium-tree))
            small-table (vec (table/calc-table ws-path top-dir small-tree))
            unreferenced-components (mapv #(unused->component ws-path top-dir %) unused-entities)]
        {"name"        system
         "description" (project-description ws-path "systems" system)
         "largetable"  (freemarker/->map large-table)
         "mediumtable" (freemarker/->map medium-table)
         "smalltable"  (freemarker/->map small-table)
         "libraries"   (entity-libs ws-path "system" system)
         "entities"    (mapv ->name used-entities)
         "unreferencedComponents" unreferenced-components}))))

(def sorting {"component" 1
              "base" 2})

(defn ->entity-map [ws-path top-dir all-bases entity-deps entity]
  (let [interface (shared/interface-of ws-path top-dir entity)
        type (if (contains? all-bases entity)
               "base"
               "component")
        table (ifc-table/table ws-path top-dir entity entity-deps all-bases)]
    {"name" entity
     "description" (project-description ws-path (str type "s") entity)
     "type" type
     "libraries" (entity-libs ws-path type entity)
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
  (let [root-dir (str ws-path "/environments/" environment)
        dir (str root-dir "/src/" (shared/full-name top-dir "/" ""))
        entities (sort (filter #(base-or-component all-bases all-components %)
                               (map file/path->dir-name (file/directories dir))))
        description (project-description ws-path "environments" environment)]
    {"name" environment
     "description" description
     "libraries" (entity-libs ws-path "environment" environment)
     "entities" (pimped-entities ws-path top-dir all-bases all-components entities)}))

(defn environments [ws-path top-dir all-bases all-components]
  (mapv #(env-entities ws-path top-dir % all-bases all-components)
       (sort (shared/all-environments ws-path))))

(defn ->workspace [ws-path]
  {"name" (last (str/split ws-path #"/"))
   "description" (project-description ws-path)})

(defn template-data [ws-path top-dir github-url]
  (let [libraries (->libs (shared/all-libraries ws-path))
        interfaces (shared/all-interfaces ws-path top-dir)
        all-bases (shared/all-bases ws-path)
        all-components (shared/all-components ws-path)
        systems (mapv #(system-info ws-path top-dir all-bases "/systems/" %)
                      (sort (shared/all-systems ws-path)))
        components (pimped-entities ws-path top-dir all-bases all-components all-components)
        envs (environments ws-path top-dir all-bases all-components)
        bases (pimped-entities ws-path top-dir all-bases all-components all-bases)]
    {"workspace"    (->workspace ws-path)
     "githomeurl"   github-url
     "libraries"    libraries
     "interfaces"   (vec (sort interfaces))
     "components"   components
     "bases"        bases
     "systems"      systems
     "environments" envs}))

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
        config (freemarker/configuration)]
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

(defn copy-style [ws-path]
  (let [path (str ws-path "/doc/style.css")
        content (-> "templates/style.css" io/resource slurp)]
    (file/create-file path [content])))

(defn execute [ws-path top-dir github-url args]
  (if (info/has-circular-dependencies? ws-path top-dir)
    (println (str "  Cannot generate documentation. Circular dependencies detected. "
                  "Run the 'info' command for details."))
    (let [browse? (not (shared/has-args? args "-browse"))
          generate? (not (shared/has-args? args "-generate"))
          doc-path (str ws-path "/doc")]
      (when generate?
        (generate-docs doc-path (template-data ws-path top-dir github-url)))
      (copy-style ws-path)
      (browse-file browse? doc-path))))
