(ns leiningen.polylith.cmd.create.system
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.create.base :as create-base]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn create-dev-links [ws-path dev-dir base system base-dir system-dir]
  (let [dir (str ws-path "/environments/" dev-dir)
        levels (+ 2 (count (str/split system-dir #"/")))
        parent-src-path (str/join (repeat levels "../"))
        bases-path (str "../../../bases/" base)
        systems-path (str "../../../systems/" system)
        bases-src-path (str parent-src-path "bases/" base)]
    (file/create-symlink-if-not-exists (str dir "/docs/" base "-Readme.md")
                                       (str bases-path "/Readme.md"))
    (file/create-symlink-if-not-exists (str dir "/docs/" system "-Readme.md")
                                       (str systems-path "/Readme.md"))
    (file/create-symlink-if-not-exists (str dir "/resources/" base)
                                       (str bases-path "/resources/" base))
    (file/create-symlink-if-not-exists (str dir "/project-files/bases/" base "-project.clj")
                                       (str "../" bases-path "/project.clj"))
    (file/create-symlink-if-not-exists (str dir "/project-files/systems/" system "-project.clj")
                                       (str "../" systems-path "/project.clj"))
    (file/create-symlink-if-not-exists (str dir "/src/" base-dir)
                                       (str bases-src-path "/src/" base-dir))
    (file/create-symlink-if-not-exists (str dir "/test/" base-dir)
                                       (str bases-src-path "/test/" base-dir))))

(defn create [ws-path top-dir top-ns clojure-version clojure-spec-version system base-name]
  (let [base (if (str/blank? base-name) system base-name)
        ;ns-name (shared/full-name top-ns "." base)
        proj-ns (shared/full-name top-ns "/" system)
        base-dir (shared/full-name top-dir "/" base)
        base-ns (shared/full-name top-ns "." base)
        system-dir (shared/full-name top-dir "/" system)
        levels (+ 2 (count (str/split system-dir #"/")))
        base-relative-path (str (str/join (repeat levels "../")) "bases/" base)
        systems-dir (str ws-path "/systems/" system)
        project-content [(str "(defproject " proj-ns " \"0.1\"")
                         (str "  :description \"A " system " system.\"")
                         (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version))
                         (str "                 " (shared/->dependency "org.clojure/spec" clojure-spec-version) "]")
                         (str "  :aot :all")
                         (str "  :main " base-ns ".core)")]
        build-content ["#!/usr/bin/env bash"
                       "lein compile"
                       "lein uberjar"]
        doc-content [(str "# " system " system")
                     ""
                     "add documentation here..."]
        dev-dirs (file/directory-names (str ws-path "/environments"))]
    (when-not (file/file-exists (str ws-path "/bases/" base-dir))
      (create-base/create-base ws-path top-dir top-ns base clojure-version clojure-spec-version))

    (file/create-dir systems-dir)
    (file/create-dir (str systems-dir "/resources"))
    (file/create-dir (str systems-dir "/src"))
    (file/create-file (str systems-dir "/project.clj") project-content)
    (file/create-file (str systems-dir "/build.sh") build-content)
    (file/create-file (str systems-dir "/Readme.md") doc-content)
    (file/make-executable (str systems-dir "/build.sh"))
    (shared/create-src-dirs! ws-path (str "systems/" system "/src") [top-dir])
    (file/create-symlink (str systems-dir "/src/" base-dir)
                         (str base-relative-path "/src/" base-dir))
    (file/create-symlink (str systems-dir "/resources/" base)
                         (str base-relative-path "/resources/" base))

    (doseq [dev-dir dev-dirs]
      (create-dev-links ws-path dev-dir base system base-dir system-dir))))
