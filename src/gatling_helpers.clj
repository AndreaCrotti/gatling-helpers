(ns gatling-helpers
  (:require [clojure.string :as str]
            [clojure.test :refer [testing is]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [medley.core :as medley]))

(def report-root "report")
(def performance-tests "perf")

(defn to-double [d]
  (let [f #(try (Float/parseFloat %)
                (catch Exception _e
                  %))]
    (medley/map-vals f d)))

(defn transform-to-double [report]
  (medley.core/map-vals
   (fn [g]
     (medley.core/map-vals
      to-double
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
       transform-to-double))

(def rules
  {:maxResponseTime (fn [])
   :meanNumberOfRequestsPerSecond (fn [{:keys [meanNumberOfRequestsPerSecond]}]
                                    (:ok meanNumberOfRequestsPerSecond))
   :meanResponseTime (fn [])
   :failureRate (fn [])})

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
  ;; fetch the latest report and check the
  (let [results (last-report)]
    (doseq [[name res] results
            [path threshold] (get thresholds name)]
      (testing (str "Scenario: " name ", Step: " path)
        (let [f (get rules  path)]
          (is (<= threshold (f res))))))))
