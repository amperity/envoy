(ns envoy.types-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [envoy.types :as types]))


(deftest type-parsing
  (testing "edge cases"
    (is (nil? (types/parse :foo nil))
        "nil values are not parsed")
    (is (thrown-with-msg?
          RuntimeException #"Cannot parse non-string value"
          (types/parse :string 'xyz)))
    (is (thrown-with-msg?
          RuntimeException #"Cannot parse value without parsing function"
          (types/parse :bar "abcd"))))
  (testing "string"
    (is (= "foo" (types/parse :string "foo"))))
  (testing "keyword"
    (is (= :foo (types/parse :keyword :foo)))
    (is (= :foo (types/parse :keyword "foo")))
    (is (= :foo/bar (types/parse :keyword "foo/bar"))))
  (testing "boolean"
    (are [v] (true? (types/parse :boolean v))
      true "1" "t" "true" "y" "yes")
    (are [v] (false? (types/parse :boolean v))
      false "0" "f" "false" "n" "no")
    (is (false? (types/parse :boolean ""))
        "empty string parses as false")
    (is (true? (types/parse :boolean "foo"))
        "other non-empty strings parse as true"))
  (testing "integer"
    (is (== 0 (types/parse :integer 0)))
    (is (== 0 (types/parse :integer "0")))
    (is (== 12345 (types/parse :integer "12345")))
    (is (== -8502 (types/parse :integer "-8502"))))
  (testing "decimal"
    (is (== 0.0 (types/parse :decimal 0.0)))
    (is (== 123.456 (types/parse :decimal "123.456")))
    (is (== -85.0 (types/parse :decimal "-85"))))
  (testing "list"
    (is (= ["a" "b" "c"] (types/parse :list ["a" "b" "c"])))
    (is (= ["a" "b" "c"] (types/parse :list "a,b,c")))))
