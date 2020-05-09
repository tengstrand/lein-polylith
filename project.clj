(defproject polylith/lein-polylith "0.2.3"
  :description "Polylith - a component based architecture, by Joakim Tengstrand."
  :url "https://github.com/tengstrand/lein-polylith"
  :license {:name "Eclipse Public License",
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git", :url "https://github.com/tengstrand/lein-polylith"}
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo/",
                                    :username      :env/clojars_username,
                                    :password      :env/clojars_password,
                                    :sign-releases false}]]
  :dependencies [[clojure-future-spec "1.9.0-alpha17"]
                 [zprint "0.4.15"]
                 [org.freemarker/freemarker "2.3.28"]]
  :eval-in-leiningen true)
