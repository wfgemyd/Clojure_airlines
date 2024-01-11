(ns clojure_airlines.broker
  (:require [clojure.java.io :as io]))

(defn read_data [full_path]
  (into [] (rest
             (with-open [rdr (clojure.java.io/reader full_path)]
               (reduce conj [] (line-seq rdr)))
             ))
  )

(defn run
  [team_number search_function]
  (let [data_file_name (str "src/clojure_airlines/data/broker_team_" team_number ".csv")]
    (if (.exists (io/as-file data_file_name))
      (let [data (read_data data_file_name)
            departure (atom "")
            destination (atom "")
            people (atom [])
            last_proposed_budget (atom 0)
            income (atom 0)
            sold_amount (atom 0)
            data_count (count data)
            ]
        (loop [row 0]
          (if (< row data_count)
            (do
              (let [tokens (clojure.string/split (get data row) #",")
                    current_name (first tokens)
                    current_yob (Integer/parseInt (second tokens))
                    current_departure (get tokens 2)
                    current_destination (get tokens 3)
                    current_budget (Integer/parseInt (last tokens))
                    count_people (count @people)
                    search_function_proposition (search_function current_departure current_destination @people)
                    ;_ (println current_name "|" current_yob "|" current_departure "|" current_destination "|" current_budget)
                    ]
                (when (empty? @departure)
                  (reset! departure current_departure)
                  )
                (when (empty? @destination)
                  (reset! destination current_destination)
                  )

                (reset! last_proposed_budget current_budget)

                (if (and (not= current_departure @departure)
                         (not= current_destination @destination)
                         )
                  (do
                    (when (and (> count_people 0)
                               (>= current_budget search_function_proposition)
                               )
                      (swap! income + (* search_function_proposition count_people))
                      (swap! sold_amount + count_people)
                      )
                    (reset! departure current_departure)
                    (reset! destination current_destination)
                    (reset! people [[current_name current_yob]])
                    )
                  (do
                    (swap! people conj [current_name current_yob])
                    )
                  )
                )
              (recur (inc row))
              )
            (let [search_function_proposition (search_function @departure @destination @people)
                  count_people (count @people)
                  ]
              (when (and (> count_people 0)
                         (>= @last_proposed_budget search_function_proposition)
                         )
                (swap! income + (* search_function_proposition count_people))
                (swap! sold_amount + count_people)
                )
              )
            )
          )
        (println "Simulation completed!")
        (println "Sold tickets:" @sold_amount "piece(s)")
        (println "Earned:" @income)
        )
      (println "Can't find a data file. Please check team number.")
      )
    )
  )
