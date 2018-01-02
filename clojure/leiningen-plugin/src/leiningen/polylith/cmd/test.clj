(ns leiningen.polylith.cmd.test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.match :as match]
            [leiningen.polylith.cmd.diff :as diff]))

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

(defn system->tests [ws-path system test-dir dir]
  (let [paths (map second (file/paths-in-dir (str ws-path "/" dir "/" system "/" test-dir)))]
    (map path->ns paths)))

(defn tests-or-empty [tests? ws-path dir test-dir changed-systems]
  (if tests?
    (mapcat #(system->tests ws-path % test-dir dir) changed-systems)
    []))

(defn all-tests
  ([ws-path [tests? integration-tests?]]
   (let [changed-systems (file/directory-names (str ws-path "/systems"))
         changed-components (file/directory-names (str ws-path "/components"))]
     (all-tests ws-path [tests? integration-tests?] changed-systems changed-components)))
  ([ws-path [tests? integration-tests?] [last-success-sha1 current-sha1]]
   (let [paths (diff/diff ws-path last-success-sha1 current-sha1)
         changed-systems (info/changed-systems ws-path paths)
         changed-components (info/changed-components ws-path paths)]
     (all-tests ws-path [tests? integration-tests?] changed-systems changed-components)))
  ([ws-path [tests? integration-tests?] changed-systems changed-components]
   (let [system-tests (tests-or-empty tests? ws-path "systems" "test" changed-systems)
         system-itests (tests-or-empty integration-tests? ws-path "systems" "test-int" changed-systems)
         component-tests (tests-or-empty tests? ws-path "components" "test" changed-components)
         component-itests (tests-or-empty integration-tests? ws-path "components" "test-int" changed-components)]
     (vec (sort (map str (concat system-tests system-itests component-tests component-itests)))))))

(defn execute [ws-path ignored-tests sha1 sha2 [cmd last-success-sha1 current-sha1]]
  (if (nil? cmd)
    (do
      (println "Missing parameters.")
      (help/test-cmd sha1 sha2))
    (let [u? (str/includes? cmd "u")
          i? (str/includes? cmd "i")
          show-single-line? (str/includes? cmd "-")
          show-multi-lines? (str/includes? cmd "+")
          tests (match/filter-tests
                  (if (and last-success-sha1 current-sha1)
                    (all-tests ws-path [u? i?] [last-success-sha1 current-sha1])
                    (all-tests ws-path [u? i?]))
                  ignored-tests)]
      (if (or show-single-line? show-multi-lines?)
        (show-tests tests show-single-line?)
        (run-tests tests show-single-line?)))))
