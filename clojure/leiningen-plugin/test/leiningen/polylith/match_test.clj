(ns leiningen.polylith.match-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.match :as match]
            [clojure.string :as str]))

(deftest ignored-test->matching-patterns--asterisk
  (is (= ["user" "user."]
         (match/ignored-test->matching-patterns "user*"))))

(deftest ignored-test->matching-patterns--exact-match
  (is (= ["user.core" "."]
         (match/ignored-test->matching-patterns "user.core"))))

(deftest filter-tests--filter-components
  (is (= ["address.core"]
         (match/filter-tests ["user"
                              "user.work"
                              "user.hobby"
                              "address.core"]
                             ["user*"]))))

(deftest filter-tests--filter-components
  (is (= ["foo.bar.x"
          "foo.bar.x.y"
          "address.b"]
         (match/filter-tests ["user"
                              "user.work"
                              "user.work.x"
                              "foo.bar.x"
                              "foo.bar.x.y"
                              "foo.bar.z"
                              "foo.bar.z.z"
                              "address.a"
                              "address.b"
                              "address.c"]
                             ["user*"
                              "foo.bar.z*"
                              "address.a"
                              "address.c"]))))
