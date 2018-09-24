(ns leiningen.polylith.cmd.sync.environments
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.sync.shared :as shared-env]
            [leiningen.polylith.file :as file]))

(defn ->entity [file index]
  (shared/entity-src-dir-name (first (str/split (subs (str file) index) #"/"))))

(defn updated-dev-lib [libs lib]
  (if (= "interfaces" (-> lib first name))
    libs
    (if (shared-env/index-of-lib libs lib)
      libs
      (conj libs lib))))

(defn updated-dev-libs [dev-libs entity-libs]
  (vec (sort-by first (reduce updated-dev-lib dev-libs entity-libs))))

(defn entity-libs [dev-libs [ws-path entity components]]
  (let [type (if (contains? (set components) entity) "components" "bases")
        libs (shared/libraries (str ws-path "/" type "/" entity "/project.clj"))]
    (updated-dev-libs dev-libs libs)))

(defn entities-libs [ws-path dev-libs entities components]
  (reduce entity-libs dev-libs (map #(vector ws-path % components) entities)))

(defn update-environment! [ws-path top-dir dev-project-path dev-libs environment all-components all-bases]
  (let [project-path (str ws-path "/" dev-project-path)
        path (str ws-path "/environments/" environment "/src/" top-dir)
        index (if (empty? top-dir) (count path) (inc (count path)))
        entities (set (map #(->entity % index) (file/source-files path)))
        components (filterv all-components entities)
        libs (vec (sort-by #(-> % first str) (entities-libs ws-path dev-libs entities components)))
        content (vec (first (file/read-file project-path)))
        index (inc (shared-env/deps-index content))
        new-content (seq (assoc content index libs))]
    (when (not= content new-content)
      (println (str "updated: " dev-project-path))
      (file/write-to-file project-path dev-project-path new-content))))

(defn update-environments [ws-path top-dir dev-project-path]
  (let [dev-libs (shared/libraries (str ws-path "/" dev-project-path))
        components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)]
    (doseq [environment (shared/all-environments ws-path)]
      (update-environment! ws-path top-dir dev-project-path dev-libs environment components bases))))
