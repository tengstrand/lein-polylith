(ns leiningen.polylith.cmd.compile
  (:require [clojure.java.shell :as shell]
            [leiningen.polylith.cmd.changes :as changes]))

;; TODO: move to common
(defn sh [& args]
  (let [{:keys [exit out err]} (apply shell/sh args)]
    (if (= 0 exit)
      out
      (throw (Exception. (str "Shell Err: " err " Exit code: " exit))))))

(defn find-changes [ws-path top-dir args]
  (let [changed-components (changes/changes ws-path top-dir "c" args)
        changed-bases (changes/changes ws-path top-dir "b" args)
        changed-systems (changes/changes ws-path top-dir "s" args)]
    (println)
    (apply println "Changed components:" changed-components)
    (apply println "Changed bases:" changed-bases)
    (apply println "Changed systems:" changed-systems)
    (println)
    [changed-components changed-bases changed-systems]))

(defn compile-it [ws-path dir changes]
  (doseq [change changes]
    (println "Compiling" (str dir "/" change))
    (println (sh "lein" "compile" :dir (str ws-path "/" dir "/" change)))))

(defn compile-changes [ws-path components bases systems]
  (println "Compiling interfaces")
  (println (sh "lein" "install" :dir (str ws-path "/interfaces")))
  (compile-it ws-path "components" components)
  (compile-it ws-path "bases" bases)
  (compile-it ws-path "systems" systems))

(defn execute [ws-path top-dir args]
  (let [[changed-components
         changed-bases
         changed-systems] (find-changes ws-path top-dir args)]
    (compile-changes ws-path changed-components changed-bases changed-systems)))
