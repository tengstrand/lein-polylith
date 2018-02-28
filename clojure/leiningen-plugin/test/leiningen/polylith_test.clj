(ns leiningen.polylith-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith :as polylith]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.cmd.create-workspace-fn :as cmd-ws]
            [leiningen.polylith.cmd.create-component-fn :as cmd-component]
            [leiningen.polylith.cmd.changes-fn :as cmd-changes]
            [leiningen.polylith.cmd.delete-fn :as cmd-delete]))

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
  (with-redefs [file/current-path (fn [] @root-dir)
                leiningen.polylith.cmd.diff/diff (fn [_ _ _] helper/diff)
                ;; todo: replace this when we can create bases.
                leiningen.polylith.cmd.info/all-bases (fn [_] #{"base1" "base2"})]
    (test-fn (str @root-dir "/ws1"))))

(deftest create-workspace-with-ns
  (call-test cmd-ws/create-workspace-with-ns))

(deftest create-workspace-without-ns
  (call-test cmd-ws/create-workspace-without-ns))

(deftest create-component-with-ns
  (call-test cmd-component/create-component-with-ns))

(deftest create-component-without-ns
  (call-test cmd-component/create-component-without-ns))

(deftest changes-interface
  (call-test cmd-changes/changes-interface))

(deftest changes-component
  (call-test cmd-changes/changes-component))

(deftest changes-base
  (call-test cmd-changes/changes-base))

(deftest delete-component-with-ns
  (call-test cmd-delete/delete-component-with-ns))

(deftest delete-component-without-ns
  (call-test cmd-delete/delete-component-without-ns))
