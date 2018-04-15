(ns leiningen.polylith.cmd.test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.match :as match]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.time :as time]))

(defn show-tests [tests single-line-statment?]
  (if single-line-statment?
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

(defn ->tests [ws-path base-or-component]
  (let [paths (map second (file/paths-in-dir (str ws-path "/environments/development/test/" base-or-component)))]
    (map path->ns paths)))

(defn tests [ws-path changed-bases-or-components]
  (mapcat #(->tests ws-path %) changed-bases-or-components))

(defn all-tests [ws-path timestamp]
  (let [paths (map second (diff/do-diff ws-path timestamp))
        changed-bases (info/changed-bases ws-path paths)
        changed-components (info/changed-components ws-path paths)
        base-tests (tests ws-path changed-bases)
        component-tests (tests ws-path changed-components)]
     (vec (sort (map str (concat base-tests component-tests))))))

(defn execute [ws-path ignored-tests args]
  (let [[show-single-line?
         show-multi-lines?
         timestamp] (time/parse-time-args ws-path args)
        tests (match/filter-tests
                (all-tests ws-path timestamp)
                ignored-tests)]
    (if (or show-single-line? show-multi-lines?)
      (show-tests tests show-single-line?)
      (run-tests tests show-single-line?))))
