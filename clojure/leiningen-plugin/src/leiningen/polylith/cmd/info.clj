(ns leiningen.polylith.cmd.info
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.file :as file]))

(defn changed-dirs [dir file-paths]
  (let [f #(and (str/starts-with? % (str dir "/"))
                (> (count (str/split % #"/")) 2))]
    (vec (sort (set (map #(second (str/split % #"/"))
                         (filter f file-paths)))))))

(defn changed-system? [ws-path path changed-systems]
  (let [systems-path (str ws-path "/systems")
        system? (str/starts-with? path systems-path)
        changed? (and
                   system?
                   (let [system (second (str/split (subs path (count systems-path)) #"/"))]
                     (contains? (set changed-systems) system)))]
    {:system?  system?
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

(defn changed? [ws-path file changed-systems changed-components]
  (let [path (file/file-path->real-path file)
        changed-system (changed-system? ws-path path changed-systems)
        changed-component (changed-component? ws-path path changed-components)]
    {:name     (file/path->dir-name path)
     :type     (cond
                 (:system? changed-system) "-> system"
                 (:component? changed-component) "-> component"
                 :else "?")
     :changed? (cond
                 (:system? changed-system) (:changed? changed-system)
                 (:component? changed-component) (:changed? changed-component)
                 :else false)}))

(defn build-links [ws-path system changed-systems changed-components]
  (mapv #(changed? ws-path % changed-systems changed-components)
        (file/directories (str ws-path "/builds/" system "/src"))))

(defn build-info [ws-path builds changed-systems changed-components]
  (into {} (mapv (juxt identity #(build-links ws-path % changed-systems changed-components)) builds)))

(defn any-changes? [builds-info system]
  (or (some true? (map :changed? (builds-info system))) false))

(defn system-or-component-changed? [builds-info changed-builds]
  (let [system-changes (map (juxt identity #(any-changes? builds-info %)) (keys builds-info))]
    (map (juxt first #(or (last %) (contains? changed-builds (first %)))) system-changes)))

(defn all-builds [ws-path]
  (file/directory-names (str ws-path "/builds")))

(defn all-components [ws-path]
  (set (file/directory-names (str ws-path "/components"))))

(defn all-systems [ws-path]
  (set (file/directory-names (str ws-path "/systems"))))

(defn all-changed-build-dirs
  ([paths systems]
   (set (filter systems (changed-dirs "builds" paths)))))

(defn changed-apis
  ([ws-path paths]
   (changed-apis ws-path paths (all-systems ws-path)))
  ([ws-path paths systems]
   (set (filter systems (set (changed-dirs "apis" paths))))))

(defn changed-components
  ([ws-path paths]
   (changed-components ws-path paths (all-components ws-path)))
  ([ws-path paths components]
   (set (filter components (changed-dirs "components" paths)))))

(defn changed-systems
  ([ws-path paths]
   (changed-systems ws-path paths (all-systems ws-path)))
  ([ws-path paths systems]
   (set (filter systems (set (changed-dirs "systems" paths))))))

(defn changed-builds
  ([ws-path paths systems]
   (changed-builds (build-info ws-path
                               (all-builds ws-path)
                               (changed-systems ws-path paths)
                               (changed-components ws-path paths))
                   (all-changed-build-dirs paths systems)))
  ([builds-info changed-build-dirs]
   (mapv first (filter second (system-or-component-changed? builds-info (set changed-build-dirs))))))

(defn info
  ([ws-path]
   (info ws-path []))
  ([ws-path last-success-sha1 current-sha1]
   (info ws-path (diff/diff ws-path last-success-sha1 current-sha1)))
  ([ws-path paths]
   (let [apis (set (file/directory-names (str ws-path "/apis/src")))
         builds (file/directory-names (str ws-path "/builds"))
         components (all-components ws-path)
         systems (all-systems ws-path)
         ch-components (changed-components ws-path paths components)
         ch-systems (changed-systems ws-path paths systems)]
     {:apis               (-> apis sort vec)
      :builds             (-> builds sort vec)
      :components         (-> components sort vec)
      :systems            (-> systems sort vec)
      :diff               paths
      :changed-apis       (changed-apis ws-path paths systems)
      :changed-builds     changed-builds
      :changed-components ch-components
      :changed-systems    ch-systems
      :changed-builds-dir (all-changed-build-dirs paths systems)
      :builds-info        (build-info ws-path builds ch-systems ch-components)})))

(defn print-entity
  ([spaces entity changes show-changed? show-unchanged?]
   (let [changed? (contains? changes entity)
         star (if (and show-changed? changed?) " *" "")]
     (print-entity (str spaces entity star) changed? show-changed? show-unchanged?)))
  ([spaces entity type maxlength changed? show-changed? show-unchanged?]
   (let [star (if (and show-changed? changed?) " *" "")
         star-spaces (str/join (repeat (- maxlength (count (str entity star))) " "))
         string (str spaces entity star star-spaces type)]
     (print-entity string changed? show-changed?  show-unchanged?)))
  ([string changed? show-changed? show-unchanged?]
   (if (or (and changed? show-changed?)
           (and (not changed?) show-unchanged?))
     (println string))))

(defn print-info [{:keys [apis
                          components
                          systems
                          changed-apis
                          changed-systems
                          changed-components
                          changed-builds-dir
                          builds-info]}
                  show-changed?
                  show-unchanged?
                  show-apis?]
  (let [builds (keys builds-info)
        name-counts (map #(+ 3 (count (:name %)) (if (:changed? %) 2 0))
                         (filter #(or show-unchanged? (:changed? %))
                                 (mapcat second builds-info)))
        maxlength (if (empty? name-counts) 150 (apply max name-counts))]

    (when (or show-apis?
              (and show-unchanged? (not show-changed?))
              (and show-changed? (not show-unchanged?))
              (-> changed-apis empty? not))
      (println "apis:")
      (doseq [api apis]
        (print-entity "  " api changed-apis show-changed? show-unchanged?)))

    (println "components:")
    (doseq [component components]
      (print-entity "  " component changed-components show-changed? show-unchanged?))

    (println "systems:")
    (doseq [system systems]
      (print-entity "  " system changed-systems show-changed? show-unchanged?))

    (println "builds:")
    (doseq [build builds]
      (let [infos (sort-by :name
                           (filter #(or (and (:changed? %) show-changed?)
                                        (and (not (:changed? %)) show-unchanged?))
                                   (builds-info build)))]
        (when (or (-> infos empty? not)
                  (contains? changed-builds-dir build))
          (if show-changed?
            (print-entity "  " build changed-builds-dir true true)
            (println " " build)))
        (doseq [{:keys [name type changed?]} infos]
          (print-entity "    " name type maxlength changed? show-changed? show-unchanged?))))))

(defn execute [ws-path args]
  (let [cmd (first args)
        a? (= "a" cmd)
        filter? (= 1 (count cmd))
        [show-changed?
         show-unchanged?
         show-apis?] (if filter?
                       [(or a? (= "c" cmd))
                        (or a? (= "u" cmd))
                        a?]
                       [true true false])
        [last-success-sha1
         current-sha1] (if filter? (rest args) args)
        data (if (and last-success-sha1 current-sha1)
               (info ws-path last-success-sha1 current-sha1)
               (info ws-path))]
    (print-info data show-changed? show-unchanged? show-apis?)))
