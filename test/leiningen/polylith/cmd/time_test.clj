(ns leiningen.polylith.cmd.time-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.time :as time]))

(deftest milliseconds->minutes-and-seconds--less-than-a-minute--returns-seconds
  (is (= "7.3 seconds"
         (time/milliseconds->minutes-and-seconds 7345))))

(deftest milliseconds->minutes-and-seconds--more-then-a-minute--returns-minutes-and-seconds
  (is (= "2 minutes 7.3 seconds"
         (time/milliseconds->minutes-and-seconds 127345))))
