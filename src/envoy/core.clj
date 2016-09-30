(ns envoy.core
  "Core environment handling namespace."
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [environ.core :as environ]))


;; ## Definition Schema

(def behavior-types
  "Set of valid values for behavior types."
  #{nil :warn :abort})


(def value-types
  "Set of valid value type keys."
  #{:string :keyword :boolean
    :integer :decimal :list})


(def variable-schema
  "Simple key->predicate schema for variable definitions."
  {:ns symbol?
   :line number?
   :description string?
   :type value-types
   :missing behavior-types})



;; ## Var Declaration

(def known-vars
  "Map of environment keywords to a definition map which may contain a
  `:description` and optionally a `:type` for auto-coercion."
  {})


(defn- declared-location
  "Returns a string naming the location an env variable was declared."
  [definition]
  (let [ns-sym (:ns definition)
        ns-line (:line definition)]
    (some-> ns-sym (cond-> ns-line (str ":" ns-line)))))


(defn declare-env-var!
  "Helper function for the `defenv` macro. Declares properties for an
  environment variable, checking various schema attributes."
  [env-key properties]
  (when-let [extant (get known-vars env-key)]
    (log/errorf "Environment variable definition for %s at %s is overriding existing definition from %s"
                env-key (declared-location properties) (declared-location extant)))
  (doseq [[prop-key prop-val] properties]
    (if-let [pred (variable-schema prop-key)]
      ; Check value against schema predicate.
      (when-not (pred prop-val)
        (log/warnf "Environment variable %s (%s) declares invalid value %s for property %s (failed %s)"
                   env-key (declared-location properties) (pr-str prop-val)
                   prop-key pred))
      ; Declaring a property not in the schema.
      (log/debugf "Environment variable %s (%s:%s) declares ex-schema property %s")))
  (-> #'known-vars
      (alter-var-root assoc env-key properties)
      (get env-key)))


(defmacro defenv
  "Define a new environment variable used by the system."
  [env-key description & {:as opts}]
  `(declare-env-var!
     ~env-key
     (assoc ~opts
            :ns '~(symbol (str *ns*))
            :line ~(:line (meta &form))
            :description ~description)))



;; ## Type Handling

(def falsey-strings
  "Set of strings which are considered 'falsey' values for a boolean
  environment variable. Strings are downcased before checking this set."
  #{"" "0" "f" "false" "n" "no"})


(def ^:private type-predicates
  "Map of type keys to predicate functions which test whether a value satisfies
  the given type."
  {:string string?
   :keyword keyword?
   :boolean (some-fn true? false?)
   :integer integer?
   :decimal float?
   :list sequential?})


(def ^:private type-parsers
  {:string  str
   :keyword keyword
   :boolean (comp not falsey-strings str/lower-case str)
   :integer #(Long/parseLong %)
   :decimal #(Double/parseDouble %)
   :list    #(str/split % #",")})


(defn parse
  "Parse a value based on its type."
  [type-key value]
  {:pre [(keyword? type-key)]}
  (when (some? value)
    (let [tester (type-predicates type-key)
          parser (type-parsers type-key)]
      (cond
        ; Value already has the right type.
        (and tester (tester value))
          value
        ; Value is not a string, so we can't parse it.
        (not (string? value))
          (throw (ex-info (str "Cannot parse non-string value to " (name type-key))
                          {:type type-key, :value value}))
        ; Parse value with parsing function.
        parser
          (parser value)
        ; No reasonable approach, so throw an error.
        :else
          (throw (ex-info (str "Cannot parse value without parsing function for " (name type-key))
                          {:type type-key, :value value}))))))



;; ## Access Behavior

(def accesses
  "Atom containing access counts for all environment maps."
  (atom {} :validator #(and (map? %)
                            (every? keyword? (keys %))
                            (every? number? (vals %)))))


(def behaviors
  "Definition for how the library should behave in various situations."
  {:undeclared-access :warn
   :undeclared-override :warn
   :undeclared-config :abort})


(defn set-behavior!
  "Set the behavior of the library in various situations."
  [& {:as opts}]
  {:pre [(every? behaviors (keys opts))
         (every? behavior-types (vals opts))]}
  (alter-var-root #'behaviors merge opts))


(defn ^:no-doc behave!
  "Standard function for interpreting behavior settings."
  ([behavior message var-key]
   (behave!
     behavior
     (behaviors (keyword (name behavior)))
     var-key
     message))
  ([behavior setting message var-key & format-args]
   (case setting
     nil    nil
     :warn  (log/warnf (apply format message var-key format-args))
     :abort (throw (ex-info (apply format message var-key format-args)
                            {:type behavior, :var var-key}))
     (log/errorf "Unknown behavior type for %s %s"
                 behavior (pr-str setting)))))


(defn- on-access!
  "Called when a variable is accessed in the environment map with the key and
  original (string) config value. Returns the processed value."
  [k v]
  ; Update access counter.
  (swap! accesses update k (fnil inc 0))
  ; Look up variable definition.
  (if-let [definition (get known-vars k)]
    (if (some? v)
      ; Parse the string value for known types.
      (if-let [type-key (:type definition)]
        (parse type-key v)
        v)
      ; Check if the var has missing behavior.
      (behave! ::missing-access (:missing definition)
               "Access to env variable %s which has no value" k))
    ; No definition found for key.
    (do
      (behave! ::undeclared-access "Access to undeclared env variable %s" k)
      v)))


(defn- on-override!
  "Called when a variable is overridden in the environment map with the key,
  old value, and new value. Returns the new value to use."
  [k v1 v2]
  ; Look up variable definition.
  (let [definition (get known-vars k)]
    (when-not definition
      (behave! ::undeclared-override "Overriding undeclared env variable %s" k))
    v2))



;; ## Environment Map

(deftype EnvironmentMap
  [config _meta]

  java.lang.Object

  (toString
    [this]
    (str "EnvironmentMap " config))

  (equals
    [this that]
    (boolean
      (or (identical? this that)
          (when (identical? (class this) (class that))
            (= config (.config ^EnvironmentMap that))))))

  (hashCode
    [this]
    (hash [(class this) config]))


  clojure.lang.IObj

  (meta
    [this]
    _meta)

  (withMeta
    [this meta-map]
    (EnvironmentMap. config meta-map))


  clojure.lang.IFn

  (invoke
    [this k]
    (.valAt this k))

  (invoke
    [this k not-found]
    (.valAt this k not-found))


  clojure.lang.ILookup

  (valAt
    [this k]
    (.valAt this k nil))

  (valAt
    [this k not-found]
    (on-access! k (get config k not-found)))


  clojure.lang.IPersistentMap

  (count
    [this]
    (count config))

  (empty
    [this]
    (EnvironmentMap. (empty config) _meta))

  (cons
    [this element]
    (EnvironmentMap. (cons config element) _meta))

  (equiv
    [this that]
    (.equals this that))

  (containsKey
    [this k]
    (contains? config k))

  (entryAt
    [this k]
    (let [v (.valAt this k this)]
      (when-not (identical? this v)
        (clojure.lang.MapEntry. k v))))

  (seq
    [this]
    (seq config))

  (iterator
    [this]
    (clojure.lang.RT/iter (seq config)))

  (assoc
    [this k v]
    (EnvironmentMap.
      (update config k #(on-override! k % v))
      _meta))

  (without
    [this k]
    (on-override! k (get config k) nil)
    (EnvironmentMap. (dissoc config k) _meta)))


(defmethod print-method EnvironmentMap
  [v ^java.io.Writer w]
  (.write w (str v)))


;; Remove automatic constructor function.
(ns-unmap *ns* '->EnvironmentMap)


(defn ^:no-doc env-map
  "Constructs a new environment map."
  ([]
   (env-map {}))
  ([config]
   {:pre [(map? config)
          (every? keyword? (keys config))
          (every? string? (vals config))]}
   (EnvironmentMap. config nil)))


(def env
  "Global environment map as loaded by `environ.core`."
  (env-map environ/env))



;; ## Build Commands

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
  [target]
  (when-let [config (@#'environ/read-env-file target)]
    (println "Linting environment configuration file" (str target))
    (when-let [unknown-vars (not-empty (set/difference
                                         (set (keys config))
                                         (set (keys known-vars))))]
      (behave! ::undeclared-config
               "File %2$s configures undeclared env variables: %1$s"
               unknown-vars target))))


(defn print-env-report
  "Prints out a table of all the known environment variables, their types, and definitions."
  []
  (if (empty? known-vars)
    (println "No defined environment variables!")
    (do
      (println "| Name | Type | Declaration | Description |")
      (println "| ---- | ---- | ----------- | ----------- |")
      (doseq [[var-name definition] (sort-by (comp (juxt :ns :line) val)
                                             known-vars)]
        (printf "| %s | %s | %s:%s | %s |\n"
                var-name (name (:type definition :string))
                (:ns definition "?") (:line definition "?")
                (:description definition))))))


(defn -main
  "Runs the lint configuration check."
  [& [command & args]]
  (case command
    nil
    (do (print-error nil "Usage: lein run -m environ.sentry <lint|report> [args...]")
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

    (do (print-error nil (str "Unknown environ-sentry command: "
                              (pr-str command) "\n"))
        (System/exit 2))))
