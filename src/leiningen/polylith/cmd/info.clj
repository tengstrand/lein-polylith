(ns leiningen.polylith.cmd.info
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]))

(defn changed-dirs [dir file-paths]
  (let [n (count (str/split dir #"/"))
        nidx #(nth % n)
        f #(and (str/starts-with? % (str dir "/"))
                (> (count (str/split % #"/")) 2))]
    (vec (sort (set (map #(nidx (str/split % #"/"))
                         (filter f file-paths)))))))

(defn changed-base? [ws-path path changed-bases]
  (let [bases-path (str ws-path "/bases")
        base? (str/starts-with? path bases-path)
        changed? (and
                   base?
                   (let [base (second (str/split (subs path (count bases-path)) #"/"))]
                     (contains? (set changed-bases) base)))]
    {:base?  base?
     :changed? changed?}))

(defn changed-component? [ws-path path changed-components]
  (let [components-path (str ws-path "/components")
        component? (str/starts-with? path components-path)
        changed? (and
                   component?
                   (let [component (second (str/split (subs path (count components-path)) #"/"))]
                     (contains? (set changed-components) component)))]
    {:component? component?
     :changed?   changed?}))

(defn changed? [ws-path file changed-bases changed-components]
  (let [path (file/file->real-path file)
        changed-base (changed-base? ws-path path changed-bases)
        changed-component (changed-component? ws-path path changed-components)]
    {:name     (file/path->dir-name path)
     :type     (cond
                 (:base? changed-base) "-> base"
                 (:component? changed-component) "-> component"
                 :else "?")
     :changed? (cond
                 (:base? changed-base) (:changed? changed-base)
                 (:component? changed-component) (:changed? changed-component)
                 :else false)}))


(defn system-links [ws-path top-dir system changed-bases changed-components]
  (let [dir (if (zero? (count top-dir)) "/src" (str "/src/" top-dir))]
    (mapv #(changed? ws-path % changed-bases changed-components)
          (file/directories (str ws-path "/systems/" system dir)))))

(defn system-info [ws-path top-dir systems changed-bases changed-components]
  (into {} (mapv (juxt identity #(system-links ws-path top-dir % changed-bases changed-components)) systems)))

(defn any-changes? [systems-info system]
  (or (some true? (map :changed? (systems-info system))) false))

(defn base-or-component-changed? [systems-info changed-systems]
  (let [base-changes (map (juxt identity #(any-changes? systems-info %)) (keys systems-info))]
    (map (juxt first #(or (last %) (contains? changed-systems (first %)))) base-changes)))

(defn all-interfaces [ws-path top-dir]
  (let [dir (if (zero? (count top-dir))
              "/interfaces/src"
              (str "/interfaces/src/" top-dir))]
    (set (file/directory-names (str ws-path dir)))))

(defn all-bases [ws-path]
  (set (file/directory-names (str ws-path "/bases"))))

(defn all-components [ws-path]
  (set (file/directory-names (str ws-path "/components"))))

(defn all-systems [ws-path]
  (set (file/directory-names (str ws-path "/systems"))))

(defn all-changed-system-dirs
  ([paths bases]
   (set (filter bases (changed-dirs "systems" paths)))))

(defn changed-interfaces
  ([ws-path paths top-dir]
   (changed-interfaces paths (all-interfaces ws-path top-dir)))
  ([paths interfaces]
   (set (filter interfaces (set (changed-dirs "interfaces/src" paths))))))

(defn changed-components
  ([ws-path paths]
   (changed-components ws-path paths (all-components ws-path)))
  ([ws-path paths components]
   (set (filter components (changed-dirs "components" paths)))))

(defn changed-bases
  ([ws-path paths]
   (changed-bases ws-path paths (all-bases ws-path)))
  ([ws-path paths bases]
   (set (filter bases (set (changed-dirs "bases" paths))))))

(defn changed-systems
  ([ws-path paths top-dir bases]
   (changed-systems (system-info ws-path
                                 top-dir
                                 (all-systems ws-path)
                                 (changed-bases ws-path paths)
                                 (changed-components ws-path paths))
                    (all-changed-system-dirs paths bases)))
  ([systems-info changed-system-dirs]
   (mapv first (filter second (base-or-component-changed? systems-info (set changed-system-dirs))))))

(defn info [ws-path top-dir timestamp]
   (let [paths (mapv second (diff/do-diff ws-path timestamp))
         interfaces (all-interfaces ws-path top-dir)
         systems (all-systems ws-path)
         components (all-components ws-path)
         bases (all-bases ws-path)
         ch-interfaces (changed-interfaces ws-path paths interfaces)
         ch-systems (changed-systems ws-path paths top-dir bases)
         ch-components (changed-components ws-path paths components)
         ch-bases (changed-bases ws-path paths bases)]
     {:interfaces          (-> interfaces sort vec)
      :systems             (-> systems sort vec)
      :components          (-> components sort vec)
      :bases               (-> bases sort vec)
      :diff                paths
      :changed-interfaces  ch-interfaces
      :changed-systems     ch-systems
      :changed-components  ch-components
      :changed-bases       ch-bases
      :changed-systems-dir (all-changed-system-dirs paths bases)
      :systems-info        (system-info ws-path top-dir systems ch-bases ch-components)}))

(defn print-entity
  ([spaces entity changes]
   (let [changed? (contains? changes entity)
         star (if changed? " *" "")]
     (print-entity (str spaces entity star))))
  ([spaces entity type maxlength changed?]
   (let [star (if changed? " *" "")
         star-spaces (str/join (repeat (- maxlength (count (str entity star))) " "))
         string (str spaces entity star star-spaces type)]
     (print-entity string)))
  ([string]
   (println string)))

(defn print-info [{:keys [interfaces
                          components
                          bases
                          changed-interfaces
                          changed-bases
                          changed-components
                          changed-systems-dir
                          systems-info]}]
  (let [systems (-> systems-info keys sort)
        name-counts (map #(+ 3 (count (:name %)) (if (:changed? %) 2 0))
                         (mapcat second systems-info))
        maxlength (if (empty? name-counts) 150 (apply max name-counts))]

    (println "interfaces:")
    (doseq [interface interfaces]
      (print-entity "  " interface changed-interfaces))

    (println "components:")
    (doseq [component components]
      (print-entity "  " component changed-components))

    (println "bases:")
    (doseq [base bases]
      (print-entity "  " base changed-bases))

    (println "systems:")
    (doseq [system systems]
      (let [infos (sort-by :name (systems-info system))]
        (when (or (-> infos empty? not)
                  (contains? changed-systems-dir system))
          (print-entity "  " system changed-systems-dir))
        (doseq [{:keys [name type changed?]} infos]
          (print-entity "    " name type maxlength changed?))))))

(defn execute [ws-path top-dir args]
  (let [[_ _ timestamp] (time/parse-time-args ws-path args)
        data (info ws-path top-dir timestamp)]
    ;; todo: fix
    (print-info data)))
