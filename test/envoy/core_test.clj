(ns envoy.core-test
  (:require
    [clojure.test :refer :all]
    [envoy.core :as env]))


(deftest definition-schemas
  (testing "behavior-types"
    (is (set? env/behavior-types))
    (is (every? (some-fn nil? keyword?) env/behavior-types)))
  (testing "type-parsing"
    (is (nil? (env/parse :foo nil))
        "nil values are not parsed")
    (testing "string"
      (is (= "foo" (env/parse :string "foo"))))
    (testing "keyword"
      (is (= :foo (env/parse :keyword :foo)))
      (is (= :foo (env/parse :keyword "foo")))
      (is (= :foo/bar (env/parse :keyword "foo/bar"))))
    (testing "boolean"
      (are [v] (true? (env/parse :boolean v))
           true "1" "t" "true" "y" "yes")
      (are [v] (false? (env/parse :boolean v))
           false "0" "f" "false" "n" "no")
      (is (false? (env/parse :boolean ""))
          "empty string parses as false")
      (is (true? (env/parse :boolean "foo"))
          "other non-empty strings parse as true"))
    (testing "integer"
      (is (== 0 (env/parse :integer 0)))
      (is (== 0 (env/parse :integer "0")))
      (is (== 12345 (env/parse :integer "12345")))
      (is (== -8502 (env/parse :integer "-8502"))))
    (testing "decimal"
      (is (== 0.0 (env/parse :decimal 0.0)))
      (is (== 123.456 (env/parse :decimal "123.456")))
      (is (== -85.0 (env/parse :decimal "-85"))))
    (testing "list"
      (is (= ["a" "b" "c"] (env/parse :list ["a" "b" "c"])))
      (is (= ["a" "b" "c"] (env/parse :list "a,b,c"))))))
