(ns gatling-helpers
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

(defn parse-report [raw-js]
  (->> (str/split raw-js #"stats: ")
   (drop 1)
   (map #(str/replace % #",\w+\"group1\".*", "}"))
   (map #(json/parse-string % keyword))
   (group-by :name)
   (reduce-kv (fn [res name [r]]
                (assoc res name r))
              {})))