(ns gatling-helpers-test
  (:require [gatling-helpers :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest transform-test
  (is (= {"Global Information"
          {:standardDeviation {:total 9.0, :ok 9.0, :ko 0},
           :percentiles1 {:total 36.0, :ok 36.0, :ko 0},
           :maxResponseTime {:total 56.0, :ok 56.0, :ko 0},
           :group2 {:name "800 ms < t < 1200 ms", :count 0, :percentage 0},
           :group1 {:name "t < 800 ms", :count 100, :percentage 100},
           :meanNumberOfRequestsPerSecond {:total 1695.0, :ok 1695.0, :ko 0},
           :meanResponseTime {:total 23.0, :ok 23.0, :ko 0},
           :percentiles2 {:total 52.0, :ok 52.0, :ko 0},
           :group4 {:name "failed", :count 0, :percentage 0},
           :numberOfRequests {:total 100.0, :ok 100.0, :ko 0.0},
           :minResponseTime {:total 3.0, :ok 3.0, :ko 0},
           :group3 {:name "t > 1200 ms", :count 0, :percentage 0}},
          "one"
          {:standardDeviation {:total 9.0, :ok 9.0, :ko 0},
           :percentiles1 {:total 36.0, :ok 36.0, :ko 0},
           :maxResponseTime {:total 56.0, :ok 56.0, :ko 0},
           :group2 {:name "800 ms < t < 1200 ms", :count 0, :percentage 0},
           :group1 {:name "t < 800 ms", :count 100, :percentage 100},
           :meanNumberOfRequestsPerSecond {:total 1695.0, :ok 1695.0, :ko 0},
           :meanResponseTime {:total 23.0, :ok 23.0, :ko 0},
           :percentiles2 {:total 52.0, :ok 52.0, :ko 0},
           :group4 {:name "failed", :count 0, :percentage 0},
           :numberOfRequests {:total 100.0, :ok 100.0, :ko 0.0},
           :minResponseTime {:total 3.0, :ok 3.0, :ko 0},
           :group3 {:name "t > 1200 ms", :count 0, :percentage 0}}}
         (sut/parse-report (slurp "test-resources/stats.js")))))
