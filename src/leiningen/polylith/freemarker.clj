(ns leiningen.polylith.freemarker
  (:require [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.shared :as shared])
  (:import (freemarker.template TemplateNotFoundException)))

(defn ->column [[k v]]
  [(name k) v])

(defn ->entity [entity]
  (into {} (map ->column entity)))

(defn ->row [row]
  (map ->entity row))

(defn ->map [m]
  (map ->row m))

(defn configuration [template-dir]
  (doto (freemarker.template.Configuration.)
    (.setDirectoryForTemplateLoading (file/file template-dir))))

(defn write-file [config templates-root-dir template-file out-file-path table]
  (try
    (let [template (.getTemplate config template-file)
          out (file/writer out-file-path)]
      (.process template table out)
      [true])
    (catch TemplateNotFoundException _
      [false (str "Could not find template '" template-file "' in directory '" templates-root-dir "'.")])))
