(ns envoy.types
  "Value types and parsing code."
  (:require
    [clojure.string :as str]))


(def value-types
  "Set of valid value type keys."
  #{:string :keyword :boolean
    :integer :decimal :list})


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
  "Map of type keys to parsing functions."
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
