(ns leiningen.polylith.cmd.create.interface
  (:require [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.file :as file]))

(defn create-interface [ws-path top-dir top-ns interface]
  (let [ns-name (shared/full-name top-ns "." interface)
        proj-dir (shared/full-name top-dir "/" interface)
        content [(str "(ns " ns-name ".interface)")
                 ""
                 ";; add your function signatures here..."
                 "(defn myfn [x])"]]
    (shared/create-src-dirs! ws-path "interfaces/src" [proj-dir])
    (file/create-file (str ws-path "/interfaces/src/" proj-dir "/interface.clj") content)))
