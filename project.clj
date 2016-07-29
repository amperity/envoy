(defproject amperity/environ-sentry "0.1.0-SNAPSHOT"
  :description "Environment configuration in Clojure."
  :url "https://github.com/amperity/environ-sentry"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :deploy-branches ["master"]
  :pedantic? :abort

  :dependencies
  [[org.clojure/clojure "1.8.0" :scope "provided"]
   [org.clojure/tools.logging "0.3.1"]
   [environ "1.0.3"]])
