(ns gatling-helpers
  "Helper functions to parse, analyse and automatically generate tests
  from gatling simulation results"
  (:require [clojure.string :as str]
            [clojure.test :refer [testing is]]
            [clojure.set :refer [subset?]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [medley.core :as medley]))

(def global-information "Global Information")
;; these two should maybe be customisable to avoid clashing
(def report-root "report")
(def performance-tests "perf")
(def nil-value "-")

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

(def rules
  ;; implement the missing rules
  {:maxResponseTime
   (fn [{:keys [maxResponseTime]}]
     (:ok maxResponseTime))

   :meanNumberOfRequestsPerSecond
   (fn [{:keys [meanNumberOfRequestsPerSecond]}]
     (:ok meanNumberOfRequestsPerSecond))

   :meanResponseTime
   (fn [{:keys [meanResponseTime]}]
     (:ok meanResponseTime))

   :successRate
   (fn [{:keys [numberOfRequests]}]
     (* 100 (float (/ (:ok numberOfRequests) (:total numberOfRequests)))))})

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

(defn check-thresholds [thresholds]
  {:pre [(subset? (set (mapcat keys (vals thresholds))) (set (keys rules)))]}
  ;; fetch the latest report and check the
  (let [results (last-report)]
    (doseq [[name res] results
            [path threshold] (get thresholds name)]
      (testing (str "Scenario: " name ", Step: " path)
        (let [f (get rules  path)]
          (is (<= threshold (f res))))))))
