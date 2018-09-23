(ns leiningen.polylith.cmd.sync.out-of-sync-interfaces
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

(defn signatures [src-code]
  "Takes the source code of a def, function or macro
   and returns a list with the signatures."
  (let [type (first src-code)
        name (second src-code)
        code (drop-while #(not (or (list? %)
                                   (vector? %)))
                         src-code)]
    (if (= 'def type)
      #{[type name 0]}
      (if (vector? (first code))
        #{[type name (-> code first count)]}
        (set (map #(vector type name (-> % first count)) code))))))

(defn ifc-set [defs]
  (set (map second defs)))

(defn missing-defs [interface-path component-path]
  (let [interface-code (read-code interface-path)
        component-code (read-code component-path)
        interface-defs (set (mapcat signatures interface-code))
        component-defs (set (mapcat signatures component-code))
        missing (set/difference component-defs interface-defs)
        already-defined (set/intersection (ifc-set (filter #(not= 'def (first %)) missing))
                                          (ifc-set interface-defs))]
    {:missing missing
     :already-defined (filter #(contains? already-defined (first %)) missing)}))

(defn error-message [[type name arity]]
  (cond
    (= type 'defn) (str "\"function '" name "' with arity " arity " must be added manually\"")
    (= type 'defmacro) (str "\"macro '" name "' with arity " arity " must be added manually\"")
    :else (str "\"def '" name "' will be added automatically\"")))

(defn wspath [ws path]
  (let [index (str/index-of path (str "/" ws "/"))]
    (subs path (+ index (count ws) 2))))

(defn missing-definitions [ws interface-path component-path]
  (if (not (file/file-exists component-path))
    [false (str "Expected to find interface '" component-path "'.")]
    (let [path (wspath ws interface-path)
          {:keys [missing already-defined]} (missing-defs interface-path component-path)]
      (if (not (empty? already-defined))
        [false (str "Workspace interfaces are out of sync in '" path "': "
                    (str/join ", " (map error-message already-defined)))]
        [true missing]))))

(defn def-statement [[type name arity]]
  (cond
    (= 'def type) (str "(def " name ")")
    :else (str "(" type " " name " [" (str/join " " (repeat arity "_")) "])")))

(defn sync-interface! [ws-path ws top-dir ifc->components sub-path]
  (let [ns-path (if (str/blank? top-dir) "" (str "/" top-dir))
        interface-path (str ws-path "/interfaces/src" ns-path "/" sub-path)
        paths (->component-paths ws-path top-dir sub-path ifc->components)
        missing-defs (set (mapv #(missing-definitions ws interface-path %) paths))
        errors (str/join ", " (mapv second (sort (filter (complement first) missing-defs))))]
    (if (not (empty? errors))
      (do
        (println errors)
        [false errors])
      (let [defs (mapcat second (filter first missing-defs))
            path (wspath ws interface-path)]
        (when (not (empty? defs))
          (println (str "Added these definitions to '" path "':")))
        (doseq [missing-def defs]
          (let [statement (def-statement missing-def)]
            (file/append-to-file interface-path statement)
            (println (str "  " statement))))
        [true]))))

(defn interface-path [ns-path path]
  (let [index (+ (str/index-of path "/interfaces")
                 (count ns-path)
                 16)]
    (subs path index)))

; todo:
; - add missing namespaces in workspace interfaces.
; - write test that covers functions that exist in several components.
; - return with error if any component doesn't fulfill their interface.

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

;(sync-interfaces! ws-path top-dir)
