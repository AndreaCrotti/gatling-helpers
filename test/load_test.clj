(ns load-test
  (:require [gatling-helpers :as gh]
            [clj-gatling.core :as g]
            [org.httpkit.client :as oh]
            [clojure.core.async :as async]
            [clojure.test :refer [deftest]]))

(def url "http://localhost:4444")

(defn sample-request []
  ;; understand how to do async requests properly
  (async/go
    (oh/get "http://google.co.uk")))

;;FIXME: this would make gatling think it's always working need to use
;;core.async instead
(defn request [& _args]
  #_(let [ch (async/chan)]
      (oh/get url
              (fn [response]
                (if (= 200 (:status response))
                  (async/<! true)
                  (do (gh/add-failure! url response)
                      false))))
      (let [response (async/<! (oh/get url))]
        (if (= 200 (:status response))
          true
          (do (gh/add-failure! url response)
              false))))
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
   :requests    1000000
   :root        gh/report-root})

(def thresholds
  {gh/global-information
   {:failureRate                   0.01
    :meanNumberOfRequestsPerSecond 2000
    :meanResponseTime              30
    :maxResponseTime               100}})

(deftest load-test
  (g/run scenarios options)
  (gh/check-thresholds (gh/last-report) thresholds))
