(ns leiningen.polylith
  (:require [leiningen.help :as help]
            [leiningen.polylith.core :as core]))

(defn ^:no-project-needed polylith
  "Manage a Polylith based project"
  {:help-arglists '([print])
   :subtasks [#'print]}
  ([project]
   (println (help/help-for "polylith")))
  ([project subtask & args]
   (case subtask
     "print" (core/print-dependencies)
     (println "Subtask" (str \" subtask \") "not found."
              (help/subtask-help-for *ns* #'polylith)))))
