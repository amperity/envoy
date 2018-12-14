(ns envoy.core-test
  (:require
    [clojure.test :refer :all]
    [envoy.check-test :refer [behaved?]]
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
  :parser clojure.edn/read-string)

(defenv :envoy-abort
  "Setting abort behavior."
  :missing :abort)

(defenv :envoy-warn
  "Setting warn behavior."
  :missing :warn)

(defenv :envoy-boolean
  "Testing booleans for truthy/falsey gotchas."
  :type :boolean)


(deftest env-declaration
  (is (contains? env/known-vars :envoy-test))
  (is (= "Test environment variable." (get-in env/known-vars [:envoy-test :description])))
  (is (= :integer (get-in env/known-vars [:envoy-test :type])))
  (is (= 'envoy.core-test (get-in env/known-vars [:envoy-test :ns]))))


(deftest parsed-values
  (do
    (env/set-env! :envoy-test "1")
    (is (= 1 (env/env :envoy-test))))
  (do
    (env/set-env! :envoy-custom-parser "{:a 1}")
    (is (= {:a 1} (env/env :envoy-custom-parser))))
  (do
    (env/set-env! :envoy-custom-parser "{:a {:b [\"1\" 2.0 \\3 \" \"]}}")
    (is (= {:a {:b ["1" 2.0 \3 " "]}} (env/env :envoy-custom-parser)))))


(deftest test-behaviors
  (behaved? :missing-access :abort (env/env :envoy-abort))
  (behaved? :missing-access :warn (env/env :envoy-warn))
  (behaved? :undeclared-access :warn (env/env :never-got-defined))
  (behaved? :undeclared-override :warn (env/set-env! :never-got-defined 1)))


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
