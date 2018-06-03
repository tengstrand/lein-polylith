(ns leiningen.polylith.cmd.diff
  (:require [environ.core :refer [env]]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]))

(defn changed-file-paths-with-git [ws-path args])

(defn changed-file-paths-with-time
  ([include-time? ws-path args]
   (let [time (time/parse-time-args ws-path args)
         all-paths (time/paths ws-path)]
     (file/changed-relative-paths include-time? ws-path all-paths time)))
  ([ws-path args]
   (changed-file-paths-with-time false ws-path args)))

(defn changed-file-paths
  ([include-time? ws-path args]
   (if (env :ci)
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
    (if (env :ci)
      (println " " path)
      (println " " (path-with-time->string show-time? path)))))

(defn execute [ws-path args]
  (let [show-time?   (contains? (set args) "+")
        without-plus (filter #(not= "+" %) args)
        paths        (changed-file-paths show-time? ws-path without-plus)]
    (print-paths paths show-time?)))
