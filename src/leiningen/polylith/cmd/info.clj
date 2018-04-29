(ns leiningen.polylith.cmd.info
  (:require [clojure.string :as str]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.time :as time]
            [leiningen.polylith.cmd.shared :as shared]))

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
    {:base?    base?
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

(defn systems-info [ws-path top-dir systems changed-bases changed-components]
  (into {} (mapv (juxt identity #(system-links ws-path top-dir % changed-bases changed-components)) systems)))

(defn any-changes? [systems-info system]
  (or (some true? (map :changed? (systems-info system))) false))

(defn base-or-component-changed? [systems-info changed-systems]
  (let [base-changes (map (juxt identity #(any-changes? systems-info %)) (keys systems-info))]
    (map (juxt first #(or (last %) (contains? changed-systems (first %)))) base-changes)))

(defn all-changed-systems-dir [paths bases]
  (set (filter bases (changed-dirs "systems" paths))))

(defn all-environments [ws-path]
  (sort (file/directory-names (str ws-path "/environments"))))

(defn changed-interfaces
  ([ws-path paths top-dir]
   (changed-interfaces paths (shared/all-interfaces ws-path top-dir)))
  ([paths interfaces]
   ;; todo: also check "interfaces/test".
   (set (filter interfaces (changed-dirs "interfaces/src" paths)))))

(defn changed-components
  ([ws-path paths]
   (changed-components nil paths (shared/all-components ws-path)))
  ([_ paths components]
   (set (filter components (changed-dirs "components" paths)))))

(defn changed-bases
  ([ws-path paths]
   (changed-bases nil paths (shared/all-bases ws-path)))
  ([_ paths bases]
   (set (filter bases (set (changed-dirs "bases" paths))))))

(defn changed-systems
  ([ws-path paths top-dir bases]
   (changed-systems (systems-info ws-path
                                  top-dir
                                  (shared/all-systems ws-path)
                                  (changed-bases ws-path paths)
                                  (changed-components ws-path paths))
                    (all-changed-systems-dir paths bases)))
  ([systems-info changed-system-dirs]
   (mapv first (filter second (base-or-component-changed? systems-info (set changed-system-dirs))))))

(defn environment-links [ws-path top-dir environment changed-bases changed-components]
  (let [dir (str ws-path "/environments/" environment "/src/" (shared/full-name top-dir "/" ""))]
    (sort-by :name
      (mapv #(changed? ws-path % changed-bases changed-components)
            (file/directories dir)))))

(defn environments-info [ws-path top-dir environments changed-bases changed-components]
  (into {} (mapv (juxt identity #(environment-links ws-path top-dir % changed-bases changed-components)) environments)))

(defn info [ws-path top-dir timestamp]
   (let [paths (mapv second (diff/do-diff ws-path timestamp))
         interfaces (shared/all-interfaces ws-path top-dir)
         systems (shared/all-systems ws-path)
         components (shared/all-components ws-path)
         bases (shared/all-bases ws-path)
         environments (all-environments ws-path)
         ch-interfaces (changed-interfaces paths interfaces)
         ch-systems (changed-systems ws-path paths top-dir bases)
         ch-components (changed-components nil paths components)
         ch-bases (changed-bases nil paths bases)]
     {:interfaces          (-> interfaces sort vec)
      :systems             (-> systems sort vec)
      :components          (-> components sort vec)
      :bases               (-> bases sort vec)
      :diff                paths
      :changed-interfaces  ch-interfaces
      :changed-systems     ch-systems
      :changed-components  ch-components
      :changed-bases       ch-bases
      :changed-systems-dir (all-changed-systems-dir paths bases)
      :systems-info        (systems-info ws-path top-dir systems ch-bases ch-components)
      :environments-info   (environments-info ws-path top-dir environments ch-bases ch-components)}))

(defn print-entity
  ([spaces entity changes]
   (let [changed? (contains? changes entity)
         star (if changed? " *" "")]
     (println (str spaces entity star))))
  ([spaces entity type maxlength changed?]
   (let [star (if changed? " *" "")
         star-spaces (str/join (repeat (- maxlength (count (str entity star))) " "))
         string (str spaces entity star star-spaces type)]
     (println string))))

(defn max-length [entities]
  (let [name-counts (map #(+ 3 (count (:name %)) (if (:changed? %) 2 0))
                         (mapcat second entities))]
    (if (empty? name-counts)
      150
      (apply max name-counts))))

(def type->sort {"-> interface" 1
                 "-> component" 2
                 "-> base" 3})

(defn info-sorting [{:keys [name type]}]
  (str (type->sort type) name))

(defn print-info [{:keys [interfaces
                          components
                          bases
                          changed-interfaces
                          changed-bases
                          changed-components
                          changed-systems-dir
                          systems-info
                          environments-info]}
                  component->interface]
  (let [systems (-> systems-info keys sort)
        components-max-length (apply max (mapv count components))
        systems-max-length (max-length systems-info)
        environments-maxlength (max-length environments-info)]

    (println "interfaces:")
    (doseq [interface interfaces]
      (print-entity "  " interface changed-interfaces))

    (println "components:")
    (doseq [component components]
      (let [interface (component->interface component)
            changed? (contains? changed-components component)]
        (print-entity "  " component interface components-max-length changed?)))

    (println "bases:")
    (doseq [base bases]
      (print-entity "  " base changed-bases))

    (println "systems:")
    (doseq [system systems]
      (let [infos (sort-by info-sorting (systems-info system))]
        (when (or (-> infos empty? not)
                  (contains? changed-systems-dir system))
          (print-entity "  " system changed-systems-dir))
        (doseq [{:keys [name type changed?]} infos]
          (print-entity "    " name type systems-max-length changed?))))

    (println "environments:")
    (doseq [[name info-data] environments-info]
      (let [info (sort-by info-sorting info-data)]
        (println " " name)
        (doseq [{:keys [name type changed?]} info]
          (when (or (contains? (set components) name)
                    (contains? (set bases) name))
            (print-entity "    " name type environments-maxlength changed?)))))))

(defn component-interface [ws-path top-dir component]
  (let [interface (shared/interface-of ws-path top-dir component)]
    (if (= component interface)
      [component ""]
      [component (str "  > " interface)])))

(defn execute [ws-path top-dir args]
  (let [[_ timestamp] (time/parse-time-args ws-path args)
        data (info ws-path top-dir timestamp)
        component->interface (into {} (map #(component-interface ws-path top-dir %) (data :components)))]
    (print-info data component->interface)))
