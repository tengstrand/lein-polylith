(ns leiningen.polylith.utils
  (:require [clojure.string :as str]))

(defn is-empty-str? [value]
  (or (nil? value) (str/blank? value)))
