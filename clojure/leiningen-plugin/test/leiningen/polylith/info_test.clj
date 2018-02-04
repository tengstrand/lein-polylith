(ns leiningen.polylith.info-test
  (:require [clojure.test :refer :all]))

(deftest x)


;bases ["backend" "commontest" "migration" "worker"]
; components ["authentication" "authorization" "base-data" "career-networks" "common" "db-delta" "email" "intercom-clj" "talent" "test-data-generator"]
; changed-bases #{}
; changed-components #{}
; changed-builds-dir #{}
; builds-info {"backend" [{:name "authentication", :type "-> component", :changed? false} {:name "authorization", :type "-> component", :changed? false} {:name "backend", :type "-> base", :changed? false} {:name "base-data", :type "-> component", :changed? false} {:name "career-networks", :type "-> component", :changed? false} {:name "common", :type "-> component", :changed? false} {:name "db-delta", :type "-> component", :changed? false} {:name "email", :type "-> component", :changed? false} {:name "intercom-clj", :type "-> component", :changed? false} {:name "talent", :type "-> component", :changed? false} {:name "test-data-generator", :type "-> component", :changed? false}], "worker" [{:name "common", :type "-> component", :changed? false} {:name "email", :type "-> component", :changed? false} {:name "intercom-clj", :type "-> component", :changed? false} {:name "talent", :type "-> component", :changed? false} {:name "worker", :type "-> base", :changed? false}]}
; show-unchanged? false
