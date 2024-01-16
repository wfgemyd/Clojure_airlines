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

  ;; Group the data by departure and destination.
  (let [grouped-by-route (group-by #(vector (:departure %) (:destination %)) transformed-data)


        ;; Calculate the maximum price for each route.
        ;; This involves mapping over each group, extracting the 'paid' values, and finding the maximum.
        max-prices (->> grouped-by-route
                        (map (fn [[k v]] [k (apply max (map :paid v))]))
                        (into {}))

        ;; Adding a keyword flag to each map with data, indicating whether the paid price is the maximum price for the specific departure-destination route.
        with-max-price-flag (map #(assoc % :max-price-sold (= (:paid %)
                                                              (get max-prices [(:departure %) (:destination %)])))
                                 transformed-data)

        ;; Group the data by group type, departure, and destination.
        grouped-by-category (group-by #(vector (:relation %) (:departure %) (:destination %))
                                      with-max-price-flag)]

    ;; Process each group to calculate the success rate.
    (->> grouped-by-category
         (map (fn [[k v]]
                ;; Total number of records in the group.
                (let [total (count v)
                      ;; Count of records where the maximum price was paid.
                      max-price-sold-count (count (filter :max-price-sold v))
                      ;; Calculate the success rate as the ratio of max-price-sold to total records.
                      success-rate (float (/ max-price-sold-count total))]

                  ;; Return a map with the group type, departure, destination, and the calculated success rate.
                  {:group-type (nth k 0)
                   :departure (nth k 1)
                   :destination (nth k 2)
                   :success-rate success-rate})))
         (into []))))

;; Function to predict future sales based on demand
(defn predict-future-sales-with-demand [transformed-data]
  ;; Group the data by departure and destination to get the routes.
  (let [grouped-by-route (group-by #(vector (:departure %) (:destination %)) transformed-data)

        ;; Calculate the maximum price for each route.
        ;; This is done by mapping over each group, extracting the 'paid' values, and finding the maximum.
        max-prices (->> grouped-by-route
                        (map (fn [[k v]] [k (apply max (map :paid v))]))
                        (into {}))

        ;; Adding a keyword flag to each map with data, indicating whether the paid price is the maximum price for the specific departure-destination route.
        with-max-price-flag (map #(assoc % :max-price-sold (= (:paid %)
                                                              (get max-prices [(:departure %) (:destination %)])))
                                 transformed-data)

        ;; Group the data again, this time by group type, departure, and destination, to analyze each route with respect to the group type.
        grouped-by-category (group-by #(vector (:relation %) (:departure %) (:destination %))
                                      with-max-price-flag)

        ;; Calculate the proportion of sales at the maximum price for each route and group type.
        max-price-proportion (->> grouped-by-category
                                  (map (fn [[k v]]
                                         (let [total (count v)
                                               max-price-sold-count (count (filter :max-price-sold v))]
                                           [k (float (/ max-price-sold-count total))])))
                                  (into {}))

        ; Counting total sales for each route and group type.
        total-sales (->> grouped-by-category
                         (map (fn [[k v]] [k (count v)]))
                         (into {}))

        ;; Predict future sales for each category based on the proportion of max-price sales and total sales.
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

;; Calculate the probability of purchase if the price is increased by the specified percentage
(defn calculate-purchase-probability-with-new-analysis
  [transformed-data increase-percentage statistics success-rates future-sales-predictions]

  ;; Function to calculate the new price after applying the increase percentage.
  ;; By increasing the 'paid' amount by the specified percentage.
  (let [increase-fn (fn [paid] (+ paid (* paid increase-percentage 0.01)))

        ;; Apply the increase function to each record in the dataset.
        ;; This step creates a new key-value pair in each record: :new-max-price, which is the increased price.
        with-new-max-price (map #(assoc % :new-max-price (increase-fn (:paid %))) transformed-data)

        ;; Function to merge each record (route and group type) with corresponding statistics, success rates, and future sales predictions.
        merge-fn (fn [record]

                   ;; Extract statistics, success rates and prediction
                   ;; for the current record based on group-type, departure, and destination.
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

                     ;; Merging the current record with its corresponding statistics, success rates, and future sales predictions.
                     (merge record matching-statistics matching-success-rates matching-future-sales)))
        merged-data (map merge-fn with-new-max-price)

        ;; Function to calculate the probability of purchase for each record.
        calculate-probability (fn [row]
                                ;; Calculate probability only if the new price is not more that the maximum historical price.
                                (if (<= (:new-max-price row) (:max row))
                                  ;; The probability of purchase is the product of success rate and predicted demand, capped at 1.
                                  (min (* (:success-rate row) (:predicted-demand row)) 1)
                                  0))

        ;; Associate each record with its calculated probability of purchase after the price increase.
        with-probability (map #(assoc % :probability-with-increase (calculate-probability %)) merged-data)
        filtered-data (filter #(> (:probability-with-increase %) 0) with-probability)]

    ;; Return a distinct list of records, each containing only the essential keys for analysis.
    ;; These keys include group-type, departure, destination, and the newly calculated probability with the price increase.
    (distinct (map #(select-keys % [:group-type :departure :destination :probability-with-increase]) filtered-data))))


(defn mean [values]
  (let [sum (reduce + 0 values)
        count (count values)]
    (/ sum count)))

; A function to calculate mean, max, min, median and demand values for each group type and route
(defn calculate-statistics [transformed-data]
  (let [grouped-data (group-by #(vector (:relation %) (:departure %) (:destination %)) transformed-data)
        calculate-stats (fn [[k v]]
                          ;; Extract the 'paid' values from each group.
                          (let [paid-values (map :paid v)

                                ;; Sort the paid values to assist in median calculation.
                                sorted-values (sort paid-values)

                                ;; Calculate the median value.
                                ;; If the number of values is odd, take the middle value.
                                ;; If even, calculate the mean of the two middle values.
                                median-val (if (odd? (count sorted-values))
                                             (nth sorted-values (quot (count sorted-values) 2))
                                             (mean [(nth sorted-values (quot (count sorted-values) 2))
                                                    (nth sorted-values (dec (quot (count sorted-values) 2)))]))

                                ;; Find the maximum and minimum value in the paid values.
                                max-val (apply max paid-values)
                                min-val (apply min paid-values)

                                ;; Calculate the mean value.
                                mean-val (mean paid-values)

                                ;; Demand is the number of tickets sold,
                                ;; which is the count of paid values in the group.
                                count-val (count paid-values)]

                            ;; Return a map containing the calculated statistics for each group.
                            ;; This includes the group type, departure, destination, and the calculated values.
                            {:group-type (nth k 0)
                             :departure (nth k 1)
                             :destination (nth k 2)
                             :max max-val
                             :min min-val
                             :mean mean-val
                             :median median-val
                             :count count-val}))]
    (map calculate-stats grouped-data)))


;; Main function to print the statistics
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