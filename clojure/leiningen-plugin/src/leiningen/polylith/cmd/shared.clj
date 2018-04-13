(ns leiningen.polylith.cmd.shared
  (:require [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn full-name [top separator name]
  (if (zero? (count top)) name (str top separator name)))

(defn ->dependency [library lib-and-version]
  "lib-and-version can either be a single library version number
   or 'library (space) library-version'"
  (let [[lib ver] (str/split lib-and-version #" ")]
    (if ver
      (str "[" lib " \"" ver "\"]")
      (str "[" library " \"" lib "\"]"))))

(defn src-dirs [ws-path src-dir top-dir]
  "Helper function for create-src-dirs!.
   returns a list of full paths based 'ws-path', 'src-dir'
   and the directories in 'top-dir', e.g.
   if top-dir is 'a/b/c' then it returns something similar to:
     ['.../a' '.../a/b' '.../a/b/c']
   where '.../' is 'ws-dir/src-dir/'."
  (let [dirs (str/split top-dir #"/")
        new-dirs (mapv #(str ws-path "/" src-dir "/" (str/join "/" (take % dirs)))
                       (range 1 (-> dirs count inc)))]
    (if (zero? (count dirs))
      []
      new-dirs)))

(defn create-src-dirs!
  "This function assumes that the workspace is already created.
   It creates 'ws-dir'/'src-dir' + all directories given in
   the incoming 'top-dirs' parameter, beneath that directory."
  ([ws-path src-dir top-dirs]
   (file/create-dir (str ws-path "/" src-dir))
   (let [dirs (sort-by #(count (str/split % #"/"))
                (set (mapcat #(src-dirs ws-path src-dir %) top-dirs)))]
     (doseq [dir dirs]
       (file/create-dir dir)))))

(defn relative-parent-path [dir]
  (let [levels (+ 2 (count (str/split dir #"/")))]
    (str/join (repeat levels "../"))))

(defn src-dir-name [directory]
  (str/replace directory #"-" "_"))

