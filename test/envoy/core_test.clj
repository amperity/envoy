(ns envoy.core-test
  (:require
    [clojure.test :refer :all]
    [envoy.core :as env]))


(env/defenv :envoy-test
  "Test environment variable."
  :type :integer)


(deftest env-declaration
  (is (contains? env/known-vars :envoy-test))
  (is (= "Test environment variable." (get-in env/known-vars [:envoy-test :description])))
  (is (= :integer (get-in env/known-vars [:envoy-test :type])))
  (is (= 'envoy.core-test (get-in env/known-vars [:envoy-test :ns]))))
