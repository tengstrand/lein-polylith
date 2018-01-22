(defproject polylith/lein-polylith "0.0.27-alpha"
  :description "Polylith - a tool for building component based architectures, by Joakim Tengstrand."
  :url "https://github.com/tengstrand/polylith"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/tengstrand/polylith"}
  :eval-in-leiningen true
  :pom-addition [:developers [:developer
                              [:name "Joakim Tengstrand"]
                              [:email "joakim.tengstrand@gmail.com"]
                              [:timezone "+1"]]]
  :deploy-repositories {"releases" {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                                    :creds :gpg}
                        "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/"
                                     :creds :gpg}})
