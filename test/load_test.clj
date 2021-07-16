(ns load-test
  (:require [gatling-helpers :as gh]
            [clj-gatling.core :as g]
            [org.httpkit.client :as oh]
            [clojure.test :refer [deftest]]))

(def url "http://localhost:4444")

(defn request [& _args]
  (oh/get url
          (fn [response]
            (if (= 200 (:status response))
              true
              (do (gh/add-failure! url response)
                  false)))))

(def scenarios
  {:name      gh/performance-tests
   :scenarios [{:name  "First"
                :steps [{:name    "one"
                         :request request}]}]})

(def options
  {:concurrency 100
   :requests    10000
   :root        gh/report-root})

(def thresholds
  {gh/global-information
   {:failureRate                   0.1
    :meanNumberOfRequestsPerSecond 2000
    :meanResponseTime              100
    :maxResponseTime               200}})

(deftest load-test
  (g/run scenarios options)
  (gh/check-thresholds thresholds))
