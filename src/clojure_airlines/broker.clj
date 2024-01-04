(ns  clojure_airlines.broker
  (:require [clojure.java.io :as io]))

(defn read_data [full_path]
  (into [] (rest
             (with-open [rdr (clojure.java.io/reader full_path)]
               (reduce conj [] (line-seq rdr)))
             ))
  )

(defn run
  [team_number search_function]
  (let [data_file_name (str "/Users/anna-alexandradanchenko/Documents/University/Second Year/Symbolic Computation/Clojure_airlines/src/clojure_airlines/data/broker_team_" team_number ".csv")]
    (if (.exists (io/as-file data_file_name))
      (let [data (read_data data_file_name)
            departure (atom "")
            destination (atom "")
            people (atom [])
            income (atom 0)
            sold_amount (atom 0)
            ]
        (loop [row 0]
          (when (< row (count data))
            (let [tokens (clojure.string/split (get data row) #",")
                  current_name (first tokens)
                  current_yob (Integer/parseInt (second tokens))
                  current_departure (get tokens 2)
                  current_destination (get tokens 3)
                  current_budget (Integer/parseInt (last tokens))
                  count_people (count @people)
                  ;_ (println current_name "|" current_yob "|" current_departure "|" current_destination "|" current_budget)
                  ]
              (if (and (not= current_departure @departure)
                       (not= current_destination @destination)
                       )
                (do
                  (when (and (> count_people 0)
                             (>= current_budget (search_function current_departure current_destination @people))
                             )
                    (swap! income + current_budget)
                    (swap! sold_amount + count_people)
                    (println "SOLD")
                    )
                  (reset! departure current_departure)
                  (reset! destination current_destination)
                  (reset! people [])
                  (newline)
                  )
                (swap! people conj [current_name current_yob])
                )
              )
            (recur (inc row)))
          )
        (println "Simulation completed!")
        (println "Sold tickets:" @sold_amount "piece(s)")
        (println "Earned:" @income)
        )
      (println "Can't find a data file. Please check team number.")
      )
    )
  )
