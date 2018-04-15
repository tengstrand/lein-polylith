(ns leiningen.polylith.cmd.test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.match :as match]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.shared :as shared]))

(defn show-tests [tests single-line-statement?]
  (if single-line-statement?
    (if (empty? tests)
      (println "echo 'Nothing changed - no tests executed'")
      (println (str "lein test " (str/join " " tests))))
    (doseq [test tests]
      (println " " test))))

(defn run-tests [tests sing-line-statement?]
  (if (zero? (count tests))
    (println "Nothing to test")
    (do
      (println "Start execution of" (count tests) "tests:")
      (show-tests tests sing-line-statement?)
      (apply shell/sh (concat ["lein" "test"] tests)))))

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

(defn execute [ws-path top-dir ignored-tests args]
  (let [[show-single-line?
         show-multi-lines?
         timestamp] (time/parse-time-args ws-path args)
        tests (match/filter-tests
                (all-tests ws-path top-dir timestamp)
                ignored-tests)]
    (if (or show-single-line? show-multi-lines?)
      (show-tests tests show-single-line?)
      (run-tests tests show-single-line?))))
