(ns envoy.tools
  "Build tools and tasks for checking the environment."
  (:require
    [clojure.java.io :as io]
    [clojure.set :as set]
    [environ.core :as environ]
    [envoy.check :refer [behave!]]
    [envoy.core :as envoy]
    [table.core :refer [table]]))


(defn- print-error
  "Gracefully handles a raised exception and writes messages to stderr. The
  `message` does not need to be newline delimited."
  [^Throwable ex message & format-args]
  (let [out ^java.io.Writer *err*]
    (.write out (str (apply format message format-args) "\n"))
    (when-let [reason (and ex (.getMessage ex))]
      (.write out (str reason "\n")))
    (when-let [data (ex-data ex)]
      (.write out (prn-str data)))
    (.flush out)))


(defn- load-namespaces
  "Requires a sequence of namespaces to load variable definitions."
  [namespaces]
  (doseq [code-ns namespaces]
    (try
      (printf "Loading namespace %s ...\n" code-ns)
      (require (symbol code-ns))
      (catch Exception ex
        (print-error ex "Failure loading namespace %s!" code-ns)
        (System/exit 3)))))


(defn lint-env-file
  "Inspects the configuration in the given env file (as written by `lein-environ`)
  and warns about undeclared variable definitions."
  [setting target]
  (when-let [config (@#'environ/read-env-file target)]
    (println "Linting environment configuration file" (str target))
    (when-let [unknown-vars (not-empty (set/difference
                                         (set (keys config))
                                         (set (keys envoy/known-vars))))]
      (behave! :undeclared-config setting
               "File %2$s configures undeclared env variables: %1$s"
               unknown-vars target))))


(defn- env-report-table
  []
  (when-not (empty? envoy/known-vars)
    (->>
      envoy/known-vars
      (sort-by (comp (juxt :ns :line) val))
      (map
        (fn [[var-name definition]]
          [var-name
           (name (:type definition :string))
           (str (:ns definition \?) \: (:line definition \?))
           (:description definition)]))
      (cons ["Name" "Type" "Declaration" "Description"]))))


(defn print-env-report
  "Prints out a table of all the known environment variables, their types, and definitions."
  []
  (if-let [rows (env-report-table)]
    (table rows :style :github-markdown)
    (println "No defined environment variables!")))


(defn -main
  "Runs the lint configuration check."
  [& [command & args]]
  (case command
    nil
    (do (print-error nil (str "Usage: lein run -m " *ns* " <lint|report> [args...]"))
        (System/exit 2))

    "lint"
    (do
      (load-namespaces args)
      (try
        (lint-env-file :abort ".lein-env")
        (lint-env-file :abort (io/resource ".boot-env"))
        (catch Exception ex
          (print-error ex "Configuration files have errors")
          (System/exit 1))))

    "report"
    (do (load-namespaces args)
        (print-env-report))

    (do (print-error nil (str "Unknown envoy command: " (pr-str command) "\n"))
        (System/exit 2)))
  (System/exit 0))
