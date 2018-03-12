(ns leiningen.polylith.cmd.diff
  (:require [leiningen.polylith.cmd.help.diff :as diff-help]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn diff [ws-path last-success-sha1 current-sha1]
  (let [diff (:out (shell/sh "git" "diff" "--name-only" last-success-sha1 current-sha1 :dir ws-path))]
    (str/split diff #"\n")))

(defn execute [ws-path [sha1 sha2]]
  (if (or (nil? sha1)
          (nil? sha2))
    (do
      (println "Missing parameters.")
      (diff-help/help sha1 sha2))
    (let [paths (diff ws-path sha1 sha2)]
      (doseq [path paths]
        (println " " path)))))
