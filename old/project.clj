(defproject old "0.1.0-SNAPSHOT"
  :description "Online Linguistic Database, in Clojure."
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.11.0"]
                 [clj-commons/clj-yaml "1.0.26"]
                 [com.stuartsierra/component "1.1.0"]
                 [metosin/reitit "0.6.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.41.0.0"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-json "0.5.1"]]
  :main old.core
  :source-paths ["src"]
  :java-source-paths ["target/java"]
  :aliases {"test" ["run" "-m" "kaocha.runner"]
            "lint" ["run" "-m" "clj-kondo.main" "--lint" "."]
            "openapi" ["run" "-m" "old.http.openapi.serialize"]}
  :resource-paths ["resources"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-devel "1.9.6"]]}})
