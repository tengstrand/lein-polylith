(ns leiningen.polylith.file
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.io File)
           (java.nio.file Files LinkOption Paths)
           (java.nio.file.attribute BasicFileAttributes FileAttribute PosixFilePermission PosixFilePermissions)
           (java.util HashSet)))

(defn delete-file [path]
  (clojure.java.io/delete-file path))

(defn delete-dir [path]
  (doseq [f (reverse (file-seq (clojure.java.io/file path)))]
    (clojure.java.io/delete-file f)))

(defn create-dir [path]
  (.mkdir (File. path)))

(defn str->path [path]
  (.toPath (clojure.java.io/file path)))

(defn create-symlink [path target]
  (Files/createSymbolicLink (str->path path) (str->path target) (make-array FileAttribute 0)))

(defn create-file [path rows]
  (io/delete-file path true)
  (let [sep (atom "")
          _ (doseq [row rows]
              (spit path (str row "\n") :append true))]))

(defn file-separator []
  (java.io.File/separator))

(defn- file-separator-regexp []
  (let [separator (file-separator)]
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

(defn path->filename [path]
  (last (str/split path (file-separator-regexp))))

(defn file-path->real-path [file-path]
  (str (.toRealPath (.toPath file-path) (into-array LinkOption []))))

(defn- keep? [path]
  (not (str/starts-with? (path->filename path) ".")))

(defn- component-path [dir path]
  (let [parts (str/split (subs path (count dir)) #"/")]
    [(second parts) path]))

(defn paths-in-dir [dir]
  (let [f (clojure.java.io/file dir)
        fs (file-seq f)
        paths (map str (filter #(.isFile %) fs))
        file-paths (filter keep? paths)]
    (map #(component-path dir %) file-paths)))

(defn path->dir-name [file-path]
  (let [dir (last (str/split (str file-path) #"/"))]
     (str/replace dir #"_" "-")))

(defn directories [dir]
  (let [files (.listFiles (clojure.java.io/file dir))]
    (filter #(.isDirectory %) files)))

(defn directory-names [dir]
  (filterv #(not (= "target" %))
    (map path->dir-name (directories dir))))

(defn current-path []
  (let [path (.getAbsolutePath (File. "."))]
    (subs path 0 (- (count path) 2))))

(defn parent-path [path]
  (when path
    (let [parts (str/split path #"/")]
      (when (= "development" (last parts))
        (subs path 0 (- (count path) 12))))))
