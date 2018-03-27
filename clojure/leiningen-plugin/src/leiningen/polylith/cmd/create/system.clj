(ns leiningen.polylith.cmd.create.system
  (:require [leiningen.polylith.cmd.create.shared :as shared]
            [leiningen.polylith.cmd.create.base :as create-base]
            [leiningen.polylith.file :as file]
            [clojure.string :as str]))

(defn create [ws-path top-dir top-ns clojure-version clojure-spec-version system base-name]
  (let [base (if (str/blank? base-name) system base-name)
        ;ns-name (shared/full-name top-ns "." base)
        proj-ns (shared/full-name top-ns "/" system)
        proj-base-dir (shared/full-name top-dir "/" base)
        proj-system-dir (shared/full-name top-dir "/" system)
        levels (+ 3 (count (str/split proj-system-dir #"/")))
        base-relative-path (str (str/join (repeat levels "../")) "bases/" system "/src/" proj-system-dir)
        ; ln -s ../../../../../../systems/sys1/src/my/comp/sys1
        systems-dir (str ws-path "/systems/" system)
        project-content [(str "(defproject " proj-ns " \"0.1\"")
                         (str "  :description \"A " system " system.\"")
                         (str "  :dependencies [" (shared/->dependency "org.clojure/clojure" clojure-version))
                         (str "                 " (shared/->dependency "org.clojure/spec" clojure-spec-version) "])")]
        build-content ["#!/usr/bin/env bash"]]
    (when-not (file/file-exists (str ws-path "/bases/" proj-base-dir))
      (create-base/create-base ws-path top-dir top-ns base clojure-version clojure-spec-version))

    (file/create-dir systems-dir)
    (file/create-dir (str systems-dir "/resources"))
    (file/create-dir (str systems-dir "/src"))
    (file/create-file (str systems-dir "/project.clj") project-content)
    (file/create-file (str systems-dir "/build.sh") build-content)
    (file/make-executable (str systems-dir "/build.sh"))
    (shared/create-src-dirs! ws-path (str "systems/" system "/src") [top-dir])

    (file/create-symlink (str systems-dir "/src/" proj-system-dir) base-relative-path)))
