(ns clojure-airlines.core
  (:gen-class))

(defrecord Graph [vertices edges])
(defn make-graph []
  (Graph. (ref {}) (ref {})))

(defrecord Vertex [label visited neighbors cost-so-far path])
(defn make-vertex [label]
  (Vertex. label (ref 0) (ref '()) (ref 0) (ref '())))

(defrecord Edge [from to label weight])
(defn make-edge [from to label weight]
  (Edge. from to label weight))