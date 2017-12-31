(ns leiningen.polylith.cmd.diff
  (:require [leiningen.polylith.cmd.help :as help]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn diff [ws-path last-success-sha1 current-sha1]
  (let [diff (:out (shell/sh "git" "diff" "--name-only" last-success-sha1 current-sha1 :dir ws-path))]
    (str/split diff #"\n")))

(defn execute [ws-path [last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters.")
      (help/diff))
    (let [paths (diff ws-path last-success-sha1 current-sha1)]
      (doseq [path paths]
        (println " " path)))))
