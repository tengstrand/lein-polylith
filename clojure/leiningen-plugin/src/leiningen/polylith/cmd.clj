(ns leiningen.polylith.cmd
  (:require [clojure.pprint :as p]
            [clojure.string :as str]
            [leiningen.polylith.core :as core]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.help :as help]
            [leiningen.polylith.info :as info]
            [leiningen.polylith.match :as match]))

(defn help []
  (println "Polylith 0.0.19-alpha (2017-12-01)")
  (println "  https://github.com/tengstrand/polylith")
  (println)
  (println "  lein polylith x     (where x is):")
  (println)
  (println "    changes x s1 s2   List changed components, systems or builds")
  (println "                      between two Git sha1:s")
  (println "    deps              List all dependencies")
  (println "    diff s1 s2        List all changes between two Git sha1:s")
  (println "    help              Show this help")
  (println "    info [x] [s1 s2]  list systems, components and builds")
  (println "    settings          The polylith settings in current project.clj")
  (println "    tests x [s1 s2]   Show or run tests"))


(defn info
  ([root-dir args]
   (if (= "help" (first args))
     (help/info)
     (let [filter? (= 1 (-> args first count))
           [show-changed?
            show-unchanged?] (if filter?
                                [(= "c" (first args))
                                 (= "u" (first args))]
                                [true true])
           [last-success-sha1
            current-sha1] (if filter? (rest args) args)
           data (if (and last-success-sha1 current-sha1)
                  (core/info root-dir last-success-sha1 current-sha1)
                  (core/info root-dir))]
       (info/print-info data show-changed? show-unchanged?)))))

(defn diff [root-dir [last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith diff s1 s2")
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1")
      (println)
      (println "   example:")
      (println "     lein polylith diff 2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1 58cd8b3106c942f372a40616fe9155c9d2efd122"))
    (let [paths (core/diff root-dir last-success-sha1 current-sha1)]
      (doseq [path paths]
        (println " " path)))))

(defn deps [root-dir]
  (doseq [dependency (core/all-dependencies root-dir)]
    (println dependency)))

(defn changes [root-dir [cmd last-success-sha1 current-sha1]]
  (if (or (nil? current-sha1)
          (nil? last-success-sha1))
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith changes x s1 s2")
      (println "     x = b -> show changed builds")
      (println "         s -> show changed systems")
      (println "         c -> show changed components")
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1")
      (println)
      (println "   example:")
      (println "     lein polylith changes s 2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1 58cd8b3106c942f372a40616fe9155c9d2efd122"))
    (doseq [dir (core/changes root-dir cmd last-success-sha1 current-sha1)]
      (println (str " " dir)))))

(defn task-not-found [subtask]
  (println "Subtask" subtask "not found.")
  (help))

(defn settings [root-dir settings]
  (println "root-dir:")
  (println " " root-dir)
  (println "settings:")
  (println " " settings))

(defn tests [root-dir ignore-tests [cmd last-success-sha1 current-sha1]]
  (if (nil? cmd)
    (do
      (println "Missing parameters, use the format:")
      (println "   lein polylith tests x [s1 s2]")
      (println "     x: different combinations of u,i,+,-:")
      (println "         u = unit tests")
      (println "         i = integration tests")
      (println "         + = execute tests")
      (println "         - = show test statement")
      (println "     combinations:")
      (println "         u = list unit tests")
      (println "         i = list integration tests")
      (println "         ui = list unit + integration tests")
      (println "         u- = show test statement (u tests)")
      (println "         i- = show test statement (i tests)")
      (println "         ui- = show test statement (u+i tests)")
      (println "         u+ = execute unit tests")
      (println "         i+ = execute integration tests")
      (println "         ui+ = execute unit + integration tests")
      (println "         ui+- = execute unit + integration tests + show test statement")
      (println)
      (println "     s1 = last successful Git sha1")
      (println "     s2 = current Git sha1")
      (println)
      (println "     -----------------------------------------------------------")
      (println)
      (println "     if s1 and s2 are given:")
      (println "       include tests from changed components and systems")
      (println "     if s1 and s2 are omitted:")
      (println "       include tests from all components and systems")
      (println)
      (println "     Component and system tests can be ignored in project.clj:")
      (println "       (defproject ....")
      (println "         ...")
      (println "         :polylith {:ignore-tests [...]}}")
      (println "         ...")
      (println "       )")
      (println "     where [...] is e.g.:")
      (println "       [\"comp*\"] = ignore all 'comp' tests including all underlying namespaces")
      (println "       [\"comp.x*\"] = ignore all 'comp.x' tests including all underlying namespaces")
      (println "       [\"comp.x\" \"sys.y\"] = ignore all 'comp.x' and 'sys.y' tests")
      (println)
      (println "   examples:")
      (println "     lein polylith tests u")
      (println "     lein polylith tests ui+ 2c851f3c6e7a5114cecf6bdd6e1c8c8aec8b32c1 58cd8b3106c942f372a40616fe9155c9d2efd122"))
    (let [u? (str/includes? cmd "u")
          i? (str/includes? cmd "i")
          single-line-statement? (str/includes? cmd "-")
          execute? (str/includes? cmd "+")
          tests (match/filter-tests
                  (if (and last-success-sha1 current-sha1)
                    (core/tests root-dir [u? i?] [last-success-sha1 current-sha1])
                    (core/tests root-dir [u? i?]))
                  ignore-tests)]
      (if execute?
        (core/run-tests tests single-line-statement?)
        (core/show-tests tests single-line-statement?)))))
