(ns leiningen.polylith.cmd.shared
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [leiningen.polylith.file :as file]
            [clojure.set :as set])
  (:import (java.util.concurrent ExecutionException)))

(defn throw-polylith-exception
  ([message]
   (throw (ExecutionException. message (Exception.)))))

(defn print-error-message [e]
  (when-let [message (.getMessage e)]
    (println message)))

(defn interface? [flag]
  (contains? #{"i" "interface"} flag))

(defn +function? [flag]
  (contains? #{"+f" "+function"} flag))

(defn component? [flag]
  (contains? #{"c" "component"} flag))

(defn +component? [flag]
  (contains? #{"+c" "+component"} flag))

(defn base? [flag]
  (contains? #{"b" "base"} flag))

(defn system? [flag]
  (contains? #{"s" "system"} flag))

(defn workspace? [flag]
  (contains? #{"w" "workspace"} flag))

(defn has-args? [args & flags]
  (not (empty? (set/intersection (set args) (set flags)))))

(defn src-dir-name [directory]
  (str/replace directory #"-" "_"))

(defn entity-src-dir-name [directory]
  (str/replace directory #"_" "-"))

(defn path->file [path]
  (entity-src-dir-name (last (str/split (str path) #"/"))))

(defn full-name [top separator name]
  (if (str/blank? top)
    name
    (if (= "" name)
      (str top)
      (str top separator name))))

(defn full-dir-name [top name]
  (src-dir-name (full-name top "/" name)))

(defn src-root-dir [ws-path top-dir entity-dir entity]
  (if (str/blank? top-dir)
    (str ws-path "/" entity-dir "/" entity)
    (str ws-path "/" entity-dir "/" entity "/" top-dir)))

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
  (let [dirs     (str/split top-dir #"/")
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

(def env "The environment as a map, memoized for performance."
  (memoize #(into {} (System/getenv))))

(defn sh [& args]
  (let [;; apply doesn't accept extra args at the end so we must use concat
        args-with-env (concat args [:env (dissoc (env) "CLASSPATH")])
        {:keys [exit out err]} (apply shell/sh args-with-env)]
    (if (= 0 exit)
      out
      (do
        ;; Print out the stack trace with the error message
        (println out)
        (throw-polylith-exception (str "Shell Err: " err " Exit code: " exit))))))

(defn interfaces-src-dir [top-dir]
  (if (zero? (count top-dir))
    "interfaces/src"
    (str "interfaces/src/" top-dir)))

(defn libraries [path]
  (let [content (first (file/read-file path))
        index (ffirst
                (filter #(= :dependencies (second %))
                        (map-indexed vector content)))]
    (if index (nth content (inc index))
              [])))

(defn all-interfaces [ws-path top-dir]
  (set (file/directory-names (str ws-path "/" (interfaces-src-dir top-dir)))))

(defn all-components [ws-path]
  (set (file/directory-names (str ws-path "/components"))))

(defn all-bases [ws-path]
  (set (file/directory-names (str ws-path "/bases"))))

(defn all-systems [ws-path]
  (set (file/directory-names (str ws-path "/systems"))))

(defn all-environments [ws-path]
  (set (file/directory-names (str ws-path "/environments"))))

(defn libs [ws-path type entity]
  (libraries (str ws-path (str type entity "/project.clj"))))

(defn all-libraries [ws-path]
  (let [base-libs (mapcat #(libs ws-path "/bases/" %) (all-bases ws-path))
        component-libs (mapcat #(libs ws-path "/components/" %) (all-components ws-path))
        system-libs (mapcat #(libs ws-path "/systems/" %) (all-systems ws-path))
        env-libs (mapcat #(libs ws-path "/environments/" %) (all-environments ws-path))]

    (set (concat base-libs component-libs base-libs system-libs env-libs))))

(defn interface-of
  ([ws-path top-dir component]
   (interface-of ws-path top-dir component (all-interfaces ws-path top-dir)))
  ([ws-path top-dir component interfaces]
   (let [dir (str ws-path "/components/" component "/src/" (full-name top-dir "/" ""))
         directories (file/directory-names dir)]
     (first (filter #(contains? interfaces %) directories)))))

(defn ->interface-component [ws-path top-dir component interfaces]
  [(interface-of ws-path top-dir component interfaces) component])

(defn interface->component [ws-path top-dir interfaces entities]
  (into {} (filterv first
                    (map #(->interface-component ws-path top-dir % interfaces)
                         entities))))

(defn- ifc-comp->map [m [interface component]]
  (if (contains? m interface)
    (assoc m interface (conj (m interface) component))
    (assoc m interface [component])))

(defn link->entity [ws-path path]
  (try
    (nth (str/split (subs (file/file->real-path path)
                          (count ws-path)) #"/")
         2)
    (catch Exception _)))

(defn used-entities
  ([ws-path top-dir type system-or-env]
   (let [path (str ws-path "/" type "/" system-or-env "/src/" top-dir)]
     (set (map #(link->entity ws-path %) (file/directories path)))))
  ([ws-path top-dir system-or-env]
   (set (concat (used-entities ws-path top-dir "systems" system-or-env)
                (used-entities ws-path top-dir "environments" system-or-env))))
  ([ws-path top-dir]
   (let [sys-entities (mapcat #(used-entities ws-path top-dir "systems" %)
                              (all-systems ws-path))
         env-entities (mapcat #(used-entities ws-path top-dir "environments" %)
                              (all-environments ws-path))]
     (set (concat sys-entities env-entities)))))

(defn interface->components [ws-path top-dir used-components]
  (let [interfaces (all-interfaces ws-path top-dir)]
    (reduce ifc-comp->map {}
            (map #(->interface-component ws-path top-dir % interfaces)
                 used-components))))

(defn ci? []
  (or (System/getenv "CI")
      (System/getProperty "CI")))
