(ns leiningen.polylith.info
  (:require [leiningen.polylith.core :as core]
            [clojure.string :as str]))

(defn print-entity
  ([spaces entity changes show-unchanged?]
   (let [changed? (contains? changes entity)
         changed (if changed? " *" "")]
     (print-entity (str spaces entity changed) changed? show-unchanged?)))
  ([string changed? show-unchanged?]
   (if (or changed? show-unchanged?)
     (println string))))

(defn print-info [{:keys [systems
                          components
                          changed-systems
                          changed-components
                          changed-builds-dir
                          builds-info]}
                  show-unchanged?]
  (let [builds (keys builds-info)
        maxlength (apply max
                         (map #(+ 2 (count (:name %)) (if (:changed? %) 2 0))
                              (filter #(or show-unchanged? (:changed? %))
                                      (mapcat second builds-info))))]

    (println "systems:")
    (doseq [system systems]
      (print-entity "  " system changed-systems show-unchanged?))

    (println "components:")
    (doseq [component components]
      (print-entity "  " component changed-components show-unchanged?))

    (println "builds:")
    (doseq [build builds]
      (print-entity "  " build changed-builds-dir true)
      (doseq [{:keys [name type changed?]} (builds-info build)]
        (let [star (if changed? " *" "")
              spaces (str/join (repeat (- maxlength (count (str name star))) " "))
              string (str "    " name star spaces type)]
          (print-entity string changed? show-unchanged?))))))
