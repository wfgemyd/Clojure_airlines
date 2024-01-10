(ns clojure_airlines.core
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]))
;; Adding additional dependencies

;; Defining the graph structure
(defrecord Graph [vertices edges])
(defn make-graph []
  (Graph. (atom {}) (atom {})))

;; Defining the vertex structure
(defrecord Vertex [label neighbors])
(defn make-vertex [label]
  (Vertex. label (atom '())))

;; Adding vertices to the graph
(defn graph-add-vertex! [graph label]
  (let [vertices (:vertices graph)
        new-vertex (make-vertex label)]
    (swap! vertices assoc label new-vertex))
  nil)

;; Defining the edge structure
(defrecord Edge [from to label weight])
(defn make-edge [from to label weight]
  (Edge. from to label weight))

(defn graph-edge-key [from to]
  (sort (list from to)))

(defn graph-add-edge! [graph from to label weight]
  (let [vertices (:vertices graph)
        from-vertex (get @vertices from)
        to-vertex (get @vertices to)
        from-vertex-neighbors @(:neighbors from-vertex)
        to-vertex-neighbors @(:neighbors to-vertex)
        new-edge (make-edge from to label weight)
        new-edge-key (graph-edge-key from to)]
    ;(println "adding edge: " (:label from-vertex) (:label to-vertex))
    (swap! (:edges graph) assoc new-edge-key new-edge)
    (reset! (:neighbors from-vertex) (conj from-vertex-neighbors to))
    (reset! (:neighbors to-vertex) (conj to-vertex-neighbors from))))

;; Additional functions that might be useful
(defn graph-has-vertex? [graph label]
  (contains? @(:vertices graph) label))
(defn graph-has-edge? [graph from to]
  (contains? @(:edges graph) (graph-edge-key from to)))
(defn get-edge-weight [graph from to]
  (:weight (get @(:edges graph) (graph-edge-key from to))))

;; Parsing the CSV file into a sequence of sequences
(defn take-csv
  [fname]
  (if (clojure.string/ends-with? fname ".csv")
    (try (with-open [file (io/reader fname)]
           (-> file
               (slurp)
               (csv/read-csv)))
         (catch Exception ex (println "File not found. Please place the file in the same directory as the script.")))
    (println "Invalid file format! Please provide a CSV file as a source.")))
(def csv-file (take-csv "src/clojure_airlines/Flights_ICA1.csv"))

;(println csv-file)

;; Defining the graph
(def g (make-graph))

;; Converting the data obtained from parsing the scv file to the edges and vertices of the graph
(defn csv-to-graph [csv-file g]
  (doseq [vector csv-file]
    (doseq [vec (vec (take 2 vector))]                      ;; For every city name we stumble upon, verify whether a corresponding vertex already exists; if not, add one.

      (if (not (graph-has-vertex? g vec))
        (graph-add-vertex! g (str vec))))
    ;; For each row in the CSV file, establish a corresponding edge within the graph.
    (graph-add-edge! g
                     (str (get vector 0))
                     (str (get vector 1))
                     (str (get vector 0) " " (get vector 1) " " (get vector 2))
                     (Integer/parseInt (get vector 2)))))

(csv-to-graph csv-file g)

;; Uncomment to see the edges and vertices of the graph

;(doseq [vertex @(:vertices g)]
;  (println vertex))
;
;(doseq [edge @(:edges g)]
;  (println edge))

(defn graph-get-neighbors [graph label]
  (let [vertex (get @(:vertices graph) label)]
    (if vertex
      @(:neighbors vertex)
      (do (println (str "Warning: No vertex found for label " label))
          []))))                                            ; Return an empty list if vertex doesn't exist

(defn check-constraints [cost budget path max-flights]
  (if (and (<= cost budget)
           (< (- (count path) 1) max-flights))
    true
    false))

(defn bfs-find-plans [graph start-label end-city-spec budget max-flights]
  ; Compute the cost of the start city (self-loop).
  (let [start-cost (get-edge-weight graph start-label start-label)
        ; Initialize a queue of paths with the correct start cost.
        queue (ref [[{:vertex start-label :cost (or start-cost 0)}]])
        ; Initialize an empty list to store valid plans.
        plans (atom [])]

    ; Continue searching as long as there are paths in the queue.
    (while (not (empty? @queue))
      ; Dequeue the first path (FIFO).
      (let [path (first @queue)]
        ; Remove the path from the queue.
        (dosync
          (ref-set queue (rest @queue)))

        ; Extract the current vertex and its cost from the last map in the path.
        (let [current-vertex (-> path last :vertex)
              current-cost (-> path last :cost)]

          ; Print the current exploring path for debugging purposes.
          ;(println "Exploring path:" (vec (map :vertex path)))

          ; Check if the current vertex is a valid endpoint (matches the desired name)
          ; and the path respects the constraints (cost and number of flights).
          (when (and (and (string? end-city-spec) (= current-vertex end-city-spec))
                     (check-constraints current-cost budget path max-flights))
            ; If it's a valid plan, add it to the list of plans.
            (swap! plans conj {:path (map (fn [p] {:city (:vertex p) :cost (:cost p)}) path) :total-cost current-cost}))

          ; Check if current vertex is not the destination before exploring further.
          ; If it is, do not explore the neighbors of the current vertex.
          (when (not (= current-vertex end-city-spec))
            ; Get the neighbors of the current vertex.
            (let [neighbors (graph-get-neighbors graph current-vertex)]
              (doseq [neighbor neighbors]
                ; Determine the cost to travel from the current vertex to the current neighbor.
                (let [edge-cost (get-edge-weight graph current-vertex neighbor)
                      total-cost (+ current-cost edge-cost)]
                  ; Check if the neighbor hasn't been visited in this path,
                  ; the path respects constraints.
                  (when (and (not (some #(= neighbor (:vertex %)) path))
                             (check-constraints total-cost budget path max-flights))
                    ; If valid, enqueue a new path that includes this neighbor.
                    ; Here we update the cost for the new city in the path to be the cumulative cost up to that city.
                    (dosync
                      (alter queue conj (conj path {:vertex neighbor :cost total-cost})))))))))))

    ; Return the list of valid plans.
    @plans))

(defn sort-plans [plans]
  ;(println "Sorting plans..." plans)
  (sort-by (juxt (comp - :total-cost) (comp count :path)) plans))

(defn remove-duplicate-paths [plans]
  (let [seen-flights (atom #{})]
    (filter (fn [plan]
              (let [num-flights (- (count (:path plan)) 1)]
                (if (contains? @seen-flights num-flights)
                  false
                  (do
                    (swap! seen-flights conj num-flights)
                    true))))
            plans)))

(defn find-and-sort-plans [graph start-label end-city-name budget max-flights]
  ; Reset the costs before starting the search
  ; Use the BFS function to find the plans with the hard-max-flights as the constraint
  (let [raw-plans (bfs-find-plans graph start-label end-city-name budget max-flights)]
    ;(println "Raw plans:" raw-plans) ; Print raw plans here

    (let [filtered-plans (filter
                           (fn [plan]
                             (and (<= (:total-cost plan) budget)
                                  (< (- (count (:path plan)) 1) max-flights)))
                           raw-plans)]
      ;(println "Filtered plans:" filtered-plans) ; Print filtered plans here

      (let [sorted-plans (sort-plans filtered-plans)
            distinct-plans (remove-duplicate-paths sorted-plans)
            most-expensive-plan (first distinct-plans)
            cheapest-plan (last distinct-plans)]
        ; Check if the two plans are the same, if so, return only one
        (if (= most-expensive-plan cheapest-plan)
          [most-expensive-plan]
          [most-expensive-plan cheapest-plan])))))

(defn format-path [path]
  (let [formatted-path (map (fn [{:keys [city cost]}]
                              (str city (if (zero? cost) "" (str " (" cost ")"))))
                            path)]
    (str (clojure.string/join " --> " formatted-path))))


(defn reverse-engineer-costs [path]
  (loop [remaining-path (reverse path)                      ; Reverse the path so we start from the end
         last-cost (-> path last :cost)
         result []]
    (if (empty? remaining-path)
      (reverse result)                                      ; Return the corrected path order
      (let [current-cost (or (-> remaining-path first :cost) 0)
            calculated-cost (- last-cost current-cost)]
        (recur (rest remaining-path) current-cost
               (conj result (assoc (first remaining-path) :cost calculated-cost)))))))

(defn print-ascii-ticket [formatted-path total-cost flights]
  (let [art ["|d888888P dP  a88888b. dP     dP  88888888b d888888P"
             "|   88    88 d8'   `88 88   .d8'  88           88   "
             "|   88    88 88        88aaa8P'  a88aaaa       88   "
             "|   88    88 88        88   `8b.  88           88   "
             "|   88    88 Y8.   .88 88     88  88           88   "
             "|   dP    dP  Y88888P' dP     dP  88888888P    dP   "]

        info [(str "Path: " formatted-path)
              (str "Total Cost: " total-cost)
              (str "Amount of flights: " (- flights 1))]

        max-info-len (apply max (map count info))
        max-art-len (count (first art))
        total-len (+ max-art-len max-info-len 5)]
    (println (clojure.string/join "" (repeat total-len "-")))
    (doseq [i (range (count art))]
      (let [info-line (nth info (- i 1) nil)]
        (println (str (nth art i) " | "
                      (if info-line
                        (str info-line (apply str (repeat (- max-info-len (count info-line)) " ")))
                        (apply str (repeat max-info-len " ")))
                      " |"))))
    ; Print separator at the end only
    (println (clojure.string/join "" (repeat total-len "-")))))


(defn print-reversed-plans [plans]
  (doseq [plan plans]
    (let [{:keys [path total-cost]} plan
          reversed-path (reverse-engineer-costs path)
          formatted-path (format-path reversed-path)]
      (print-ascii-ticket formatted-path total-cost (count path)))))

(defn get-all-cities [graph]
  (keys @(:vertices graph)))


(defn choose-city [prompt graph]
  (let [cities (get-all-cities graph)]
    ;(println cities)
    (println prompt)
    (doseq [[idx city] (map vector (range 1 (inc (count cities))) cities)]
      (println (str idx ". " city)))
    (let [choice-str (read-line)
          choice (if
                   (re-matches #"\d+" choice-str)
                   (Integer/parseInt choice-str) 0)]        ; Convert valid string to integer
      (cond (and (>= choice 1) (<= choice (count cities)))
            (nth cities (dec choice))
            (some #{choice-str} cities)
            choice-str
            :else (do
                    (println "Invalid choice. Please choose again.")
                    (recur prompt graph))))))

(defn get-user-input [graph]
  (when (not (empty? @(:vertices graph)))
    (let [start-city (choose-city "Where are you located?" graph)
          end-city (choose-city "Where do you want to go to?" graph)]
      (println "How much do you want to spend?")
      (let [budget (Integer/parseInt (read-line))]
        (println "How many flights can you suffer?")
        (let [max-flights (+ 1 (Integer/parseInt (read-line)))]
          [start-city end-city budget max-flights])))))

(defn main [g]
  (when (not (empty? @(:vertices g)))
    (let [[start-city end-city budget max-flights] (get-user-input g)
          plans (find-and-sort-plans g start-city end-city budget max-flights)]
      (println (str "Searching for plans from " start-city " to " end-city " with a budget of " budget " and maximum " (- max-flights 1) " flights:"))
      ;(println plans)
      (if (nil? (first plans))
        (println "No valid plans found!")
        (print-reversed-plans plans)))))

;; People classification function returns true if the group is a family, false otherwise.
(defn people-classification [people]
  (let [surnames (atom [])
        ysob (atom [])]
    (doseq [person people]
      (let [surname (second (str/split (first person) #" "))
            yob (second person)]
        (swap! surnames conj surname)
        (swap! ysob conj yob)))
    ;; The 2006 is hardcoded, as there is no specific function in clojure to retrieve the current year.
    ;; If the group consists of at least one child and one adult, and all the people have the same surname, the group is a family.
    ;; The departure and destination of the people in the group is always the same, as it is defined in the broker program.
    (if (and (= (count (distinct @surnames)) 1)
             (some #(> % 2006) @ysob)
             (some #(< % 2006) @ysob))
      true
      false)))

;; The following function retrieves the data from the analysis file and predicts the budget based on it for each customer group.
(defn get-stats-return-budget [historical-file p-type dep dest]
  (let [historical-data (clojure_airlines.analysis/process-csv historical-file)
        statistics (clojure_airlines.analysis/calculate-statistics (clojure_airlines.analysis/transform-data historical-data 2024))
        filtered-stats (atom
                         (filter #(and (= (:departure %) dep)
                                       (= (:destination %) dest))
                                 statistics))
        ;; The default budget is 1000, if the error occurs and there would be no statistics found.
        budget-output (atom 1000)
        p-type-transformed (if p-type
                             "family"
                             "group")
        ;; Mean of all the historically bought tickets for the specific group type and all the routes.
        stats-for-type-general (try
                                 (clojure_airlines.analysis/mean
                                 (map :mean
                                      (filter #(= (:group-type %) p-type-transformed) statistics)))
                                 ;; Set to 0 if the error occurs (no statistics found).
                                 (catch Exception e
                                   0))]

    ;; If the departure and destination are swapped, we still consider it as the same route.
    (if (empty? @filtered-stats)
      (reset! filtered-stats (filter #(and (= (:departure %) dest)
                                           (= (:destination %) dep))
                                     statistics)))

    ;; If there is no route even after swapping departure and destination OR
    ;; If there is no historical data for this route and for this specific group type:
    (if (or (empty? @filtered-stats) (empty? (filter #(= (:group-type %) p-type-transformed) @filtered-stats)))
      (do
        ;; We proceed to check whether there is a data for all the routes for this specific group type.
        (if (= 0 stats-for-type-general)
          ;; If there is no, the budget is set to the mean of all the historical prices, for all destinations and group types.
          (reset! budget-output (clojure_airlines.analysis/mean (map :mean statistics)))
          ;; If there is, we set the budget to the mean of the historical prices for this specific group type, for all the routes.
          (reset! budget-output stats-for-type-general)))

      ;; If the historical data for this route and group type exists, we set the budget to
      ;; the maximum amount of money that was spent historically for this route and group type.
        (reset! budget-output
              (:max (first (filter #(= (:group-type %) p-type-transformed) @filtered-stats)))))
    ;;(println "PREDICTED BUDGET IS: " @budget-output)
    @budget-output
    ))

;; This function retrieves the cheapest ticket price from the plans that were found.
(defn check-broker [plans]
  (let [plan (last plans)]
    (let [{:keys [path total-cost]} plan]
      ;;(println "TOTAL COST: " total-cost)
      ;;(println "FOR PATH: " formatted-path)

      total-cost)))

(def total-profit (atom 0))

;; Main-check-broker is the search function that is called by the broker program.
;; It returns the price of the ticket that will be sold to the customer.
(defn main-check-broker [departure-city destination-city people]
  (let [g g]
    (when (not (empty? @(:vertices g)))
      ;; Get statistics from the historical data and return the predicted budget for the customer.
      (let [budget (get-stats-return-budget "/Users/anna-alexandradanchenko/Documents/University/Second Year/Symbolic Computation/Clojure_airlines/src/clojure_airlines/data/sales_team_2.csv"
                                            ;; Classify whether the customers belong to a family or a group
                                            (people-classification people)
                                            departure-city
                                            destination-city)
            ;; Rounding up the budget to the nearest lowest 100 to ensure that the ticket will be sold.
            rounded-budget (* 100 (Math/floor (/ budget 100)))
            ;; If the group is a family, the maximum number of flights is 3, otherwise it is 4.
            max-cities (if (people-classification people)
                         4
                         5)
            ;; Find the plans that match the customer's budget and the maximum number of flights.
            plans (find-and-sort-plans g departure-city destination-city budget max-cities)
            ;; Get the actual price of the cheapest ticket that we have.
            ticket-price (check-broker plans)]

        ;; If there is no plans found, it returns ##Inf, to ensure that the broker will not sell non-existing ticket.
        (if (nil? (first plans))
          (do
            ;;(println "NO PLANS FOUND" departure-city "to" destination-city)
            ##Inf)
          (do
            ;; If the customer budget is too low, the function returns ##Inf
            ;; In order to ensure that there won't be negative profit (the broker won't sell ticket for infinite money).
            (if (< rounded-budget ticket-price)
              (do
                ;;(println "BUDGET IS TOO LOW, CAN'T SELL TICKET")
                ##Inf)
              (do
                ;;(println "TICKET PRICE IS: " ticket-price)
                ;;(println "WILL BE SOLD TO CUSTOMER: " rounded-budget)
                ;;(println "PROFIT IS: " (- rounded-budget ticket-price))

                ;; Here, we calculate the maximum clean profit we can acquire if all the tickets will be sold.
                ;; We can not calculate the real profit, as we don't know which tickets will be sold, the broker function does not return that.
                (reset! total-profit (+ @total-profit (- rounded-budget ticket-price)))
                (println "TOTAL PROFIT" @total-profit)
                rounded-budget))))))))

;; If you want to output the total clean profit for the company in the case if all of the tickets will are sold, uncomment the following line.
(println @total-profit)
(reset! total-profit 0)

;; Cheap Ticket and Minimum Budget
;Sold tickets: 485 piece(s)
;Earned: 123600
;; 46600.0

;; Cheap Ticket and Average Budget
;Sold tickets: 452 piece(s)
;Earned: 116600
;; 54700.0

;; Cheap Ticket and Maximum Budget
;Sold tickets: 467 piece(s)
;Earned: 128000
;; 61200.0

;; The most expensive ticket and Minimum Budget
;Sold tickets: 485 piece(s)
;Earned: 123600
;; 0.0

;; The most expensive ticket and Average Budget
;; Sold tickets: 452 piece(s)
;; Earned: 116600
;; 10000.0

;; The most expensive ticket and Maximum Budget
;; Sold tickets: 467 piece(s)
;; Earned: 128000
;; 11000.0

;; TODO Sell as much tickets as you can
;; TODO Groups will buy for the maximum price, Families plan from advance,

;; Zagreb to Krakow
;TODO NO PLANS FOUND WRITE ABOUT IT IN THE REPORT