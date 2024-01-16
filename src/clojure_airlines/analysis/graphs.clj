(ns clojure-airlines.analysis.graphs
  (:use [incanter core charts io]))

;; Extract the analysis data
(def data (clojure_airlines.analysis.analysis/calculate-statistics
            (clojure_airlines.analysis.analysis/transform-data (clojure_airlines.analysis.analysis/process-csv "src/clojure_airlines/data/sales_team_2.csv") 2024)))

;; Extracting the data we need for our plot, specifically mean price for each route and group type
(defn extract-plot-data [data]
  (map (fn [{:keys [group-type departure destination mean]}]
         [group-type (str departure " to " destination) mean])
       data))

;; Plotting the data using Incanter bar chart. We use the group-type as the x-axis, the mean price as the y-axis.
(defn plot-data [plot-data]
  (let [chart (bar-chart (map second plot-data) (map last plot-data)
                         :group-by (map first plot-data)
                         :title "Average Price for Each Route and Group Type"
                         :x-label "Route"
                         :y-label "Average Price"
                         :legend true
                         :series-labels ["group" "family"]
                         :vertical false)
        ; Retrieve the plot object from the Incanter chart
        plot (.getPlot chart)
        ; Retrieve the renderer object from the plot, which is responsible for the visual representation of the chart.
        renderer (.getRenderer plot)

        ; Define the colors we want to use for each series
        salmon-color (java.awt.Color. 250 128 114)
        warm-yellow-color (java.awt.Color. 254 196 127)]

    ;; Set the colors for each series using doto, which is macro that allows to perform side-effecting actions on renderer.
    (doto renderer
      (.setSeriesPaint 0 salmon-color)
      (.setSeriesPaint 1 warm-yellow-color))

    (view chart)))

;; Display the plot. If the Java is installed, the plot will be displayed in a new window.
(plot-data (extract-plot-data data))