(ns clojure_airlines.core
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))
;; Adding additional dependencies

;; Defining the graph structure
(defrecord Graph [vertices edges])
(defn make-graph []
  (Graph. (atom {}) (atom {})))

;; Defining the vertex structure
(defrecord Vertex [label visited neighbors cost-so-far path])
(defn make-vertex [label]
  (Vertex. label (atom 0) (atom '()) (atom 0) (atom '())))


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
(defn graph-reset! [graph]
  (doseq [vertex (vals @(:vertices graph))]
    (reset! (:visited vertex) 0)))

(defn get-edge-weight [graph from to]
  (:weight (get @(:edges graph) (graph-edge-key from to))))

(defn reset-costs! [graph]
  (doseq [vertex (vals @(:vertices graph))]
    (reset! (:cost-so-far vertex) 0)))

;; Parsing the CSV file into a sequence of sequences
(defn take-csv
  [fname]
  (with-open [file (io/reader fname)]
    (-> file
        (slurp)
        (csv/read-csv))))
(def csv-file (take-csv "src/clojure_airlines/Flights_ICA1.csv"))

;(println csv-file)

;; Defining the graph
(def g (make-graph))

;; Converting the data obtained from parsing the scv file to the edges and vertices of the graph
(defn csv-to-graph [csv-file g]
    (doseq [vector csv-file]
      (doseq [vec (vec (take 2 vector))]        ;; For every city name we stumble upon, verify whether a corresponding vertex already exists; if not, add one.

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
  (reset-costs! graph)

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
    (println cities)
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
  (let [start-city (choose-city "Where are you located?" graph)
        end-city (choose-city "Where do you want to go to?" graph)]
    (println "How much do you want to spend?")
    (let [budget (Integer/parseInt (read-line))]
      (println "How many flights can you suffer?")
      (let [max-flights (Integer/parseInt (read-line))]
        [start-city end-city budget max-flights]))))


(defn main [g]
  (let [[start-city end-city budget max-flights] (get-user-input g)
        plans (find-and-sort-plans g start-city end-city budget max-flights)]
    (println (str "Searching for plans from " start-city " to " end-city " with a budget of " budget " and maximum " max-flights " flights:"))
    ;(println plans)
    (if (nil? (first plans))
      (println "No valid plans found!")
      (print-reversed-plans plans))))

(main g)
