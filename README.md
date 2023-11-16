# Clojure Airlines
This Clojure program efficiently reads and processes flight data, offering personalized flight plans tailored to your preferences.

## Overview

The program is structured as follows:

- **Data Parsing**: Utilizes clojure.data.csv to parse flight information from the "Flights_ICA1.csv" dataset.

- **Graph Representation**: Constructs a graph where cities are vertices and flights are edges, representing the entire flight network.

- **User Input**: Accepts user preferences including current location, destination, budget, and maximum flight count.

- **Flight Plan Generation**: Employs a breadth-first search (BFS) algorithm to curate flight plans meeting user's criteria.

- **Sorting and Optimization**: Sorts flight plans by cost and flight count, eliminating duplicates.


## Usage

1. Ensure Clojure and Leiningen are installed on your system. Visit [Clojure's official website](https://clojure.org/guides/install_clojure) and [Leiningen's website](https://leiningen.org/#install) for installation instructions.

2. Clone this repository and navigate to the project directory.

  ```
  git clone https://github.com/wfgemyd/Clojure_airlines.git
  ```

3.  Make sure that the required dependencies are connected correctly.

```
lein deps 
lein deps :tree
```

4. Ensure that the dataset "Flights_ICA1.csv" file is in the "src/airlines" directory. Alternatively, you can use your own dataset. Ensure it's in CSV format and contains necessary flight data fields.

5. Run the program using Clojure REPL. 

5. Follow the prompts to provide your current location, destination, budget, and maximum number of flights.

6. The program will display flight plans that meet your criteria, showing the most expensive and cheapest options.

## Example

### Scenario
Finding flights from New York to Los Angeles with a budget of $500 and a maximum of 2 flights.

### Results
- **Plan 1 (Most Expensive)**:

  Path: New York --> Chicago (200) --> Los Angeles (300)

  Total Cost: 500

  Amount of flights: 2

- **Plan 2 (Cheapest)**:

  Path: New York --> Los Angeles (500)

  Total Cost: 500

  Amount of flights: 1

## Features
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
