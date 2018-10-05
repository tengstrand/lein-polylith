(ns leiningen.polylith
  (:require [clojure.stacktrace :as stacktrace]
            [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.commands :as commands]
            [leiningen.polylith.cmd.shared :as shared]))

(defn ^:no-project-needed polylith
  "Helps you develop component based systems"
  ([_]
   (help/execute [] false))
  ([project command & args]
   (let [{:keys [ok? system-error? exception]} (apply commands/execute project command args)]
     (when (not ok?)
       (if system-error?
         (stacktrace/print-stack-trace exception)
         (shared/print-error-message exception))
       (System/exit 1)))))
