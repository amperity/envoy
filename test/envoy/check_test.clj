(ns envoy.check-test
  (:require
    [clojure.test :refer :all]
    [envoy.check :refer :all])
  (:import
    clojure.lang.ExceptionInfo))


(defmacro behaved?
  [behavior setting expr]
  `(let [behaved# (atom nil)
         old-behave!# behave!]
     (with-redefs [behave!
                   (fn new-behave!#
                     [& args#]
                     (if (>= 3 (count args#))
                       (apply old-behave!# args#)
                       (do (reset! behaved# args#)
                           nil)))]
       ~expr
       (when (is @behaved# ~(str "Behavior " behavior " triggered"))
         (is ~behavior (first @behaved#))
         (is ~setting (second @behaved#))))))


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
    (set-behavior! :undeclared-access nil)
    (is (nil? (:undeclared-access behaviors))
        "value is nil after setting")
    (set-behavior! :undeclared-access :warn)
    (is (= :warn (:undeclared-access behaviors))
        "value is :warn after setting")))


(deftest behavior-checks
  (testing "lookup in map"
    (set-behavior! :undeclared-access nil)
    (is (nil? (behave! :undeclared-access "Undeclared access to %s" :foo))))
  (testing "nil"
    (is (nil? (behave! :test nil "Undeclared access to %s" :foo))))
  (testing ":warn"
    (is (nil? (behave! :test :warn "Undeclared access to %s" :foo))))
  (testing ":abort"
    (is (thrown-with-msg?
          RuntimeException #"Undeclared access to :foo"
          (behave! :test :abort "Undeclared access to %s" :foo)))))
