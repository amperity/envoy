(ns envoy.core
  "Core environment handling namespace."
  (:require
    [clojure.tools.logging :as log]
    [environ.core :as environ]
    [envoy.check :as check :refer [behave!]]
    [envoy.types :as types]))


(def known-vars
  "Map of environment keywords to a definition map which may contain a
  `:description` and optionally a `:type` for auto-coercion."
  {})


(def variable-schema
  "Simple key->predicate schema for variable definitions."
  {:ns symbol?
   :line number?
   :description string?
   :type types/value-types
   :parser fn?
   :default any?
   :missing check/behavior-types})


;; ## Var Declaration

(defn- declared-location
  "Returns a string naming the location an env variable was declared."
  [properties]
  (let [ns-sym (:ns properties)
        ns-line (:line properties)]
    (some-> ns-sym (cond-> ns-line (str ":" ns-line)))))


(defn- validate-attr!
  "Checks whether the given variable property matches the declared schema. No
  effect if no schema is found. Returns true if the value is valid."
  [env-key prop-key properties]
  (if-let [pred (and (contains? properties prop-key)
                     (variable-schema prop-key))]
    (let [value (get properties prop-key)]
      (if (pred value)
        true
        (log/warnf
          "Environment variable %s (%s) declares invalid value %s for property %s (failed %s)"
          env-key (declared-location properties) (pr-str value) prop-key pred)))
    true))


(defn declare-env-attr!
  "Helper function which adds elements to the `variable-schema` map."
  [prop-key pred]
  ;; Check existing variables.
  (doseq [[env-key properties] known-vars]
    (validate-attr! env-key prop-key properties))
  ;; Update schema map.
  (alter-var-root #'variable-schema assoc prop-key pred))


(defn declare-env-var!
  "Helper function for the `defenv` macro. Declares properties for an
  environment variable, checking various schema properties."
  [env-key properties]
  ;; Check for previous declarations.
  (when-let [extant (get known-vars env-key)]
    (when (not= (:ns extant) (:ns properties))
      (log/warnf "Environment variable definition for %s at %s is overriding existing definition from %s"
                 env-key
                 (declared-location properties)
                 (declared-location extant))))
  ;; Check property schemas.
  (doseq [prop-key (keys properties)]
    (validate-attr! env-key prop-key properties))
  ;; Update known variables map.
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


;; ## Access Behavior

(def accesses
  "Atom containing access counts for all environment maps."
  (atom {} :validator #(and (map? %)
                            (every? keyword? (keys %))
                            (every? number? (vals %)))))


(defn clear-accesses!
  "Resets the variable accesses map to an empty state."
  []
  (swap! accesses empty))


(defn- on-access!
  "Called when a variable is accessed in the environment map with the key and
  original (string) config value. Returns the processed value."
  [k v]
  ;; Update access counter.
  (swap! accesses update k (fnil inc 0))
  ;; Look up variable definition.
  (if-let [definition (get known-vars k)]
    (if (some? v)
      ;; Parse the string value with custom parser or for known types.
      (if-let [parser (:parser definition)]
        (parser v)
        (if-let [type-key (:type definition)]
          (types/parse type-key v)
          v))
      (if-let [default (:default definition)]
        (do
          (log/infof "Environment variable %s has no value, using default '%s'"
                     k default)
          default)
        ;; Check if the var has missing behavior.
        (behave! :missing-access (:missing definition)
                 "Access to env variable %s which has no value" k)))
    ;; No definition found for key.
    (do
      (behave! :undeclared-access "Access to undeclared env variable %s" k)
      v)))


(defn- on-override!
  "Called when a variable is overridden in the environment map with the key,
  old value, and new value. Returns the new value to use."
  [k v1 v2]
  ;; Look up variable definition.
  (let [definition (get known-vars k)]
    (when-not definition
      (behave! :undeclared-override "Overriding undeclared env variable %s" k))
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


  (applyTo
    [this args]
    (case (count args)
      1 (.valAt this (first args))
      2 (.valAt this (first args) (second args))
      (throw (clojure.lang.ArityException. (count args) "EnvironmentMap.applyTo"))))


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
    (EnvironmentMap. (conj config element) _meta))


  (equiv
    [this that]
    (.equals this that))


  (containsKey
    [this k]
    (contains? config k))


  (entryAt
    [this k]
    (when-some [v (.valAt this k)]
      (clojure.lang.MapEntry. k v)))


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


(defn set-env!
  "Updates the global environment map with a new value for the given variable.
  This should generally only be used from a REPL, and will not affect the actual
  system environment!"
  [var-key value & kvs]
  (alter-var-root #'env #(apply assoc % var-key value kvs))
  nil)
