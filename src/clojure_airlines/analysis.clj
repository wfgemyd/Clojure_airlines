(ns clojure_airlines.analysis
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]))

; Loading and Preprocessing the CSV Data
(defn read-csv [filename]
  (with-open [reader (io/reader filename)]
    (doall (csv/read-csv reader))))

(defn parse-row [row]
  {:full-name (nth row 0)
   :yob (Integer/parseInt (nth row 1))
   :departure (nth row 2)
   :destination (nth row 3)
   :paid (Double/parseDouble (nth row 4))})

(defn process-csv [filename]
  (map parse-row (rest (read-csv filename)))) ; 'rest' to skip header

; Print Raw Data for Verification
(defn print-raw-data [filename]
  (let [data (process-csv filename)]
    (doseq [row data]
      (println row))))

; Data Transformation Functions
(defn split-name [record]
  (let [[name surname] (str/split (:full-name record) #" ")]
    (assoc record :name name :surname surname)))

(defn calculate-age [record current-year]
  (assoc record :age (- current-year (:yob record))))

; Utility Functions
(defn adult? [age] (>= age 18))

; Identifying Families
; The group is a family if:
(defn identify-family-group [transformed-data]
  ; ; 1. Departure, destination and surnames are the same
  (let [grouped-by-common-traits (group-by #(vector (:surname %) (:departure %) (:destination %) (:paid %)) transformed-data)]
    ;; Otherwise, identify as a group
    (mapcat (fn [[_ group]]
              (let [adults (filter #(adult? (:age %)) group)
                    children (remove #(adult? (:age %)) group)]
                ; ; 2. There are at least one adult and one child in the group
                (if (and (>= (count adults) 1) (>= (count children) 1))
                  (map #(assoc % :relation "family") group)
                  (map #(assoc % :relation "group") group))))
            grouped-by-common-traits)))

; Data Transformation Functions (Updated)
(defn transform-data [dataset current-year]
  (->> dataset
       (map #(-> % (split-name) (calculate-age current-year)))
       (identify-family-group)))

;;(println (transform-data (process-csv "src/clojure_airlines/data/sales_team_2.csv") 2023))

; calculating the percentage of max price purchase
(defn calculate-success-rates [transformed-data]
  (let [grouped-by-route (group-by #(vector (:departure %) (:destination %)) transformed-data)
        max-prices (->> grouped-by-route
                        (map (fn [[k v]] [k (apply max (map :paid v))]))
                        (into {}))
        with-max-price-flag (map #(assoc % :max-price-sold (= (:paid %)
                                                              (get max-prices [(:departure %) (:destination %)])))
                                 transformed-data)
        grouped-by-category (group-by #(vector (:relation %) (:departure %) (:destination %))
                                      with-max-price-flag)]
    (->> grouped-by-category
         (map (fn [[k v]]
                (let [total (count v)
                      max-price-sold-count (count (filter :max-price-sold v))
                      success-rate (float (/ max-price-sold-count total))]
                  {:group-type (nth k 0)
                   :departure (nth k 1)
                   :destination (nth k 2)
                   :success-rate success-rate})))
         (into []))))

;; Function to predict future sales based on demand
(defn predict-future-sales-with-demand [transformed-data]
  (let [grouped-by-route (group-by #(vector (:departure %) (:destination %)) transformed-data)
        max-prices (->> grouped-by-route
                        (map (fn [[k v]] [k (apply max (map :paid v))]))
                        (into {}))
        ;; Adding a keyword flag to each map with data, indicating whether the paid price is the maximum price for the specific departure-destination route.
        with-max-price-flag (map #(assoc % :max-price-sold (= (:paid %)
                                                              (get max-prices [(:departure %) (:destination %)])))
                                 transformed-data)

        grouped-by-category (group-by #(vector (:relation %) (:departure %) (:destination %))
                                      with-max-price-flag)

        max-price-proportion (->> grouped-by-category
                                  (map (fn [[k v]]
                                         (let [total (count v)
                                               max-price-sold-count (count (filter :max-price-sold v))]
                                           [k (float (/ max-price-sold-count total))])))
                                  (into {}))
        ; Counting total sales for each category.
        total-sales (->> grouped-by-category
                         (map (fn [[k v]] [k (count v)]))
                         (into {}))

        predicted-sales (map (fn [[k max-price-prop]]
                               (let [total (get total-sales k)]
                                 {:group-type (nth k 0)
                                  :departure (nth k 1)
                                  :destination (nth k 2)
                                  :max-price-proportion max-price-prop
                                  :total-sales total
                                  :predicted-demand (* max-price-prop total)}))
                             max-price-proportion)]
    predicted-sales))

;;probabilty calc
(defn calculate-purchase-probability-with-new-analysis
  [transformed-data increase-percentage statistics success-rates future-sales-predictions]
  (let [increase-fn (fn [paid] (+ paid (* paid increase-percentage 0.01)))
        with-new-max-price (map #(assoc % :new-max-price (increase-fn (:paid %))) transformed-data)
        merge-fn (fn [record]
                   (let [matching-statistics (first (filter #(and (= (:group-type %) (:relation record))
                                                                  (= (:departure %) (:departure record))
                                                                  (= (:destination %) (:destination record)))
                                                            statistics))
                         matching-success-rates (first (filter #(and (= (:group-type %) (:relation record))
                                                                     (= (:departure %) (:departure record))
                                                                     (= (:destination %) (:destination record)))
                                                               success-rates))
                         matching-future-sales (first (filter #(and (= (:group-type %) (:relation record))
                                                                    (= (:departure %) (:departure record))
                                                                    (= (:destination %) (:destination record)))
                                                              future-sales-predictions))]
                     (merge record matching-statistics matching-success-rates matching-future-sales)))
        merged-data (map merge-fn with-new-max-price)
        calculate-probability (fn [row]
                                (if (<= (:new-max-price row) (:max row))
                                  (min (* (:success-rate row) (:predicted-demand row)) 1)
                                  0))
        with-probability (map #(assoc % :probability-with-increase (calculate-probability %)) merged-data)
        filtered-data (filter #(> (:probability-with-increase %) 0) with-probability)]
    (distinct (map #(select-keys % [:group-type :departure :destination :probability-with-increase]) filtered-data))))


(defn mean [values]
  (let [sum (reduce + 0 values)
        count (count values)]
    (/ sum count)))

; A function to calculate mean, max, min, median and demand values for each group type and route
(defn calculate-statistics [transformed-data]
  (let [grouped-data (group-by #(vector (:relation %) (:departure %) (:destination %)) transformed-data)
        calculate-stats (fn [[k v]]
                          (let [paid-values (map :paid v)
                                sorted-values (sort paid-values)
                                median-val (if (odd? (count sorted-values))
                                             (nth sorted-values (quot (count sorted-values) 2))
                                             (mean [(nth sorted-values (quot (count sorted-values) 2))
                                                    (nth sorted-values (dec (quot (count sorted-values) 2)))]))
                                max-val (apply max paid-values)
                                min-val (apply min paid-values)
                                mean-val (mean paid-values)
                                ;; demand is the number of tickets sold
                                count-val (count paid-values)]
                            {:group-type (nth k 0)
                             :departure (nth k 1)
                             :destination (nth k 2)
                             :max max-val
                             :min min-val
                             :mean mean-val
                             :median median-val
                             :count count-val}))]
    (map calculate-stats grouped-data)))


;main
(defn run-analysis [filename current-year increase-percentage]
  (let [raw-data (process-csv filename)
        transformed-data (transform-data raw-data current-year)
        statistics (calculate-statistics transformed-data)
        success-rates (calculate-success-rates transformed-data)
        future-sales-predictions (predict-future-sales-with-demand transformed-data)
        purchase-probabilities (calculate-purchase-probability-with-new-analysis transformed-data increase-percentage statistics success-rates future-sales-predictions)]
    (println "Basic Statistics:")
    (doseq [stat statistics]
      (println stat))
    (println "\nSuccess Rates:")
    (doseq [rate success-rates]
      (println rate))
    (println "\nPredicted Future Sales:")
    (doseq [prediction future-sales-predictions]
      (println prediction))
    (println "\nPurchase Probabilities:")
    (doseq [probability purchase-probabilities]
      (println probability))))


(run-analysis "src/clojure_airlines/data/sales_team_2.csv" 2023 4) ;file_name current_year increase_%


