(ns leiningen.polylith.cmd.test
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.shared :as shared]))

(defn show-tests [tests]
  (if (empty? tests)
    (println "echo 'Nothing changed - no tests executed'")
    (println (str "lein test " (str/join " " tests)))))

(defn run-tests [test-namespaces ws-path]
  (if (zero? (count test-namespaces))
    (println "Nothing to test")
    (do
      (println "Start execution of tests in" (count test-namespaces) "namespaces:")
      (show-tests test-namespaces)
      (println (apply shared/sh (concat ["lein" "test"] test-namespaces [:dir (str ws-path "/environments/development")]))))))

(defn path->ns [path]
  (second (first (file/read-file path))))

(defn ->tests [ws-path top-dir base-or-component]
  (let [dir   (shared/full-dir-name top-dir base-or-component)
        path  (str ws-path "/environments/development/test/" dir)
        paths (map second (file/paths-in-dir path))]
    (map path->ns paths)))

(defn tests [ws-path top-dir changed-entities]
  (mapcat #(->tests ws-path top-dir %) changed-entities))

(defn all-test-namespaces [ws-path top-dir timestamp]
  (let [paths                     (mapv second (diff/do-diff ws-path timestamp))
        changed-bases             (info/changed-bases ws-path paths)
        changed-components        (info/changed-components ws-path paths)
        indirect-changed-entities (info/all-indirect-changes ws-path top-dir paths)
        changed-entities          (set (concat changed-components changed-bases indirect-changed-entities))
        entity-tests              (tests ws-path top-dir changed-entities)]
    (vec (sort (map str entity-tests)))))

(defn execute [ws-path top-dir args]
  (let [skip-compile?        (contains? (set args) "-compile")
        args-without-compile (filter #(not= "-compile" %) args)
        [_ timestamp] (time/parse-time-args ws-path args-without-compile)
        tests                (all-test-namespaces ws-path top-dir timestamp)]
    (when-not skip-compile? (compile/execute ws-path top-dir args-without-compile))
    (run-tests tests ws-path)))
