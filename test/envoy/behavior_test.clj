(ns envoy.behavior-test
  (:require
    [clojure.test :refer :all]
    [envoy.behavior :refer :all]))


(deftest behavior-types-var
  (is (set? behavior-types))
  (is (every? (some-fn nil? keyword?) behavior-types)))


(deftest behavior-setting
  (testing "bad arguments"
    (is (thrown-with-msg?
          IllegalArgumentException #"Invalid behavior settings: :foo"
          (set-behavior! :foo :abort)))
    (is (thrown-with-msg?
          IllegalArgumentException #"Invalid behavior types: :bar"
          (set-behavior! :undeclared-access :bar))))
  (testing "setting logic"
    (set-behavior! :undeclared-access :abort)
    (is (= :abort (:undeclared-access behaviors))
        "value is :abort after setting")
    (set-behavior! :undeclared-access :warn)
    (is (= :warn (:undeclared-access behaviors))
        "value is :warn after setting")))


; TODO: test behave!
