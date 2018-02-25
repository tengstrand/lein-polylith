(ns leiningen.polylith-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.create-workspace-fn :as cmd-ws]))

(def root-dir (atom nil))

(defn test-setup-and-tear-down [f]
  (let [path (str (file/temp-dir) "polylith-root")]
    (if (file/create-dir path)
      (reset! root-dir path)
      (throw (Exception. (str "Could not create directory: " path))))
    (f)
    (file/delete-dir path)))

(use-fixtures :each test-setup-and-tear-down)

(defn call-test [test-fn]
  (with-redefs [file/current-path (fn [] @root-dir)]
    (test-fn (str @root-dir "/ws1"))))

(deftest create-workspace
  (call-test cmd-ws/create-workspace))
