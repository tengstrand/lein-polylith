(ns leiningen.polylith.file
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [zprint.core :as zp])
  (:import [java.io File PushbackReader FileNotFoundException]
           [java.nio.file Files LinkOption]
           [java.nio.file.attribute FileAttribute PosixFilePermission]))

(defn execute-fn [f message path]
  (try
    (f)
    (catch Exception e
      (println (str "Warning. " message " '" path "': " (.getMessage e))))))

(defn delete-file!
  ([path]
   (delete-file! path true))
  ([path silently]
   (execute-fn #(io/delete-file path silently)
               "Could not delete file" path)))

(defn delete-dir [path]
  (doseq [f (reverse (file-seq (clojure.java.io/file path)))]
    (if (or (Files/isSymbolicLink (.toPath f)) (.exists f))
      (delete-file! f true))))

(defn paths [dir-path]
  "Returns all directories and files in a directory recursively"
  (drop-last (reverse (file-seq (io/file dir-path)))))

(defn filter-invalid-paths [paths]
  (filter #(not (or (and (= File (type %)) (.isDirectory %))
                    (str/starts-with? (str %) ".")
                    (str/includes? (str %) "/.")
                    (str/includes? (str %) "/target/")))
          paths))

(defn valid-paths [ws-path]
  (filter-invalid-paths (paths ws-path)))

(defn files [dir-path]
  "Returns all files in a directory recursively"
  (filter #(.isFile %) (paths dir-path)))

(defn source-files [dir-path]
  "Returns all files in a directory recursively"
  (filter #(and (.isFile %) (str/ends-with? (str %) ".clj")) (paths dir-path)))

(defn relative-paths [path]
  (let [length (inc (count path))]
    (map #(str (subs % length))
         (map str (paths path)))))

(defn changed? [file point-in-time]
  (> (.lastModified file) point-in-time))

(defn ->time-and-name [file length]
  (vector (.lastModified file)
          (subs (str file) length)))

(defn ->name [file length]
  (subs (str file) length))

(defn changed-relative-paths [include-time? path paths point-in-time]
  (let [length (inc (count path))]
    (map #(if include-time? (->time-and-name % length) (->name % length))
         (filter #(changed? % point-in-time) paths))))

(defn latest-modified [paths]
  (reduce max (map #(.lastModified %) paths)))

(defn file-exists [path]
  (.exists (io/as-file path)))

(defn create-dir [^String path]
  (.mkdir (File. path)))

(defn str->path [path]
  (.toPath (clojure.java.io/file path)))

(defn create-symlink [path target]
  (when-not (file-exists path)
    (execute-fn
      #(Files/createSymbolicLink (str->path path)
                                 (str->path target)
                                 (make-array FileAttribute 0))
      "Could not create symbolic link" path)))

(defn create-file [path rows]
  (delete-file! path true)
  (doseq [row rows]
    (execute-fn
      #(spit path (str row "\n") :append true)
      "Could not create file" path)))

(defn replace-file! [path content]
  (delete-file! path)
  (create-file path content))

(defn read-file [path]
  (try
    (with-open [rdr (-> path
                        (io/reader)
                        (PushbackReader.))]
      (doall
        (take-while #(not= ::done %)
                    (repeatedly #(try (read rdr)
                                      (catch Exception _ ::done))))))
    (catch FileNotFoundException _
      nil)))

(defn path->filename [path]
  (last (str/split path #"/")))

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
  (let [f          (clojure.java.io/file dir)
        fs         (file-seq f)
        paths      (map str (filter #(.isFile %) fs))
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

(defn create-temp-dir! [folder-name]
  (let [temp-file (execute-fn #(File/createTempFile folder-name "")
                              "Could not create directory in temp directory" folder-name)
        _         (.delete temp-file)
        _         (.mkdirs temp-file)]
    (.getPath temp-file)))

(defn make-executable! [file-path]
  (let [path   (.toPath (File. ^String file-path))
        rights (hash-set PosixFilePermission/OWNER_READ
                         PosixFilePermission/OWNER_WRITE
                         PosixFilePermission/OWNER_EXECUTE)]
    (execute-fn #(Files/setPosixFilePermissions path rights)
                "Could not make file executable" path)))

(defn copy-resource-file! [source target-path]
  (delete-file! target-path true)
  (let [resource-file (io/input-stream (io/resource source))
        target-file   (io/file target-path)]
    (execute-fn #(io/copy resource-file target-file)
                "Could not copy resource file" target-path)))

(defn write-to-file [path filename content]
  (spit path (zp/zprint-file-str (str content "\n")
                                 filename
                                 {:width  60
                                  :map    {:comma? false}
                                  :vector {:respect-nl? true
                                           :wrap-coll?  false}
                                  :style  :community})))

