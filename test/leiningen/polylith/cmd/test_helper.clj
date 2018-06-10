(ns leiningen.polylith.cmd.test-helper
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.info]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.diff :as diff]
            [clojure.string :as str]))

(defn settings [ws-dir top-ns]
  {:root ws-dir
   :polylith {:top-namespace top-ns
              :clojure-version "1.9.0"}
   :clojure-version "1.9.0"})

(def root-dir (atom nil))

(defn test-setup-and-tear-down [f]
  (let [path (file/create-temp-dir! "polylith-root")]
    (if path
      (reset! root-dir path)
      (throw (Exception. (str "Could not create directory: " path))))
    (with-redefs [diff/ci? (fn []
                             (= (System/getProperty "CI") "CIRCLE"))]
      (f))
    (file/delete-dir path)))

(use-fixtures :each test-setup-and-tear-down)

(defn content [ws-dir directory]
  (file/read-file (str ws-dir "/" directory)))

(defn interfaces-project-content [name]
  [['defproject name "1.0"
    :description "Component interfaces"
    :dependencies [['org.clojure/clojure "1.9.0"]]
    :aot
    :all]])

(defn project-content [ns-name description dependencies]
  [['defproject ns-name "0.1"
    :description description
    :dependencies dependencies
    :aot
    :all]])

(defn component-project-content [name ns-name interfaces]
  [['defproject ns-name "0.1"
    :description (str "A " name " component")
    :dependencies [[interfaces "1.0"]
                   ['org.clojure/clojure "1.9.0"]]
    :aot
    :all]])

(defn base-project-content [name ns-name interfaces]
  [['defproject ns-name "0.1"
    :description (str "A " name " base")
    :dependencies [[interfaces "1.0"]
                   ['org.clojure/clojure "1.9.0"]]
    :aot
    :all]])

(defn split-lines [string]
  (str/split string #"\n"))

(defn print-relative-paths! [path]
  (let [paths (sort (filter #(not (str/starts-with? % ".git/"))
                            (file/relative-paths path)))]
    (doseq [row paths]
      (println (str "\"" row "\"")))))

