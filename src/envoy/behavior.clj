(ns envoy.behavior
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]))


(def behavior-types
  "Set of valid values for behavior types."
  #{nil :warn :abort})


(def behaviors
  "Definition for how the library should behave in various situations."
  {:undeclared-access :warn
   :undeclared-override :warn
   :undeclared-config :abort})


(defn set-behavior!
  "Set the behavior of the library in various situations."
  [& {:as settings}]
  (when-let [bad-settings (seq (remove (set (keys behaviors)) (keys settings)))]
    (throw (IllegalArgumentException.
             (str "Invalid behavior settings: " (str/join " " bad-settings)))))
  (when-let [bad-types (seq (remove behavior-types (vals settings)))]
    (throw (IllegalArgumentException.
             (str "Invalid behavior types: " (str/join " " bad-types)))))
  (alter-var-root #'behaviors merge settings))


(defn behave!
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
