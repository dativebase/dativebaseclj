(defproject old "0.1.0-SNAPSHOT"
  :description "Online Linguistic Database, in Clojure."
  :dependencies [[camel-snake-kebab "0.4.3"]
                 [cheshire "5.11.0"]
                 [clj-commons/clj-yaml "1.0.26"]
                 [com.layerware/hugsql "0.5.3"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [com.stuartsierra/component "1.1.0"]
                 [com.taoensso/timbre "6.1.0"]
                 [clj-http "3.12.3"]
                 [hikari-cp "3.0.1" :exclusions [org.slf4j/slf4j-api]]
                 [metosin/reitit "0.6.0"
                  :exclusions [com.fasterxml.jackson.core/jackson-core
                               org.clojure/core.rrb-vector]]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.mindrot/jbcrypt "0.4"]
                 [org.postgresql/postgresql "42.6.0"]
                 [org.slf4j/slf4j-api "1.7.36"]
                 [org.xerial/sqlite-jdbc "3.41.0.0"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-json "0.5.1"]]
  :main old.core
  :source-paths ["src"]
  :test-paths ["test"]
  :java-source-paths ["target/java"]
  :aliases {"test" ["run" "-m" "kaocha.runner"]
            "lint" ["run" "-m" "clj-kondo.main" "--lint" "."]
            "openapi" ["run" "-m" "old.http.openapi.serialize"]}
  :resource-paths ["resources"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-devel "1.9.6"]]}})
