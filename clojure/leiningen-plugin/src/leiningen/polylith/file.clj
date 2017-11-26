(ns leiningen.polylith.file
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.io File)))

(defn create-dir [path]
  (.mkdir (File. path)))

(defn create-file [path rows]
  (io/delete-file path true)
  (spit path "[" :append false)
  (let [sep (atom "")
        _ (doseq [row rows]
            (do
              (spit path (str @sep row "\n") :append true)
              (reset! sep " ")))]
    (spit path "]" :append true)))

(defn file-separator []
  (java.io.File/separator))

(defn- file-separator-regexp []
  (let [separator (java.io.File/separator)]
    (if (= "\\" separator)
      #"\\"
      #"/")))

(defn read-file [path]
  (with-open [rdr (-> path
                      (io/reader)
                      (java.io.PushbackReader.))]
    (doall
      (take-while #(not= ::done %)
                  (repeatedly #(try (read rdr)
                                    (catch Exception _ ::done)))))))

(defn- path->filename [path]
  (last (str/split path (file-separator-regexp))))

(defn- keep? [path]
  (not (str/starts-with? (path->filename path) ".")))

(defn- component-path [path]
  (let [parts (str/split path #"/")]
    [(second parts) path]))

;; todo: support nested directory structures
(defn paths-in-dir [dir]
  (let [f (clojure.java.io/file dir)
        fs (file-seq f)
        paths (map str (filter #(.isFile %) fs))
        file-paths (filter keep? paths)]
    (map component-path file-paths)))

(defn directory-names [dir]
  (let [files (.listFiles (clojure.java.io/file dir))]
    (mapv #(last (str/split (str %) #"/"))
         (filter #(.isDirectory %) files))))
