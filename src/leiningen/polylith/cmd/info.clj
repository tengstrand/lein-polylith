(ns leiningen.polylith.cmd.info
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn changed-dirs [dir file-paths]
  (let [n (count (str/split dir #"/"))
        nidx #(nth % n)
        f #(and (str/starts-with? % (str dir "/"))
                (> (count (str/split % #"/")) 2))]
    (vec (sort (set (map #(shared/entity-src-dir-name (nidx (str/split % #"/")))
                         (filter f file-paths)))))))

(defn changed-base? [ws-path path changed-bases changed-entities-by-ref]
  (let [bases-path (str ws-path "/bases")
        base? (str/starts-with? path bases-path)
        base (second (str/split (subs path (count bases-path)) #"/"))
        [changed? changed-by-ref?] [(and base? (contains? (set changed-bases) base))
                                    (and base? (contains? changed-entities-by-ref base))]]
    {:base? base?
     :changed? changed?
     :changed-by-ref? changed-by-ref?}))

(defn changed-component? [ws-path path changed-components changed-entities-by-ref]
  (let [components-path (str ws-path "/components")
        component? (str/starts-with? path components-path)
        component (second (str/split (subs path (count components-path)) #"/"))
        [changed? changed-by-ref?] [(and component? (contains? (set changed-components) component))
                                    (and component? (contains? changed-entities-by-ref component))]]
    {:component? component?
     :changed? changed?
     :changed-by-ref? changed-by-ref?}))

(defn changed? [ws-path file changed-bases changed-components changed-entities-by-ref]
  (let [path (file/file->real-path file)
        changed-base (changed-base? ws-path path changed-bases changed-entities-by-ref)
        changed-component (changed-component? ws-path path changed-components changed-entities-by-ref)]
    {:name (shared/link->entity ws-path (file/file path))
     :type (cond
             (:base? changed-base) "-> base"
             (:component? changed-component) "-> component"
             :else "?")
     :changed? (cond
                 (:base? changed-base) (:changed? changed-base)
                 (:component? changed-component) (:changed? changed-component)
                 :else false)
     :changed-by-ref? (cond
                        (:base? changed-base) (:changed-by-ref? changed-base)
                        (:component? changed-component) (:changed-by-ref? changed-component)
                        :else false)}))

(defn keep? [entity-path bases interfaces]
  (let [entity (file/path->dir-name entity-path)]
    (or (contains? bases entity)
        (contains? interfaces entity))))

(defn system-links [ws-path top-dir system bases interfaces changed-bases changed-components changed-entities-by-ref]
  (let [dir (if (zero? (count top-dir)) "/src" (str "/src/" top-dir))
        entity-paths (vec (file/directories (str ws-path "/systems/" system dir)))]
    (mapv #(changed? ws-path % changed-bases changed-components changed-entities-by-ref)
          (filter #(keep? % bases interfaces) entity-paths))))

(defn systems-info [ws-path top-dir systems bases interfaces changed-bases changed-components changed-entities-by-ref]
  (into {} (mapv (juxt identity #(system-links ws-path top-dir % bases interfaces changed-bases changed-components changed-entities-by-ref)) systems)))

(defn any-changes? [systems-info system]
  (or (some true? (map :changed? (systems-info system))) false))

(defn base-or-component-changed? [systems-info changed-systems]
  (let [base-changes (map (juxt identity #(any-changes? systems-info %)) (keys systems-info))]
    (map (juxt first #(or (last %) (contains? changed-systems (first %)))) base-changes)))

(defn system-entity [path]
  (let [parts (str/split path #"/")]
    (when (and
            (= "systems" (first parts))
            (or (< (count parts) 3)
                (not= "src" (nth parts 2))))
      (second parts))))

(defn all-changed-systems-dir [paths]
  (set (filter identity (map system-entity paths))))

(defn changed-interfaces
  ([ws-path top-dir paths]
   (changed-interfaces ws-path top-dir paths (shared/all-interfaces ws-path top-dir)))
  ([_ top-dir paths interfaces]
   (set (filter interfaces (changed-dirs (shared/interfaces-src-dir top-dir) paths)))))

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

(defn indirect-entity-changes [entity disallowed-deps all-deps changed-entities]
  (if (contains? changed-entities entity)
    [false]
    (let [deps (set (all-deps entity))]
      (if (empty? deps)
        [false]
        (if (empty? (set/intersection deps disallowed-deps))
          (if (some (comp not nil?) (map changed-entities deps))
            [true]
            (let [values (mapv #(indirect-entity-changes % (conj disallowed-deps %) all-deps changed-entities) deps)]
              [(true? (some true? (map first values)))]))
          [false "recursive dependencies"])))))

(defn environment-links [ws-path top-dir environment changed-bases changed-components changed-entities-by-ref]
  (let [dir (str ws-path "/environments/" environment "/src/" (shared/full-name top-dir "/" ""))]
    (sort-by :name
             (mapv #(changed? ws-path % changed-bases changed-components changed-entities-by-ref)
                   (file/directories dir)))))

(defn environments-info [ws-path top-dir environments changed-bases changed-components changed-entities-by-ref]
  (into {} (mapv (juxt identity #(environment-links ws-path top-dir % changed-bases changed-components changed-entities-by-ref)) environments)))

(defn ->changed [[entity [changed]]]
  [entity changed])

(defn environment-deps [ws-path top-dir changed-entities [_ infos]]
  (let [entities     (set (map :name infos))
        dependencies (deps/component-dependencies ws-path top-dir)
        changes-info (mapv #(vector % (indirect-entity-changes % #{%} dependencies changed-entities)) entities)]
    (into {} (map ->changed changes-info))))

(defn environments-deps [ws-path top-dir changed-entities env-infos]
  (let [deps (map #(environment-deps ws-path top-dir changed-entities %)
                  env-infos)]
    (set (map first (into {} (mapv #(filter second %) deps))))))

(defn all-indirect-changes [ws-path top-dir paths]
  (let [systems (shared/all-systems ws-path)
        environments (shared/all-environments ws-path)
        bases (shared/all-bases ws-path)
        interfaces (shared/all-interfaces ws-path top-dir)
        components (shared/all-components ws-path)
        ch-bases (changed-bases nil paths bases)
        ch-components (changed-components nil paths components)
        changed-entities (set (concat ch-bases ch-components))
        infos (systems-info ws-path top-dir systems bases interfaces ch-bases ch-components #{})
        envs (environments-info ws-path top-dir environments ch-bases ch-components #{})]
    (set (concat (environments-deps ws-path top-dir changed-entities envs)
                 (environments-deps ws-path top-dir changed-entities infos)))))

(defn info->circular-deps [dependencies {:keys [name]}]
  [name (deps/circular-comp-deps name dependencies)])

(defn keep-existing-components [[k v] entities]
  [k (filter entities v)])

(defn env->circular-deps [ws-path top-dir [name infos]]
  (let [entities (set (map :name infos))
        dependencies (into {} (map #(keep-existing-components % entities)
                                   (deps/component-dependencies ws-path top-dir)))]
    {name (into {} (filter second (map #(info->circular-deps dependencies %) infos)))}))

(defn envs->circular-deps [ws-path top-dir environment]
  (into {} (map #(env->circular-deps ws-path top-dir %) environment)))

(defn circular-dependencies [ws-path top-dir]
  (let [systems (shared/all-systems ws-path)
        environments (shared/all-environments ws-path)
        bases (shared/all-bases ws-path)
        interfaces (shared/all-interfaces ws-path top-dir)
        sinfos (systems-info ws-path top-dir systems bases interfaces #{} #{} #{})
        einfos (environments-info ws-path top-dir environments #{} #{} #{})
        res1 (envs->circular-deps ws-path top-dir sinfos)
        res2 (envs->circular-deps ws-path top-dir einfos)
        result {:systems      res1
                :environments res2}]
    result))

(defn has-circular-dependencies? [ws-path top-dir]
  (let [{:keys [systems environments]} (circular-dependencies ws-path top-dir)
        system-values (vals systems)
        environment-values (vals environments)]
    (not (and (every? empty? environment-values)
              (every? empty? system-values)))))

(defn changed-systems
  ([ws-path top-dir paths]
   (changed-systems (systems-info ws-path
                                  top-dir
                                  (shared/all-systems ws-path)
                                  (shared/all-bases ws-path)
                                  (shared/all-interfaces ws-path top-dir)
                                  (changed-bases ws-path paths)
                                  (changed-components ws-path paths)
                                  (all-indirect-changes ws-path top-dir paths))
                    (all-changed-systems-dir paths)))
  ([systems-info changed-system-dirs]
   (mapv first (filter second (base-or-component-changed? systems-info (set changed-system-dirs))))))

(defn info [ws-path top-dir args]
  (let [paths (diff/changed-file-paths ws-path args)
        interfaces (shared/all-interfaces ws-path top-dir)
        systems (shared/all-systems ws-path)
        components (shared/all-components ws-path)
        bases (shared/all-bases ws-path)
        environments (shared/all-environments ws-path)
        ch-interfaces (changed-interfaces ws-path top-dir paths interfaces)
        ch-systems (changed-systems ws-path top-dir paths)
        ch-components (changed-components nil paths components)
        ch-entities-by-ref (all-indirect-changes ws-path top-dir paths)
        ch-bases (changed-bases nil paths bases)]
    {:interfaces (-> interfaces sort vec)
     :systems (-> systems sort vec)
     :components (-> components sort vec)
     :bases (-> bases sort vec)
     :diff paths
     :changed-interfaces ch-interfaces
     :changed-systems ch-systems
     :changed-components ch-components
     :changed-bases ch-bases
     :changed-systems-dir (all-changed-systems-dir paths)
     :changed-entities-by-ref ch-entities-by-ref
     :circular-dependencies (circular-dependencies ws-path top-dir)
     :systems-info (systems-info ws-path top-dir systems bases interfaces ch-bases ch-components ch-entities-by-ref)
     :environments-info (environments-info ws-path top-dir environments ch-bases ch-components ch-entities-by-ref)}))

(defn print-entity
  ([spaces entity changes]
   (let [changed? (contains? changes entity)
         star (if changed? " *" "")]
     (println (str spaces entity star))))
  ([spaces entity type maxlength changed? changed-by-ref? cyclic-deps]
   (let [star (if changed? " *" (if changed-by-ref? " (*)" ""))
         star-spaces (str/join (repeat (- maxlength (count (str entity star))) " "))
         cyclic (if (str/blank? cyclic-deps) "" (str (if (= type "-> component") "" "     ")
                                                     "  (circular deps: " cyclic-deps ")"))
         string (str spaces entity star star-spaces type cyclic)]
     (println string))))

(defn max-length [entities]
  (let [name-counts (map #(+ 3 (count (:name %))
                             (if (:changed-by-ref? %)
                               3
                               (if (:changed? %)
                                 2
                                 0)))
                         (mapcat second entities))]
    (if (empty? name-counts)
      150
      (apply max name-counts))))

(defn entity-length [entity changed-entities]
  (let [length (count entity)]
    (if (contains? changed-entities entity)
      (+ length 2)
      length)))

(defn entities-max-length [entities changed-entities]
  (if (empty? entities)
    150
    (apply max (mapv #(entity-length % changed-entities) entities))))

(def type->sort {"-> interface" 1
                 "-> component" 2
                 "-> base"      3})

(defn info-sorting [{:keys [name type]}]
  (str (type->sort type) name))

(defn print-info [{:keys [interfaces
                          components
                          bases
                          changed-interfaces
                          changed-bases
                          changed-components
                          changed-entities-by-ref
                          changed-systems-dir
                          systems-info
                          environments-info
                          circular-dependencies]}
                  component->interface]
  (let [systems (-> systems-info keys sort)
        comp-max-length (entities-max-length components changed-components)
        base-max-length (entities-max-length bases changed-bases)
        systems-max-length (max-length systems-info)
        environments-maxlength (max-length environments-info)
        cyclic-systems (circular-dependencies :systems)
        cyclic-environments (circular-dependencies :environments)]

    (println "interfaces:")
    (doseq [interface interfaces]
      (print-entity "  " interface changed-interfaces))

    (println "components:")
    (doseq [component components]
      (let [interface (component->interface component)
            changed? (contains? changed-components component)
            indirectly-changed? (contains? changed-entities-by-ref component)]
        (print-entity "  " component interface comp-max-length changed? indirectly-changed? "")))

    (println "bases:")
    (doseq [base bases]
      (let [changed? (contains? changed-bases base)
            indirectly-changed? (contains? changed-entities-by-ref base)]
        (print-entity "  " base "" base-max-length changed? indirectly-changed? "")))

    (println "systems:")
    (doseq [system systems]
      (let [cyclic-deps (cyclic-systems system)
            infos (sort-by info-sorting (systems-info system))]
        (when (or (-> infos empty? not)
                  (contains? changed-systems-dir system))
          (print-entity "  " system changed-systems-dir))
        (doseq [{:keys [name type changed? changed-by-ref?]} infos]
          (print-entity "    " name type systems-max-length changed? changed-by-ref? (cyclic-deps name)))))

    (println "environments:")
    (doseq [[name info-data] environments-info]
      (let [cyclic-deps (cyclic-environments name)
            info (sort-by info-sorting info-data)]
        (println " " name)
        (doseq [{:keys [name type changed? changed-by-ref?]} info]
          (when (or (contains? (set components) name)
                    (contains? (set bases) name))
            (print-entity "    " name type environments-maxlength changed? changed-by-ref? (cyclic-deps name))))))))

(defn component-interface [ws-path top-dir component]
  (let [interface (shared/interface-of ws-path top-dir component)]
    (if (= component interface)
      [component ""]
      [component (str "   > " interface)])))

(defn execute [ws-path top-dir args]
  (let [data (info ws-path top-dir args)
        component->interface (into {} (map #(component-interface ws-path top-dir %) (data :components)))]
    (print-info data component->interface)))
