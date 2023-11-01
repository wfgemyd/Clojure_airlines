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

