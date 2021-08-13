(ns gatling-helpers
  "Helper functions to parse, analyse and automatically generate tests
  from gatling simulation results"
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.set :refer [subset?]]
            [clojure.string :as str]
            [clojure.test :refer [testing is]]
            [clojure.core.async :as async]
            [medley.core :as medley]))

(def global-information "Global Information")
;; these two should maybe be customisable to avoid clashing
(def report-root "report")
(def performance-tests "perf")
(def nil-value "-")
(def failures (atom #{}))

(defn add-failure! [url {:keys [status error]}]
  (let [failure (medley/filter-vals some?
                                    {:url url
                                     :status status
                                     :error error})]
    (swap! failures conj failure)))

(defn parse-val [d]
  (let [f #(if (= nil-value %)
             0
             (try (Float/parseFloat %)
                  (catch Exception _e
                    %)))]
    (medley/map-vals f d)))

(defn transform-report [report]
  (medley.core/map-vals
   (fn [g]
     (medley.core/map-vals
      parse-val
      (dissoc g :name)))
   report))

(defn parse-report [raw-js]
  (->> (str/split raw-js #"stats: ")
       (drop 1)
       (map #(str/replace % #",\w+\"group1\".*", "}"))
       (map #(json/parse-string % keyword))
       (group-by :name)
       (reduce-kv (fn [res name [r]]
                    (assoc res name r))
                  {})
       transform-report))

;; TODO: instead of defining rules this way we could use a multimethod
;; which would make it easier to extend from anywhere ideally
(def rules
  ;; implement the missing rules
  {:maxResponseTime
   (fn [v {:keys [maxResponseTime]}]
     (<= (:ok maxResponseTime) v))

   :failureRate
   (fn [v {:keys [numberOfRequests]}]
     (<= (* 100 (float (/ (:ko numberOfRequests) (:total numberOfRequests)))) v))

   :meanResponseTime
   (fn [v {:keys [meanResponseTime]}]
     (<= (:ok meanResponseTime) v))

   :meanNumberOfRequestsPerSecond
   (fn [v {:keys [meanNumberOfRequestsPerSecond]}]
     (>= (:ok meanNumberOfRequestsPerSecond) v))})


;; TODO: this is just relying on alphabetical ordering, which also coincidentally gets the latest report
;; would be better to just be more explicit
(defn find-latest-report []
  (->> (io/file report-root)
       file-seq
       (filter #(.isDirectory %))
       (map #(.getName %))
       (filter #(.startsWith % performance-tests))
       sort
       last))

(defn fetch-last-report [report-path]
  (let [stats-js-path (str report-path "/js/stats.js")
        js-report (slurp stats-js-path)]
    (parse-report js-report)))

(defn last-report []
  (-> (format "%s/%s" report-root (find-latest-report))
      fetch-last-report))

(defn check-thresholds [report thresholds]
  {:pre [(subset? (set (mapcat keys (vals thresholds))) (set (keys rules)))]}
  ;; fetch the latest report and generate assertions
  (doseq [[name res]       report
          [path threshold] (get thresholds name)]
    (testing (str "Scenario: " name ", Step: " path)
      (let [f       (get rules path)
            passed? (f threshold res)]
        (is passed? (str "Threshold: " threshold
                         "\nValue: " res))))))
