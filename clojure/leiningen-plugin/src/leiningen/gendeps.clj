(ns leiningen.gendeps
  (:require [clojure.pprint :as pprint]))

(defn ^:no-project-needed gendeps [project & keys]
  "Generate dependencies"
  (if (seq keys)
    (doseq [kstr keys]
      (let [k (read-string kstr)]
        (pprint/pprint (if (sequential? k)
                         (get-in project k)
                         (get project k)))))
    (pprint/pprint project))
  (flush))
