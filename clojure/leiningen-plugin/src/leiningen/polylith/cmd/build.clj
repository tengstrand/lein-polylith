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

(defn find-changes [ws-path top-dir last-success-sha1 current-sha1]
  (let [changed-components (changes/changes ws-path top-dir "c" last-success-sha1 current-sha1)
        changed-bases      (changes/changes ws-path top-dir "b" last-success-sha1 current-sha1)
        changed-systems    (changes/changes ws-path top-dir "s" last-success-sha1 current-sha1)]
    (println)
    (apply println "Changed components:" changed-components)
    (apply println "Changed bases:" changed-bases)
    (apply println "Changed systems:" changed-systems)
    (println)
    [changed-components changed-bases changed-systems]))

(defn compile-it [ws-path dir changes]
  (doseq [change changes]
    (println "Compiling" (str dir "/" change))
    (println (sh "lein" "install" :dir (str ws-path "/" dir "/" change)))))

(defn compile-changes [ws-path components bases]
  (when (< 0 (count components))
    (println "Compiling interfaces")
    (println (sh "lein" "install" :dir (str ws-path "/interfaces"))))
  (compile-it ws-path "components" components)
  (compile-it ws-path "bases" bases))

(defn run-tests [ws-path changed-systems]
  (doseq [system changed-systems]
    (println "Testing" (str "systems/" system))
    (println (sh "lein" "test" :dir (str ws-path "/systems/" system)))))

(defn increment-version [ws-path build-number system]
  (let [file (str ws-path "/systems/" system "/project.clj")
        content (slurp file)
        lines (str/split content #"\n")
        first-line (first lines)
        quotation-mark (.indexOf first-line "\"")
        last-index (dec (count first-line))
                   version (str/split (subs first-line (inc quotation-mark) last-index) #"\.")
        new-version (str (inc (read-string (first version))) "." build-number)
        new-first-line (str (subs first-line 0 quotation-mark) "\"" new-version "\"")
        new-lines (into [new-first-line] (rest lines))
        new-content (str/join "\n" new-lines)]
    (spit file new-content)))

(defn build [ws-path build-number changed-systems]
  (doseq [system changed-systems]
    (println "Building" (str "systems/" system))
    (increment-version ws-path build-number system)
    (if-not (.exists (io/file (str ws-path "/systems/" system "/build.sh")))
      (println "Cannot find build script to run. Please add a build.sh to run under systems/" system " folder. Skipping build.")
      (println (sh "./build.sh" :dir (str ws-path "/systems/" system))))))

(defn execute [ws-path top-dir [sha1 sha2 build-number]]
  (if (or (nil? sha2)
          (nil? sha1)
          (nil? build-number))
    (do
      (println "Missing parameters.")
      (help/build sha1 sha2))
    (let [[changed-components
           changed-bases
           changed-systems] (find-changes ws-path top-dir sha1 sha2)]
      (compile-changes ws-path changed-components changed-bases)
      (run-tests ws-path changed-systems)
      (build ws-path build-number changed-systems))))
