(ns leiningen.polylith.git
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared])
  (:import (java.io FileNotFoundException)))

(defn git-bookmarks [ws-path]
  (try
    (read-string (slurp (str ws-path "/.polylith/git.edn")))
    (catch FileNotFoundException _ {})))

(defn current-sha1 [ws-path]
  (if-let [current-sha1 (first (str/split (shared/sh "git" "rev-parse" "HEAD" :dir ws-path) #"\n"))]
    current-sha1
    (throw (Exception. "Workspace does not have a git repository or a valid first commit."))))

(defn diff [ws-path hash-1 hash-2]
  (let [changed-files (if hash-1
                        (shared/sh "git" "diff" "--name-only" hash-1 hash-2 :dir ws-path)
                        (shared/sh "git" "ls-tree" "-r" "HEAD" "--name-only" :dir ws-path))]
    (str/split changed-files #"\n")))

(defn last-successful-build-sha1 [ws-path]
  (:last-successful-build (git-bookmarks ws-path)))

(defn set-bookmark! [ws-path bookmark]
  (println "Set" bookmark "in git.edn")
  (let [bookmarks (assoc (git-bookmarks ws-path)
                    bookmark (current-sha1 ws-path))
        file      (str ws-path "/.polylith/git.edn")]
    (pp/pprint bookmarks (clojure.java.io/writer file))))

(defn bookmark->sha1 [ws-path bookmark]
  (let [bookmarks (git-bookmarks ws-path)
        bookmark  (keyword bookmark)
        sha1      (bookmarks bookmark)]
    sha1))

(defn valid-sha1? [ws-path arg]
  (try
    (str/starts-with? (shared/sh "git" "cat-file" "-t" arg :dir ws-path) "commit")
    (catch Exception _
      false)))

(defn parse-git-args [ws-path [bookmark-or-sha1]]
  (let [sha1 (if bookmark-or-sha1
               (if (valid-sha1? ws-path bookmark-or-sha1)
                 bookmark-or-sha1
                 (bookmark->sha1 ws-path bookmark-or-sha1))
               (last-successful-build-sha1 ws-path))]
    sha1))
