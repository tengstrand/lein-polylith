(ns leiningen.polylith.cmd.test
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.shared :as shared]))

(defn show-tests [tests]
  (if (empty? tests)
    (println "echo 'Nothing changed - no tests executed'")
    (println (str "lein test " (str/join " " tests)))))

(defn run-tests [tests ws-path]
  (if (zero? (count tests))
    (println "Nothing to test")
    (do
      (println "Start execution of" (count tests) "tests:")
      (show-tests tests)
      (println (apply shared/sh (concat ["lein" "test"] tests [:dir (str ws-path "/environments/development")]))))))

(defn path->ns [path]
  (second (first (file/read-file path))))

(defn ->tests [ws-path top-dir base-or-component]
  (let [dir (shared/full-name top-dir "/" (shared/src-dir-name base-or-component))
        path (str ws-path "/environments/development/test/" dir)
        paths (map second (file/paths-in-dir path))]
    (map path->ns paths)))

(defn tests [ws-path top-dir changed-bases-or-components]
  (mapcat #(->tests ws-path top-dir %) changed-bases-or-components))

(defn all-tests [ws-path top-dir timestamp]
  (let [paths (mapv second (diff/do-diff ws-path timestamp))
        changed-bases (info/changed-bases ws-path paths)
        changed-components (info/changed-components ws-path paths)
        base-tests (tests ws-path top-dir changed-bases)
        component-tests (tests ws-path top-dir changed-components)]
     (vec (sort (map str (concat base-tests component-tests))))))

(defn execute [ws-path top-dir args]
  (let [[_ timestamp] (time/parse-time-args ws-path args)
        tests (all-tests ws-path top-dir timestamp)]
    (run-tests tests ws-path)))
