(ns virtual-pmap.vpmap-test
  (:require [clojure.test :refer :all]
            [virtual-pmap.core :refer :all])
  (:import (java.util.concurrent ExecutionException)))


(deftest pmap-basic-functionality
  (testing "Basic single-function mapping"
    (is (= [2 3 4 5 6 7]
           (vpmap inc [1 2 3 4 5 6])))

    (is (= []
           (vpmap inc [])))

    (is (= [2]
           (vpmap inc [1]))))

  (testing "Order preservation"
    (is (= [1 2 3 4 5]
           (vpmap identity [1 2 3 4 5])))

    (let [nums (range 1000)]
      (is (= (vec nums)
             (vec (vpmap identity nums))))))

  (testing "Multiple collections"
    (is (= [3 5 7]
           (vpmap + [1 2 3] [2 3 4])))

    (is (= [6 9 12]
           (vpmap + [1 2 3] [2 3 4] [3 4 5])))))

(deftest pmap-error-handling
  (testing "Exception propagation"
    (is (thrown? ExecutionException
                 (doall (vpmap #(/ 1 %) [1 0 2]))))

    (is (thrown? ExecutionException
                 (doall (vpmap #(.toString %) [nil])))))

  (testing "Partial failure handling"
    (let [results (try
                    (doall (vpmap #(if (zero? %)
                                     (throw (Exception. "Zero!"))
                                     %)
                                  [1 0 2]))
                    (catch ExecutionException e
                      ::failed))]
      (is (= ::failed results)))))

(deftest performance
  (testing "Parallel execution"
    (let [sleep-time 10
          item-count 50
          sequential-start (System/currentTimeMillis)
          _ (doall
              (map #(do (Thread/sleep sleep-time) %)
                   (range item-count)))
          sequential-time (- (System/currentTimeMillis) sequential-start)
          _ (println "Sequential time:" sequential-time "ms")

          clj-pmap-start (System/currentTimeMillis)
          _ (doall
              (pmap #(do (Thread/sleep sleep-time) %)
                    (range item-count)))
          clj-pmap-time (- (System/currentTimeMillis) clj-pmap-start)
          _ (println "Clojure pmap time:" clj-pmap-time "ms")

          parallel-start (System/currentTimeMillis)
          _ (doall
              (vpmap #(do (Thread/sleep sleep-time) %)
                     (range item-count)))
          vparallel-time (- (System/currentTimeMillis) parallel-start)
          _ (println "Virtual Threads time:" vparallel-time "ms")]
      (is (< vparallel-time clj-pmap-time (* 0.5 sequential-time))))))


(deftest pmap-resource-management
  (testing "Resource cleanup"
    (let [run-vpmap #(doall (vpmap identity (range 1000)))]
      ;; Should be able to run multiple times without resource exhaustion
      (dotimes [_ 100]
        (run-vpmap))
      (is true "Completed without resource exhaustion")))

  (testing "Memory usage"
    (let [large-seq (range 1000000)]
      ;; Should not hold entire result set in memory
      (is (instance? clojure.lang.LazySeq
                     (vpmap identity large-seq))))))

(deftest pmap-edge-cases
  (testing "Nil handling"
    (is (= '()
                 (vpmap inc nil)))

    (is (= [nil nil nil]
           (vpmap identity [nil nil nil]))))

  (testing "Different sized collections"
    (is (= [2 4]
           (vpmap + [1 2] [1 2 3])))

    (is (= [3]
           (vpmap + [1] [1 2] [1 2 3]))))

  (testing "Type preservation"
    (is (every? integer?
                (vpmap inc [1 2 3])))

    (is (every? string?
                (vpmap #(str % "!") ["a" "b" "c"])))))

(deftest pmap-concurrent-behavior
  (testing "Concurrent modification of shared state"
    (let [counter (atom 0)]
      (doall
        (vpmap (fn [_] (swap! counter inc)) (range 100)))
      (is (= 100 @counter))))

  (testing "Thread-local state isolation"
    (let [results (vpmap (fn [x]
                          (let [thread-name (.getName (Thread/currentThread))]
                            [x thread-name]))
                        (range 10))
          thread-names (map second results)]
      ;; Virtual threads should have different names
      (is (> (count (set thread-names)) 1)))))
