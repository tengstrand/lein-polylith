(ns leiningen.gendeps
  (:require [polylith.core :as core]))

(defn ^:no-project-needed gendeps [project & keys]
  "Generate dependencies"
  (let [paths (core/paths-in-dir "src")]))
