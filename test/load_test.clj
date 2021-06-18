(ns load-test
  (:require [gatling-helpers :as gh]
            [clj-gatling.core :as g]
            [org.httpkit.client :as oh]
            [clojure.test :refer [deftest]]))

(def url "http://localhost:4444")

(defn request [& _args]
  (= 200 (:status @(oh/get url))))

(def scenarios
  {:name      gh/performance-tests
   :scenarios [{:name  "First"
                :steps [{:name    "one"
                         :request request}]}]})

(def options
  {:concurrency 100
   :root        gh/report-root})

(def thresholds
  {"Global Information"
   {:meanNumberOfRequestsPerSecond 1000}})

(deftest load-test
  (g/run scenarios options))
