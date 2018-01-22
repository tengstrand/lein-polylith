(ns leiningen.polylith.cmd.build
  (:require [leiningen.polylith.cmd.changes :as changes]
            [clojure.java.shell :as shell]
            [leiningen.polylith.cmd.help :as help]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn sh [& args]
  (let [{:keys [exit out err]} (apply shell/sh args)]
    (if (= 0 exit)
      out
      (throw (Exception. (str "Shell Err: " err " Exit code: " exit))))))

(defn find-changes [ws-path last-success-sha1 current-sha1]
  (let [changed-components   (changes/changes ws-path "c" last-success-sha1 current-sha1)
        changed-builds       (changes/changes ws-path "b" last-success-sha1 current-sha1)
        changed-systems      (changes/changes ws-path "s" last-success-sha1 current-sha1)]
    (println)
    (apply println "Changed components:" changed-components)
    (apply println "Changed systems:" changed-systems)
    (apply println "Changed builds:" changed-builds)
    (println)
    [changed-components changed-systems changed-builds]))

(defn compile [ws-path dir changes]
  (doseq [change changes]
    (println "Compiling" (str dir "/" change))
    (println (sh "lein" "install" :dir (str ws-path "/" dir "/" change)))))

(defn compile-changes [ws-path components systems]
  (when (< 0 (count components))
    (println "Compiling apis")
    (println (sh "lein" "install" :dir (str ws-path "/apis"))))
  (compile ws-path "components" components)
  (compile ws-path "systems" systems))

(defn run-tests [ws-path changed-builds]
  (doseq [build changed-builds]
    (println "Testing" (str "builds/" build))
    (println (sh "lein" "test" :dir (str ws-path "/builds/" build)))))

(defn build [ws-path build-number changed-builds]
  (doseq [build changed-builds]
    (println "Building" (str "builds/" build))
    (if-not (.exists (io/file (str ws-path "/builds/" build "/build.sh")))
      (println "Cannot find build script to run. Please add a build.sh to run under builds/" build " folder. Skipping build.")
      (println (sh "./build.sh" :dir (str ws-path "/builds/" build))))))

(defn execute [ws-path [last-success-sha1 current-sha1 build-number]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1)
          (nil? build-number))
    (do
      (println "Missing parameters.")
      (help/build))
    (let [[changed-components
           changed-systems
           changed-builds] (find-changes ws-path last-success-sha1 current-sha1)]
      (compile-changes ws-path changed-components changed-systems)
      (run-tests ws-path changed-builds)
      (build ws-path build-number changed-builds))))
