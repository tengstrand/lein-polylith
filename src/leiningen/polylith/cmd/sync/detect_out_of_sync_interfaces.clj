(ns leiningen.polylith.cmd.sync.detect-out-of-sync-interfaces
  (:require [clojure.string :as str]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]))

(defn ->ifc-components [ws-path top-dir m component all-interfaces]
  (let [interface (shared/interface-of ws-path top-dir component all-interfaces)]
    (if (contains? m interface)
      (assoc m interface (conj (m interface) component))
      (assoc m interface #{component}))))

(defn ->component-path [ws-path top-dir component sub-path]
  (let [ns-path (if (str/blank? top-dir) "" (str "/" top-dir))]
    (str ws-path "/components/" component "/src" ns-path "/" sub-path)))

(defn ->component-paths [ws-path top-dir sub-path ifc->components]
  (let [interface (first (str/split sub-path #"/"))
        components (sort (ifc->components interface))]
    (map #(->component-path ws-path top-dir % sub-path) components)))

(defn def? [code]
  (if (list? code)
    (let [f (first code)]
      (or (= f 'def) (= f 'defn) (= f 'defmacro)))
    false))

(defn read-code [path]
  (filterv def? (drop 1 (file/read-file path))))

(defn signatures [interface src-code]
  "Takes the source code of a def, function or macro
   and returns a list with the signatures."
  (let [type (first src-code)
        name (second src-code)
        code (drop-while #(not (or (list? %)
                                   (vector? %)))
                         src-code)]
    (if (= 'def type)
      #{[interface type name 0]}
      (if (vector? (first code))
        #{[interface type name (-> code first count)]}
        (set (map #(vector interface type name (-> % first count)) code))))))

(defn def-str [name]
  (str "\n(def " name ")"))

(defn ifc-set [defs]
  (set (map second defs)))

(defn missing-defs [interface interface-path component-path]
  (let [interface-code (read-code interface-path)
        component-code (read-code component-path)
        interface-defs (set (mapcat #(signatures interface %) interface-code))
        component-defs (set (mapcat #(signatures interface %) component-code))
        missing (set/difference component-defs interface-defs)
        already-defined (set/intersection (ifc-set missing) (ifc-set interface-defs))]
    {:missing missing
     :already-defined (filter #(contains? already-defined (second %)) missing)}))

(defn error-message [[interface type name arity]]
  (cond
    (= type 'defn) (str "\"function '" name "' with arity " arity " in workspace interface '" interface "' must be added manually\"")
    (= type 'defmacro) (str "\"macro '" name "' with arity " arity " in workspace interface '" interface "' must be added manually\"")
    :else (str name)))

(defn wspath [ws path]
  (let [index (str/index-of path (str "/" ws "/"))]
    (subs path (+ index (count ws) 2))))

(defn missing-definitions [ws interface interface-path component-path]
  (if (not (file/file-exists component-path))
    [false (str "Expected to find interface '" component-path "'.")]
    (let [path (wspath ws interface-path)
          {:keys [missing already-defined]} (missing-defs interface interface-path component-path)]
      (if (not (empty? already-defined))
        [false (str "Workspace interfaces are out of sync in '" path "': "
                    (str/join ", " (map error-message already-defined)))]
        [true missing]))))

(defn sync-interface! [ws-path ws top-dir ifc->components sub-path]
  (let [ns-path (if (str/blank? top-dir) "" (str "/" top-dir))
        interface (first (str/split sub-path #"/"))
        interface-path (str ws-path "/interfaces/src" ns-path "/" sub-path)
        paths (->component-paths ws-path top-dir sub-path ifc->components)
        results (mapv #(missing-definitions ws interface interface-path %) paths)
        errors (str/join ", " (mapv second (filter (complement first) results)))]
    (if (not (empty? errors))
      (do
        (println "#errors:" errors)
        [false errors])
      (do
        (doseq [missing (mapcat second results)]
          (println "#missing:" missing))
        [true]))))

(defn interface-path [ns-path path]
  (let [index (+ (str/index-of path "/interfaces")
                 (count ns-path)
                 16)]
    (subs path index)))

; todo: add missing namespaces in workspace interfaces.
; check for definitions that are not declared in components.

(defn sync-interfaces! [ws-path top-dir]
  (let [ws (last (str/split ws-path #"/"))
        interfaces (shared/all-interfaces ws-path top-dir)
        components (shared/all-components ws-path)
        ifc->components (reduce #(->ifc-components ws-path top-dir %1 %2 interfaces) {} components)
        ns-path (if (str/blank? top-dir) "" (str "/" top-dir))
        path (str ws-path "/interfaces/src" ns-path)
        interface-paths (mapv str (filterv #(str/ends-with? (str %) ".clj") (file/files path)))
        sub-paths (mapv #(interface-path ns-path %)
                        interface-paths)]
    (doseq [sub-path sub-paths]
      (sync-interface! ws-path ws top-dir ifc->components sub-path))))

;(def ws-path "/Users/joakimtengstrand/IdeaProjects/ws22")
;(def top-dir "com/abc")
;
;(sync-interfaces! ws-path top-dir)
