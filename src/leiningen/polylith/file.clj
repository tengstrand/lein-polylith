(ns leiningen.polylith.file
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pp])
  (:import [java.io File FileNotFoundException]
           [java.nio.file Files LinkOption Paths]
           [java.nio.file.attribute BasicFileAttributes FileAttribute PosixFilePermission PosixFilePermissions]
           [java.util Date]))

(defn delete-file [path]
  (clojure.java.io/delete-file path))

(defn delete-dir [path]
  (doseq [f (reverse (file-seq (clojure.java.io/file path)))]
    (if (or (Files/isSymbolicLink (.toPath f)) (.exists f))
      (clojure.java.io/delete-file f))))

(defn paths [path]
  (drop-last (reverse (file-seq (clojure.java.io/file path)))))

(defn relative-paths [path]
  (let [length (inc (count path))]
    (map #(str (subs % length))
         (map str (paths path)))))

(defn changed? [file point-in-time]
  (> (.lastModified file) point-in-time))

(defn ->time-and-name [file length]
  (vector (.lastModified file)
          (subs (str file) length)))

(defn changed-relative-paths [path paths point-in-time]
  (let [length (inc (count path))]
    (map #(->time-and-name % length)
         (filter #(changed? % point-in-time) paths))))

(defn latest-modified [paths]
  (reduce max (map #(.lastModified %) paths)))

(defn file-exists [path]
  (.exists (io/as-file path)))

(defn create-dir [path]
  (.mkdir (File. path)))

(defn str->path [path]
  (.toPath (clojure.java.io/file path)))

(defn create-symlink [path target]
  (when-not (file-exists path)
    (Files/createSymbolicLink (str->path path) (str->path target) (make-array FileAttribute 0))))

(defn create-file [path rows]
  (io/delete-file path true)
  (doseq [row rows]
    (spit path (str row "\n") :append true)))

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

(defn file->real-path [file-path]
  (let [path (str (.toRealPath (.toPath file-path) (into-array LinkOption [])))]
    (if (str/starts-with? path "/private")
      (subs path 8)
      path)))

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

(defn directory? [file]
  (or (Files/isSymbolicLink (.toPath file))
    (-> file file->real-path str clojure.java.io/file .isDirectory)
    (.isDirectory file)))

(defn directories [path]
  (let [files (.listFiles (clojure.java.io/file path))]
    (filter directory? files)))

(defn directory-names [dir]
  (filterv #(not (= "target" %))
    (map path->dir-name (directories dir))))

(defn current-path []
  (let [path (.getAbsolutePath (File. "."))]
    (subs path 0 (- (count path) 2))))

(defn temp-dir []
  (System/getProperty "java.io.tmpdir"))

(defn make-executable [file-path]
  (let [path (.toPath (File. ^String file-path))
        rights (hash-set PosixFilePermission/OWNER_READ
                         PosixFilePermission/OWNER_WRITE
                         PosixFilePermission/OWNER_EXECUTE)]
    (Files/setPosixFilePermissions path rights)))
