(ns environ.sentry
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [clojure.set :as set]
    [clojure.string :as str]
    [environ.core :as environ]))


;; ## Value Parsing

(def type-parsers
  "Map of type keys to parsing functions."
  {:string  str
   :keyword keyword
   :boolean (fn parse-bool [x]
              (case (str/lower-case (str x))
                ("" "0" "f" "false" "n" "no") false
                true))
   :integer (fn parse-int [x] (Long/parseLong (str x)))
   :decimal (fn parse-dec [x] (Double/parseDouble (str x)))
   :list    (fn parse-list [x] (str/split x #","))})



;; ## Var Declaration

(def known-vars
  "Map of environment keywords to a definition map which may contain a
  `:description` and optionally a `:type` for auto-coercion."
  {})


(defn declare-env-var!
  "Helper function for the `defenv` macro."
  [env-key properties]
  (when-let [extant (get known-vars env-key)]
    (log/errorf "Environment variable definition for %s in %s:%d is overriding existing definition in %s:%d"
                env-key (:ns properties) (:line properties) (:ns extant) (:line extant)))
  (when-let [vtype (:type properties)]
    (when-not (contains? type-parsers vtype)
      (throw (IllegalArgumentException.
               (str "Environment variable " env-key " declares unsupported type "
                    vtype " not in (" (str/join " " (keys type-parsers) ")"))))))
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



;; ## Sentry Behavior

(def accesses
  "Atom containing access counts for all environment maps."
  (atom {} :validator #(and (map? %)
                            (every? keyword? (keys %))
                            (every? number? (vals %)))))


(def behavior
  "Definition for how the sentry should behave in various situations."
  {:undeclared-access :warn
   :undeclared-override :warn})


(defn set-behavior!
  "Set the behavior of the sentry in various situations."
  [& {:as opts}]
  (alter-var-root #'behavior merge opts))


(defn- behave!
  "Standard function for interpreting behavior settings."
  [behavior setting message var-key & format-args]
  (case setting
    nil    nil
    :warn  (log/warn (apply format message var-key format-args))
    :abort (throw (ex-info (apply format message var-key format-args)
                           {:type behavior, :var var-key}))
    (log/error "Unknown behavior type for" behavior (pr-str setting))))


(defn- on-access!
  "Called when a variable is accessed in the environment map with the key and
  original (string) config value. Returns the processed value."
  [k v]
  ; Update access counter.
  (swap! accesses update k (fnil inc 0))
  ; Look up variable definition.
  (if-let [definition (get known-vars k)]
    (if (some? v)
      ; Parse the value for known types.
      (if-let [parser (type-parsers (:type definition))]
        (parser v)
        v)
      ; Check if the var has missing behavior.
      (behave! ::missing-access (:missing definition)
               "Access to env variable %s which has no value" k))
    ; No definition found for key.
    (do
      (behave! ::undeclared-access (:undeclared-access behavior)
               "Access to undeclared env variable %s" k)
      v)))


(defn- on-override!
  "Called when a variable is overridden in the environment map with the key,
  old value, and new value. Returns the new value to use."
  [k v1 v2]
  ; Look up variable definition.
  (let [definition (get known-vars k)]
    (when-not definition
      (behave! ::undeclared-override (:undeclared-override behavior)
               "Overriding undeclared env variable %s" k))
    ; TODO: figure out how to render this based on type
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


(defn env-map
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
