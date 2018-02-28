(ns leiningen.polylith.cmd.delete
  (:require [leiningen.polylith.cmd.help :as help]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.utils :as utils]))

(defn validate [ws-path top-dir cmd name]
  (cond
    (not (= "c" cmd)) [false "Illegal first argument."]
    (utils/is-empty-str? top-dir) [false "Missing top-dir."]
    (nil? name) [false "Missing name."]
    :else [true]))

(defn delete-component [ws-path top-dir name]
  (let [top-name (if (zero? (count top-dir)) name (str top-dir "/" name))
        dev-dirs (file/directory-names (str ws-path "/environments"))
        env-path (str ws-path "/environments/")]
    (file/delete-dir (str ws-path "/interfaces/src/" top-name))
    (file/delete-dir (str ws-path "/components/" name))
    (doseq [dir dev-dirs]
      (file/delete-file (str env-path dir "/project-files/components/" name "-project.clj"))
      (file/delete-file (str env-path dir "/resources/" name))
      (file/delete-file (str env-path dir "/src/" top-name))
      (file/delete-file (str env-path dir "/test/" top-name)))))

(defn execute [ws-path top-dir top-ns [cmd name]]
  (let [[ok? msg] (validate ws-path top-dir cmd name)]
    (if ok?
      (condp = cmd
        "c" (delete-component ws-path top-dir name))
      (do
        (println msg)
        (help/delete)))))
