(ns leiningen.polylith.cmd.sync
  (:require [leiningen.polylith.file :as file]
            [clojure.set :as set]
            [leiningen.polylith.cmd.shared :as shared]))

(defn libraries [path]
  (let [content (first (file/read-file path))
        index (ffirst
                (filter #(= :dependencies (second %))
                        (map-indexed vector content)))]
    (nth content (inc index))))

(defn deps-index [content]
  (ffirst
    (filter #(= :dependencies (second %))
            (map-indexed vector content))))

(defn merge-libs [dev-libs project-path]
  (let [comp1-libs (libraries project-path)
        dev-key (fn [m [k v]]
                  (if (contains? dev-libs k)
                    (into m [[k (dev-libs k)]])
                    (into m [[k v]])))]
    (reduce dev-key {} comp1-libs)))

(defn dep-row [i [[lib version] n]]
  (str "                 [" lib
       " \"" version "\"]"
       (if (= i n) "]" "")))

(defn align-libs [[k v]]
  (if (= :dependencies k)
    (let [n (-> v count dec dec)]
      (cons (str "  " k " [" (first v))
            (map-indexed dep-row (map #(vector % n) (rest v)))))
    (if (string? v)
      [(str "  " k " \"" v "\"")]
      [(str "  " k " " v)])))

(defn lib-versions-has-changed? [dev-project-path project-path]
  (let [dev-libs (into {} (libraries dev-project-path))
        libs (into {} (libraries project-path))
        shared-libs (set/intersection (set (map first libs))
                                      (set (map first dev-libs)))]
    (not
      (empty?
        (set/difference
          (set (filter #(contains? shared-libs (first %)) dev-libs))
          (set (filter #(contains? shared-libs (first %)) libs)))))))

(defn updated-content [dev-project-path project-path]
  (let [dev-libs (into {} (libraries dev-project-path))
        updated-libs (vec (map identity (merge-libs dev-libs project-path)))
        content (vec (first (file/read-file project-path)))
        index (inc (deps-index content))]
    (assoc content index updated-libs)))

(defn sync-libraries! [dev-project-path project-path]
  (when (lib-versions-has-changed? dev-project-path project-path)
    (let [content (updated-content dev-project-path project-path)
          [a b c & kv-pairs] content
          formatted-content (doall (cons (str "(" a " " b " \"" c "\"")
                                         (mapcat align-libs (partition 2 kv-pairs))))]
      (when (not (empty? formatted-content))
        (spit project-path (str (first formatted-content) "\n"))
        (doseq [row (drop-last (rest formatted-content))]
          (spit project-path (str row "\n") :append true))
        (spit project-path (str (last formatted-content) ")\n") :append true)))))

(defn sync-bases [ws-path dev-project-path]
  (let [bases (shared/all-bases ws-path)]
    (doseq [base bases]
      (sync-libraries! dev-project-path
                       (str ws-path "/bases/" base "/project.clj")))))

(defn sync-components [ws-path dev-project-path]
  (let [components (shared/all-components ws-path)]
    (doseq [component components]
      (sync-libraries! dev-project-path
                       (str ws-path "/components/" component "/project.clj")))))

(defn execute [ws-path]
  (let [dev-project-path (str ws-path "/environments/development/project.clj")]
    (sync-bases ws-path dev-project-path)
    (sync-components ws-path dev-project-path)))
