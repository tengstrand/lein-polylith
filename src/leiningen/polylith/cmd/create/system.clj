(ns leiningen.polylith.cmd.create.system
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.create.base :as create-base]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn create-dev-links [ws-path dev-dir base system base-dir system-dir]
  (let [root (str ws-path "/environments/" dev-dir)
        relative-parent-path (shared/relative-parent-path system-dir)
        base-path (str "../../../bases/" base)
        system-path (str "../../../systems/" system)
        relative-base-path (str relative-parent-path "bases/" base)]
    (file/create-symlink (str root "/docs/" base "-Readme.md")
                         (str base-path "/Readme.md"))
    (file/create-symlink (str root "/docs/" system "-Readme.md")
                         (str system-path "/Readme.md"))
    (file/create-symlink (str root "/resources/" base)
                         (str base-path "/resources/" base))
    (file/create-symlink (str root "/project-files/bases/" base "-project.clj")
                         (str "../" base-path "/project.clj"))
    (file/create-symlink (str root "/project-files/systems/" system "-project.clj")
                         (str "../" system-path "/project.clj"))
    (file/create-symlink (str root "/src/" base-dir)
                         (str relative-base-path "/src/" base-dir))
    (file/create-symlink (str root "/test/" base-dir)
                         (str relative-base-path "/test/" base-dir))))

(defn create [ws-path top-dir top-ns clojure-version system base-name]
  (let [base (if (str/blank? base-name) system base-name)
        proj-ns (shared/full-name top-ns "/" system)
        base-dir (shared/full-name top-dir "/" (shared/src-dir-name base))
        base-ns (shared/full-name top-ns "." base)
        system-dir (shared/full-name top-dir "/" (shared/src-dir-name system))
        base-relative-path (str (shared/relative-parent-path system-dir) "bases/" base)
        system-path (str ws-path "/systems/" system)
        project-content [(str "(defproject " proj-ns " \"0.1\"")
                         (str "  :description \"A " system " system.\"")
                         (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version) "]")
                         (str "  :aot :all")
                         (str "  :main " base-ns ".core)")]
        build-content ["#!/usr/bin/env bash"
                       "set -e"
                       ""
                       "lein uberjar"]
        doc-content [(str "# " system " system")
                     ""
                     "add documentation here..."]
        dev-dirs (file/directory-names (str ws-path "/environments"))]
    (when-not (file/file-exists (str ws-path "/bases/" base-dir))
      (create-base/create-base ws-path top-dir top-ns base clojure-version))

    (file/create-dir system-path)
    (file/create-dir (str system-path "/resources"))
    (file/create-file (str system-path "/resources/.keep") [""])
    (file/create-dir (str system-path "/src"))
    (file/create-file (str system-path "/project.clj") project-content)
    (file/create-file (str system-path "/build.sh") build-content)
    (file/create-file (str system-path "/Readme.md") doc-content)
    (file/make-executable! (str system-path "/build.sh"))
    (shared/create-src-dirs! ws-path (str "systems/" system "/src") [top-dir])
    (file/create-symlink (str system-path "/src/" base-dir)
                         (str base-relative-path "/src/" base-dir))
    (file/create-symlink (str system-path "/resources/" base)
                         (str "../../../bases/" base "/resources/" base))

    (doseq [dev-dir dev-dirs]
      (create-dev-links ws-path dev-dir base system base-dir system-dir))))
