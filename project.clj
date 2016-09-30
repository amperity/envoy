(defproject amperity/envoy "0.2.0-SNAPSHOT"
  :description "Clojure environment configuration tracking."
  :url "https://github.com/amperity/envoy"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :deploy-branches ["master"]
  :pedantic? :abort

  :dependencies
  [[org.clojure/clojure "1.8.0" :scope "provided"]
   [org.clojure/tools.logging "0.3.1"]
   [environ "1.0.3"]
   [table "0.5.0"]])
