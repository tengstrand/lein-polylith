(ns leiningen.polylith.cmd.test
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.compile :as compile]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.success :as success]
            [leiningen.polylith.cmd.sync :as sync]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]))

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

(defn all-test-namespaces [ws-path top-dir args]
  (let [paths                     (diff/changed-file-paths ws-path args)
        changed-bases             (info/changed-bases ws-path paths)
        changed-components        (info/changed-components ws-path paths)
        indirect-changed-entities (info/all-indirect-changes ws-path top-dir paths)
        changed-entities          (set (concat changed-components changed-bases indirect-changed-entities))
        entity-tests              (tests ws-path top-dir changed-entities)]
    (vec (sort (map str entity-tests)))))

(defn execute [ws-path top-dir args]
  (let [skip-circular-deps? (contains? (set args) "-circular-deps")
        skip-compile?       (contains? (set args) "-compile")
        skip-sync?          (contains? (set args) "-sync")
        run-success?        (contains? (set args) "+success")
        cleaned-args        (filter #(and (not= "-compile" %)
                                          (not= "-sync" %)
                                          (not= "-circular-deps" %)
                                          (not= "+success" %))
                                    args)
        tests               (all-test-namespaces ws-path top-dir cleaned-args)]
    (if (and (not skip-circular-deps?)
             (info/has-circular-dependencies? ws-path top-dir))
      (do
        (println "Cannot compile: circular dependencies detected.\n")
        (info/execute ws-path top-dir args)
        (throw (Exception. "Cannot compile: circular dependencies detected.")))
      (when (or skip-sync? (sync/sync-all ws-path top-dir "test"))
        (when-not skip-compile? (compile/execute ws-path top-dir (conj cleaned-args "-sync" "-circular-deps")))
        (run-tests tests ws-path)
        (when run-success?
          (let [bookmark-arg (first cleaned-args)
                bookmark (if (utils/bookmark? ws-path bookmark-arg)
                           bookmark-arg
                           "last-successful-test")]
            (success/execute ws-path [bookmark])))))))
