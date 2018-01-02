(ns leiningen.polylith.match
  (:require [clojure.string :as str]))

(defn ignored-test->matching-patterns [ignored-test]
  (let [index (str/index-of ignored-test "*")]
    (if index
      (let [name (subs ignored-test 0 index)]
        [name (str name ".")])
      [ignored-test "."])))

(defn match? [test [exact-match starts-with]]
  (or
    (= test exact-match)
    (str/starts-with? test starts-with)))

(defn keep-test? [test ignored-tests]
  (let [patterns (map ignored-test->matching-patterns ignored-tests)]
    (empty? (filter true? (map #(match? test %) patterns)))))

(defn filter-tests [tests ignored-tests]
  (filter #(keep-test? % ignored-tests) tests))
