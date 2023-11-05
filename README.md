# clojure_airlines
This Clojure program reads flight data from a CSV file, creates a graph representation of the flight network, and allows users to find flight plans based on their preferences.

## Overview

The program is structured as follows:

- It parses flight data from a CSV file named "Flights_ICA1.csv" using the `clojure.data.csv` library.

- The flight network is represented as a graph with vertices and edges, where vertices represent cities and edges represent flights between cities.

- Users can input their current location, destination, budget, and the maximum number of flights they are willing to take.

- The program then uses a breadth-first search (BFS) algorithm to find flight plans that meet the user's criteria.

- The found flight plans are sorted by total cost and the number of flights taken.

- Duplicate paths are removed, and the program displays the most expensive and cheapest valid plans.

## Usage

1. Make sure you have Clojure installed on your system.

2. Clone this repository and navigate to the project directory.

3. Ensure that the "Flights_ICA1.csv" file is in the "src/airlines" directory, or replace it with your own CSV file with flight data.

4. Run the program.

lein run


5. Follow the prompts to provide your current location, destination, budget, and maximum number of flights.

6. The program will display flight plans that meet your criteria, showing the most expensive and cheapest options.

## Example

Here's an example of using the program:
Searching for plans from New York to Los Angeles with a budget of 500 and a maximum of 2 flights:

Plan 1 (Most Expensive):
Path: New York --> Chicago (200) --> Los Angeles (300)
Total Cost: 500
Amount of flights: 2

Plan 2 (Cheapest):
Path: New York --> Los Angeles (500)
Total Cost: 500
Amount of flights: 1

Features
List the key features of your program, such as:

Flight data parsing from CSV files.
Graph-based representation of flight networks.
User-friendly input prompts and customization options.
Search algorithm for finding flight plans based on user criteria.
Sorting and filtering of flight plans.
Duplicate path removal.
Customizable and extensible code structure.

## Dependencies

- Clojure: The program is written in Clojure, so you need to have Clojure installed to run it.

- `clojure.data.csv`: This library is used for parsing CSV files. You can add it to your project's dependencies.

Future Improvements
In future updates, we plan to:

Enhance the user interface for a more interactive experience.
Implement additional search algorithms for finding flight plans.
Support more advanced filtering and sorting options for flight plans.
Handle various data formats and sources for flight information.
Stay tuned for updates and improvements to make your flight planning experience even better!

Thank you for using our Flight Planner in Clojure. We hope it helps you find the perfect flight plans for your travel needs.

Happy travels! ✈️🌍🌆
