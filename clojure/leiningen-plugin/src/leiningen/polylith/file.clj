(ns leiningen.polylith.file
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.io File)
           (java.nio.file Files LinkOption Paths)
           (java.nio.file.attribute BasicFileAttributes)))

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

(defn file-path->real-path [file-path]
  (str (.toRealPath (.toPath file-path) (into-array LinkOption []))))

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

(defn path->dir-name [file-path]
  (let [dir (last (str/split (str file-path) #"/"))]
     (str/replace dir #"_" "-")))

(defn directories [dir]
  (let [files (.listFiles (clojure.java.io/file dir))]
    (filter #(.isDirectory %) files)))

(defn directory-names [dir]
  (filterv #(not (= "target" %))
    (map path->dir-name (directories dir))))

(defn parent-path []
  (let [absolute-path (.getAbsolutePath (File. "."))
        chars (+ 3 (-> (str/split absolute-path #"/") drop-last last count))]
    (subs absolute-path 0 (- (count absolute-path) chars))))
