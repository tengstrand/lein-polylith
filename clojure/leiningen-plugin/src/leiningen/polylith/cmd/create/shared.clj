(ns leiningen.polylith.cmd.create.shared
  (:require [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn ->dependency [library lib-and-version]
  "lib-and-version can either be a single library version number
   or 'library (space) library-version'"
  (let [[lib ver] (str/split lib-and-version #" ")]
    (if ver
      (str "[" lib " \"" ver "\"]")
      (str "[" library " \"" lib "\"]"))))

(defn create-src-dirs! [ws-path top-dir src-dir]
  (file/create-dir (str ws-path "/" src-dir))
  (let [dirs (str/split top-dir #"/")
        new-dirs (map #(str ws-path "/" src-dir "/" (str/join "/" (take % dirs)))
                      (range 1 (-> dirs count inc)))]
    (if (not (zero? (count dirs)))
      (doseq [dir new-dirs]
        (file/create-dir dir)))))
