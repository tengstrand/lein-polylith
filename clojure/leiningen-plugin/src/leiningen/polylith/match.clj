(ns leiningen.polylith.match
  (:require [clojure.string :as str]))

(defn ignored-test->matching-patterns [ignore-test]
  (let [index (str/index-of ignore-test "*")]
    (if index
      (let [name (subs ignore-test 0 index)]
        [name (str name ".")])
      [ignore-test "."])))

(defn match? [test [exact-match starts-with]]
  (or
    (= test exact-match)
    (str/starts-with? test starts-with)))

(defn keep-test? [test ignore-tests]
  (let [patterns (map ignored-test->matching-patterns ignore-tests)]
    (empty? (filter true? (map #(match? test %) patterns)))))

(defn filter-tests [tests ignore-tests]
  (filter #(keep-test? % ignore-tests) tests))
