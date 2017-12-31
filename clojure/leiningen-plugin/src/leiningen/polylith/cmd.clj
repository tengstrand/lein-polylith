(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [clojure.string :as str]
            [leiningen.polylith.cmd.create :as create-cmd]
            [leiningen.polylith.core :as core]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.help :as help]
            [leiningen.polylith.info :as info]
            [leiningen.polylith.match :as match]
            [leiningen.polylith.validate :as validate]
            [leiningen.polylith.version :as v]))

(defn help [[cmd]]
  (condp = cmd
    "changes" (help/changes)
    "create" (help/create)
    "delete" (help/delete)
    "deps" (help/deps)
    "diff" (help/diff)
    "info" (help/info)
    "project" (help/project)
    "settings" (help/settings)
    "test" (help/test-cmd)
    (help/help)))

(defn changes [ws-path [cmd last-success-sha1 current-sha1]]
  (if (nil? current-sha1)
    (do
      (println "Missing parameters.")
      (help/changes))
    (doseq [dir (core/changes ws-path cmd last-success-sha1 current-sha1)]
      (println (str " " dir)))))

(defn delete [ws-path top-dir top-ns dev-dirs [cmd name]]
  (let [[ok? msg] (validate/delete ws-path top-dir cmd name)]
    (if ok?
      (condp = cmd
        "c" (core/delete ws-path top-dir dev-dirs name))
      (do
        (println msg)
        (help/delete)))))

(defn ns->component [nspace]
  (first (str/split (namespace nspace) #"\.")))

(defn print-component-deps [dependencies]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (let [apis (sort (set (map ns->component (dependencies component))))]
      (doseq [api apis]
        (println " " api)))))

(defn print-api-deps [dependencies]
  (doseq [component (keys dependencies)]
    (println (str component ":"))
    (doseq [nspace (dependencies component)]
      (println " " nspace))))

(defn deps [ws-path [arg]]
  (let [dependencies (core/all-dependencies ws-path)]
    (if (= "f" arg)
      (print-api-deps dependencies)
      (print-component-deps dependencies))))

(defn diff [ws-path [last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters.")
      (help/diff))
    (let [paths (core/diff ws-path last-success-sha1 current-sha1)]
      (doseq [path paths]
        (println " " path)))))

(defn info [ws-path args]
  (let [cmd (first args)
        a? (= "a" cmd)
        filter? (= 1 (count cmd))
        [show-changed?
         show-unchanged?
         show-apis?] (if filter?
                            [(or a? (= "c" cmd))
                             (or a? (= "u" cmd))
                             a?]
                            [true true false])
        [last-success-sha1
         current-sha1] (if filter? (rest args) args)
        data (if (and last-success-sha1 current-sha1)
               (core/info ws-path last-success-sha1 current-sha1)
               (core/info ws-path))]
    (info/print-info data show-changed? show-unchanged? show-apis?)))

(defn ->dir [ws-ns top-dir]
  (or top-dir
    (str/replace ws-ns #"\." "/")))

(defn create [ws-path top-dir top-ns dev-dirs [cmd name ws-ns ws-top-dir]]
  (let [[ok? msg] (validate/create ws-path top-dir top-ns cmd name ws-ns)]
    (if ok?
      (condp = cmd
        "c" (create-cmd/create-component ws-path top-dir top-ns dev-dirs name)
        "w" (create-cmd/create-workspace (file/current-path) name ws-ns (->dir ws-ns ws-top-dir)))
      (do
        (println msg)
        (help/create)))))

(defn settings [ws-path settings]
  (println "workspace path:")
  (println " " ws-path)
  (println "settings:")
  (doseq [[k d] settings]
    (println " " k d)))

(defn test-cmd [ws-path ignore-tests [cmd last-success-sha1 current-sha1]]
  (if (nil? cmd)
    (do
      (println "Missing parameters.")
      (help/test-cmd))
    (let [u? (str/includes? cmd "u")
          i? (str/includes? cmd "i")
          show-single-line? (str/includes? cmd "-")
          show-multi-lines? (str/includes? cmd "+")
          tests (match/filter-tests
                  (if (and last-success-sha1 current-sha1)
                    (core/test-cmd ws-path [u? i?] [last-success-sha1 current-sha1])
                    (core/test-cmd ws-path [u? i?]))
                  ignore-tests)]
      (if (or show-single-line? show-multi-lines?)
        (core/show-tests tests show-single-line?)
        (core/run-tests tests show-single-line?)))))

(defn task-not-found [subtask]
  (println "Subtask" subtask "not found.")
  (help/help))
