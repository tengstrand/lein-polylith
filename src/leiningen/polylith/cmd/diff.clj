(ns leiningen.polylith.cmd.diff
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.git :as git]
            [leiningen.polylith.time :as time]))

(defn changed-file-paths-with-git [ws-path args]
  (let [last-successful-sha1 (git/parse-git-args ws-path args)
        current-sha1         (git/current-sha1 ws-path)
        paths                (git/diff ws-path last-successful-sha1 current-sha1)]
    (file/filter-invalid-paths paths)))

(defn changed-file-paths-with-time
  ([include-time? ws-path args]
   (let [time      (time/parse-time-args ws-path args)
         all-paths (file/valid-paths ws-path)]
     (file/changed-relative-paths include-time? ws-path all-paths time)))
  ([ws-path args]
   (changed-file-paths-with-time false ws-path args)))

(defn changed-file-paths
  ([include-time? ws-path args]
   (if (shared/ci?)
     (changed-file-paths-with-git ws-path args)
     (changed-file-paths-with-time include-time? ws-path args)))
  ([ws-path args]
   (changed-file-paths false ws-path args)))

(defn path-with-time->string [show-time? path]
  (if show-time?
    (str (first path) " " (time/->time (first path)) " " (second path))
    path))

(defn print-paths [paths show-time?]
  (doseq [path paths]
    (if (shared/ci?)
      (println " " path)
      (println " " (path-with-time->string show-time? path)))))

(defn execute [ws-path args]
  (let [show-time?   (contains? (set args) "+")
        without-plus (filter #(not= "+" %) args)
        paths        (changed-file-paths show-time? ws-path without-plus)]
    (print-paths paths show-time?)))
