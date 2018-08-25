(ns leiningen.polylith.cmd.doc
  (:require [clojure.java.browse :as browse]
            [clojure.set :as set]
            [clojure.string :as str]
            [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.doc.crop :as crop]
            [leiningen.polylith.cmd.doc.env-table :as env-table]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.freemarker :as freemarker]
            [leiningen.polylith.cmd.doc.table :as table]
            [leiningen.polylith.cmd.doc.env-belonging :as belonging]
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

(defn ->name [name]
  {"name" name})

(def sorting {"component" 1
              "base" 2})

(defn ->entity [ws-path top-dir all-bases ifc-entity-deps entity->env entity]
  (let [interface (shared/interface-of ws-path top-dir entity)
        type (if (contains? all-bases entity)
               "base"
               "component")
        table-defs (env-table/table-defs ws-path top-dir all-bases entity->env ifc-entity-deps type entity)
        environments (mapv (fn [[type name]] {"id" (str/replace (str entity "__" type "__" name) "-" "_")
                                              "type" type,
                                              "name" name})
                           (sort (set (map #(vector ((% "info") "type") ((% "info") "name")) table-defs))))]
    {"name" entity
     "description" (project-description ws-path (str type "s") entity)
     "type" type
     "environments" environments
     "libraries" (entity-libs ws-path type entity)
     "interface" interface
     "tableDefs" table-defs
     "sort-order" (str (sorting type) entity)}))

(defn base-or-component [bases components entity]
  (or (contains? bases entity)
      (contains? components entity)))

(defn ->entities [ws-path top-dir all-bases all-components entities]
  (let [ifc-entity-deps (cdeps/interface-dependencies ws-path top-dir all-components all-bases)
        entity->env (belonging/entity->environment ws-path top-dir)]
    (sort-by #(% "sort-order")
             (mapv #(->entity ws-path top-dir all-bases ifc-entity-deps entity->env %) entities))))

(defn ->workspace [ws-path]
  {"name" (last (str/split ws-path #"/"))
   "description" (project-description ws-path)})

(defn template-data [ws-path top-dir github-url]
  (let [libraries (->libs (shared/all-libraries ws-path))
        interfaces (shared/all-interfaces ws-path top-dir)
        all-bases (shared/all-bases ws-path)
        all-components (shared/all-components ws-path)
        components (->entities ws-path top-dir all-bases all-components all-components)
        bases (->entities ws-path top-dir all-bases all-components all-bases)]
    {"workspace"    (->workspace ws-path)
     "githubUrl"    github-url
     "libraries"    libraries
     "interfaces"   (vec (sort interfaces))
     "components"   components
     "bases"        bases}))

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

(defn copy-doc-files [ws-path]
  (let [path (str ws-path "/doc/style.css")
        content (-> "templates/style.css" io/resource slurp)]
    (file/create-file path [content])
    (file/copy-resource-file! "images/github.png" (str ws-path "/doc/github.png"))))

(defn execute [ws-path top-dir github-url args]
  (if (info/has-circular-dependencies? ws-path top-dir)
    (println (str "  Cannot generate documentation. Circular dependencies detected. "
                  "Run the 'info' command for details."))
    (let [browse? (not (shared/has-args? args "-browse"))
          generate? (not (shared/has-args? args "-generate"))
          doc-path (str ws-path "/doc")]
      (when generate?
        (generate-docs doc-path (template-data ws-path top-dir github-url)))
      (copy-doc-files ws-path)
      (browse-file browse? doc-path))))
