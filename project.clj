(defproject amperity/envoy "0.3.3-SNAPSHOT"
  :description "Clojure environment configuration tracking."
  :url "https://github.com/amperity/envoy"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :aliases
  {"coverage" ["with-profile" "+test,+coverage" "cloverage"]}

  :deploy-branches ["master"]
  :pedantic? :abort

  :plugins
  [[lein-codox "0.9.5"]
   [lein-cloverage "1.0.9"]]

  :dependencies
  [[org.clojure/clojure "1.9.0" :scope "provided"]
   [org.clojure/tools.logging "0.4.0"]
   [environ "1.1.0"]
   [table "0.5.0"]]

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/amperity/envoy/blob/master/{filepath}#L{line}"
   :doc-paths [""]
   :output-path "doc/api"}

  :profiles
  {:test
   {:dependencies [[commons-logging "1.2"]]
    :jvm-opts ["-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.NoOpLog"]}

   :coverage
   {:jvm-opts ["-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog"
               "-Dorg.apache.commons.logging.simplelog.defaultlog=trace"]}})
