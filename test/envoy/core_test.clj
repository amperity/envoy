(ns envoy.core-test
  (:require
    [clojure.test :refer :all]
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

(ns envoy.core-test.special)
(def parser clojure.string/lower-case)

(ns envoy.core-test)

(defenv :envoy-custom-ns-parser
  "Specifying a special parsing fn."
  :parser envoy.core-test.special/parser)


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
    (is (= {:a {:b ["1" 2.0 \3 " "]}} (env/env :envoy-custom-parser))))
  (do
    (env/set-env! :envoy-custom-ns-parser "APascalCase")
    (is (= "apascalcase" (env/env :envoy-custom-ns-parser)))))
