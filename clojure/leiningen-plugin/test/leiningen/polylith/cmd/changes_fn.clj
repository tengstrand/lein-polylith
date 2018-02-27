(ns leiningen.polylith.cmd.changes-fn
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.cmd.test-helper :refer [settings]]
            [clojure.string :as str]))

(defn changes-interface [ws-dir]
  (let [project (settings ws-dir "my.company" "my/company")
        output (with-out-str
                 (polylith/polylith nil "create" "w" "ws1" "my.company")
                 (polylith/polylith project "create" "c" "comp1")
                 (polylith/polylith project "create" "c" "comp2")
                 (polylith/polylith project
                                    "changes"
                                    "i"
                                    "cfd1ecc4aa6e6ca0646548aeabd22a4ee3b07419"
                                    "3014244d1be37651f33e22858b8ff0e8314b79f5"))
        components (set (map str/trim (str/split output #"\n")))]
    (is (= #{"comp2"}
           components))))

(defn changes-component [ws-dir]
  (let [project (settings ws-dir "my.company" "my/company")
        output (with-out-str
                 (polylith/polylith nil "create" "w" "ws1" "my.company")
                 (polylith/polylith project "create" "c" "comp1")
                 (polylith/polylith project "create" "c" "comp2")
                 (polylith/polylith project
                                    "changes"
                                    "c"
                                    "cfd1ecc4aa6e6ca0646548aeabd22a4ee3b07419"
                                    "3014244d1be37651f33e22858b8ff0e8314b79f5"))
        components (set (map str/trim (str/split output #"\n")))]
    (is (= #{"comp1" "comp2"}
           components))))
