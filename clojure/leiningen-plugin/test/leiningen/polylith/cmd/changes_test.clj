(ns leiningen.polylith.cmd.changes-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]))

(defn create-root-dir []
  (let [path (str (file/current-path) "/.polylith-test")]
    (if (file/file-exists path)
      (file/delete-dir path))
    (if (file/create-dir path)
      path
      (throw (Exception. (str "Could not create directory: " path))))))

(def sha1 "2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1")
(def sha2 "58cd8b3106c942f372a40616fe9155c9d2efd122")
(defn project []
  {:root (create-root-dir)
   :polylith {:ignored-tests []
              :top-ns "my.company"
              :top-dir "my/company"
              :dev-dirs ["development"]
              :clojure-version ["1.9.1"]
              :example-hash1 sha1
              :example-hash2 sha2}})

;(polylith/polylith project "changes" "c" sha1 sha2)
