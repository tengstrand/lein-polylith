(ns leiningen.polylith.cmd.run
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn jar-path [ws-path system]
  (str ws-path "/systems/" system "/target/" system "-0.1-standalone.jar"))

(defn validate [ws-path system]
  (let [systems (shared/all-systems ws-path)]
    (cond
      (str/blank? system) [false "Missing system name."]
      (not (contains? systems system)) [false (str "The system '" system "' does not exist.")]
      (not (file/file-exists (jar-path ws-path system))) [false (str "No standalone jar found for '" system "'. Try execute 'build' first.")]
      :else [true])))

(defn run [ws-path system args]
  (let [sh-args (concat ["java" "-jar" (jar-path ws-path system)] args)]
    (println (apply shared/sh sh-args))))

(defn execute [ws-path [system & args]]
  (let [[ok? message] (validate ws-path system)]
    (if ok?
      (run ws-path system args)
      (println message))))
