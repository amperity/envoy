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
  "Global default environment map as loaded by `environ.core`."
  (env-map environ/env))
