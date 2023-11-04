(ns clojure-airlines.core
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defrecord Graph [vertices edges])
(defn make-graph []
  (Graph. (ref {}) (ref {})))

(defrecord Vertex [label visited neighbors cost-so-far path])
(defn make-vertex [label]
  (Vertex. label (ref 0) (ref '()) (ref 0) (ref '())))

(defrecord Edge [from to label weight])
(defn make-edge [from to label weight]
  (Edge. from to label weight))



(defn graph-add-vertex! [graph label]
  (let [vertices (:vertices graph)
        new-vertex (make-vertex label)]
    (dosync
      (ref-set vertices (assoc @vertices label new-vertex))))
  nil)

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
    (dosync
      ;(println "adding edge: " (:label from-vertex) (:label to-vertex))
      (ref-set (:edges graph) (assoc @(:edges graph) new-edge-key new-edge))
      (ref-set (:neighbors from-vertex) (conj from-vertex-neighbors to))
      (ref-set (:neighbors to-vertex) (conj to-vertex-neighbors from)))))




;; Parsing the CSV file into a sequence of sequences
(defn take-csv
  [fname]
  (with-open [file (io/reader fname)]
    (-> file
        (slurp)
        (csv/read-csv))))
(def csv-file (take-csv "src/clojure_airlines/Flights_ICA1.csv"))
;(println csv-file)

;; Defining the graph structure
(def g (make-graph))

;; Converting the data obtained from parsing the scv file to the edges and vertices of the graph
(defn csv-to-graph [csv-file g]
  (let [existing-vertex-labels (atom [])]
    (doseq [vector csv-file]
      (doseq [vec (vec (take 2 vector))]
        ;; For every city name we stumble upon, verify whether a corresponding vertex already exists; if not, add one.
        (if (not (.contains @existing-vertex-labels vec))
          (do
            (graph-add-vertex! g (str vec))
            (reset! existing-vertex-labels (conj @existing-vertex-labels vec)))))
      ;; For each row in the CSV file, establish a corresponding edge within the graph.
      (graph-add-edge! g
                       (str (get vector 0))
                       (str (get vector 1))
                       (str (get vector 0) " " (get vector 1) " " (get vector 2))
                       (Integer/parseInt (get vector 2))))))

(csv-to-graph csv-file g)

;; Uncomment to see the edges and vertices of the graph

;(doseq [vertex @(:vertices g)]
;  (println vertex))
;
;(doseq [edge @(:edges g)]
;  (println edge))


;; Additional functions that might be useful

(defn graph-get-neighbors [graph label]
  (let [vertex (get @(:vertices graph) label)]
    (if vertex
      @(:neighbors vertex)
      (do (println (str "Warning: No vertex found for label " label))
          []))))                                            ; Return an empty list if vertex doesn't exist
(defn graph-has-vertex? [graph label]
  (contains? @(:vertices graph) label))
(defn graph-has-edge? [graph from to]
  (contains? @(:edges graph) (graph-edge-key from to)))
(defn graph-reset! [graph]
  (doseq [vertex (vals @(:vertices graph))]
    (dosync (ref-set (:visited vertex) 0))))

(defn get-edge-weight [graph from to]
  (:weight (get @(:edges graph) (graph-edge-key from to))))

(defn reset-costs! [graph]
  (doseq [vertex (vals @(:vertices graph))]
    (dosync
      (ref-set (:cost-so-far vertex) 0))))

(defn bfs-find-plans [graph start-label end-city-spec budget max-flights]
  ; Compute the cost of the start city (self-loop).
  (let [start-cost (get-edge-weight graph start-label start-label)
        ; Initialize a queue of paths with the correct start cost.
        queue (ref [[{:vertex start-label :cost (or start-cost 0)}]])
        ; Initialize an empty list to store valid plans.
        plans (ref [])]

    ; Continue searching as long as there are paths in the queue.
    (while (not (empty? @queue))
      ; Dequeue the first path (FIFO).
      (let [path (first @queue)]
        ; Remove the path from the queue.
        (dosync
          (ref-set queue (rest @queue)))

        ; Extract the current vertex and its cost from the last map in the path.
        (let [current-vertex (-> path last :vertex)
              current-cost (-> path last :cost)
              ; Fetch the data associated with the current vertex from the graph.
              current-vertex-data (get @(:vertices graph) current-vertex)]

          ; Print the current exploring path for debugging purposes.
          ;(println "Exploring path:" (vec (map :vertex path)))

          ; Check if the current vertex is a valid endpoint (matches the desired name)
          ; and the path respects the constraints (cost and number of flights).
          (when (and (and (string? end-city-spec) (= current-vertex end-city-spec))
                     (<= current-cost budget)
                     (<= (- (count path) 1) max-flights))
            ; If it's a valid plan, add it to the list of plans.
            (dosync
              (ref-set plans (conj @plans {:path (map (fn [p] {:city (:vertex p) :cost (:cost p)}) path) :total-cost current-cost}))))

          ; Get the neighbors of the current vertex.
          (let [neighbors (graph-get-neighbors graph current-vertex)]
            (doseq [neighbor neighbors]
              ;(println "current cost: " current-cost) Print the current cost for debugging purposes.
              ; Determine the cost to travel from the current vertex to this neighbor.
              (let [edge-cost (get-edge-weight graph current-vertex neighbor)
                    total-cost (+ current-cost edge-cost)]

                ; Check if the neighbor hasn't been visited in this path,
                ; the path respects the budget, and the number of flights.
                (when (and (not (some #(= neighbor (:vertex %)) path))
                           (<= total-cost budget)
                           (< (- (count path) 1) max-flights))
                  ; If valid, enqueue a new path that includes this neighbor.
                  ; Here we update the cost for the new city in the path to be the cumulative cost up to that city.
                  (dosync
                    (alter queue conj (conj path {:vertex neighbor :cost total-cost}))))))))))

    ; Return the list of valid plans.
    @plans)
  )