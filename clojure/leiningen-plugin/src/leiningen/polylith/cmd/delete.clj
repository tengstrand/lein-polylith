(ns leiningen.polylith.cmd.delete
  (:require [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]))

(defn validate [ws-path top-dir cmd name]
  (cond
    (not (= "c" cmd)) [false "Illegal first argument."]
    (utils/is-empty-str? top-dir) [false "Missing top-dir."]
    (nil? name) [false "Missing name."])
  :else [true])

(defn delete-component [ws-path top-dir dev-dirs name]
  (let [top-name (if (zero? (count top-dir)) name (str top-dir "/" name))]
    (file/delete-dir (str ws-path "/apis/src/" top-name))
    (file/delete-dir (str ws-path "/components/" name))
    (doseq [dir dev-dirs]
      (file/delete-file (str ws-path "/" dir "/project-files/" name "-project.clj"))
      (file/delete-file (str ws-path "/" dir "/resources/" name))
      (file/delete-file (str ws-path "/" dir "/src/" top-name))
      (file/delete-file (str ws-path "/" dir "/test/" top-name))
      (file/delete-file (str ws-path "/" dir "/test-int/" top-name)))))

(defn execute [ws-path top-dir top-ns dev-dirs [cmd name]]
  (let [[ok? msg] (validate ws-path top-dir cmd name)]
    (if ok?
      (condp = cmd
        "c" (delete-component ws-path top-dir dev-dirs name))
      (do
        (println msg)
        (help/delete)))))
