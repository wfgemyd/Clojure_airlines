(ns clojure_airlines.sales_routines
  (:require [clojure_airlines.broker :as broker])
  ; TODO replace this link with your engine
  (:require [clojure_airlines.core :as your_engine])
  )

; TODO SET YOUR TEAM NUMBER: 1-7
(def team_number 2)
(def search_ticket_function your_engine/main-check-broker)
(broker/run team_number search_ticket_function)




