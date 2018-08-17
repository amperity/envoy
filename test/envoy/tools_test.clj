(ns envoy.tools-test
  (:require
    [clojure.test :refer :all]
    [envoy.check-test :refer [behaved?]]
    [envoy.core :as envoy :refer [defenv]]
    [envoy.tools :as tools]))


(deftest lint-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"configures undeclared env variables: #\{:undeclared-var\}"
        (#'tools/lint-env-file :abort ".lein-env"))))


(deftest report-test
  (with-redefs [envoy/known-vars {}]

    (defenv :my-string "It's a string")
    (defenv :my-int "It's an int" :type :integer)

    (let [table (vec (#'tools/env-report-table))]
      (are [i j value] (= value (get-in table [i j]))
        1 0 :my-string, 1 1 "string", 1 3 "It's a string"
        2 0 :my-int, 2 1 "integer", 2 3 "It's an int")
      (are [i j pattern] (re-matches pattern (get-in table [i j]))
        1 2 #"envoy\.tools-test:\d+"
        2 2 #"envoy\.tools-test:\d+"))))
