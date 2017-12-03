(ns leiningen.polylith.info
  (:require [leiningen.polylith.core :as core]
            [clojure.string :as str]))

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

(defn print-info [{:keys [systems
                          components
                          changed-systems
                          changed-components
                          changed-builds-dir
                          builds-info]}
                  show-changed?
                  show-unchanged?]
  (let [builds (keys builds-info)
        name-counts (map #(+ 3 (count (:name %)) (if (:changed? %) 2 0))
                         (filter #(or show-unchanged? (:changed? %))
                                 (mapcat second builds-info)))
        maxlength (if (empty? name-counts) 150 (apply max name-counts))]

    (println "systems:")
    (doseq [system systems]
      (print-entity "  " system changed-systems show-changed? show-unchanged?))

    (println "components:")
    (doseq [component components]
      (print-entity "  " component changed-components show-changed? show-unchanged?))

    (println "builds:")
    (doseq [build builds]
      (let [infos (filter #(or (and (:changed? %) show-changed?)
                              (and (not (:changed? %)) show-unchanged?))
                          (builds-info build))]
        (when (or (-> infos empty? not)
                  (contains? changed-builds-dir build))
          (if show-changed?
            (print-entity "  " build changed-builds-dir true true)
            (println " " build)))
        (doseq [{:keys [name type changed?]} infos]
          (print-entity "    " name type maxlength changed? show-changed? show-unchanged?))))))

