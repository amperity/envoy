(ns envoy.core-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer [deftest is]]
    [envoy.core :as env :refer [defenv]]))


(defenv :envoy-test
  "Test environment variable."
  :type :integer)


(defenv :envoy-bad-type
  "Variable with a bad type."
  :type :foo)


(defenv :envoy-ex-schema
  "Variable with attribute not in default schema."
  :secret true)


(defenv :envoy-bad-type
  "Re-declaring a variable.")


(defenv :envoy-custom-parser
  "Specifying a special parsing fn."
  :parser edn/read-string)


(defenv :envoy-boolean
  "Testing booleans for truthy/falsey gotchas."
  :type :boolean)


(defenv :envoy-default
  "Variable with default value."
  :type :decimal
  :default 1.2)


(deftest env-declaration
  (is (contains? env/known-vars :envoy-test))
  (is (= "Test environment variable." (get-in env/known-vars [:envoy-test :description])))
  (is (= :integer (get-in env/known-vars [:envoy-test :type])))
  (is (= 'envoy.core-test (get-in env/known-vars [:envoy-test :ns]))))


(deftest parser-declaration
  (do
    (env/set-env! :envoy-custom-parser "{:a 1}")
    (is (= {:a 1} (env/env :envoy-custom-parser))))
  (do
    (env/set-env! :envoy-custom-parser "{:a {:b [\"1\" 2.0 \\3 \" \"]}}")
    (is (= {:a {:b ["1" 2.0 \3 " "]}} (env/env :envoy-custom-parser)))))


(deftest boolean-parsing
  (is (nil? (env/env :envoy-boolean)))
  (env/set-env! :envoy-boolean true)
  (is (true? (env/env :envoy-boolean)))
  (env/set-env! :envoy-boolean false)
  (is (false? (env/env :envoy-boolean)))
  (env/set-env! :envoy-boolean "true")
  (is (true? (env/env :envoy-boolean)))
  (env/set-env! :envoy-boolean "false")
  (is (false? (env/env :envoy-boolean))))


(deftest default-value-handling
  (is (= 1.2 (env/env :envoy-default)))
  (env/set-env! :envoy-default 1.3)
  (is (= 1.3 (env/env :envoy-default)))
  (env/set-env! :envoy-default nil)
  (is (= 1.2 (env/env :envoy-default))))
