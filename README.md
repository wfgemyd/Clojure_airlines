# Clojure Airlines 🛫

This project forms a part of the Symbolic Computation course in the BSc (Hons) Computing program. Presented Clojure program efficiently reads and processes flight data, offering personalized flight tickets, which are tailored to increase the company's profit. 

## Overview

The program is structured as follows:

- **Data Parsing**: Utilizes clojure.data.csv to parse flight information from the "Flights_ICA1.csv" dataset.

- **Graph Representation**: Constructs a graph where cities are vertices and flights are edges, representing the entire flight network.

- **User Input**: Accepts user's input including current location, destination, name and date of birth. The user's input may be a group of people. 

- **Flight Plan Generation**: Employs a breadth-first search (BFS) algorithm to curate flight plans meeting user's criteria.

- **Sorting and Optimization**: Sorts flight plans by cost and flight count, eliminating duplicates.

- **User Analysis**: Categorizes the user into one of the groups, either _"family"_ or _"group"_.

- **Historical Flights Sales Analysis**: Conducts a basic analysis of the historical sales and returns predicted budget of the user based on the route they chose and the group type they belong to. 


## Usage 

1. Ensure that JDK, Clojure and Leiningen are installed on your system. Visit [Oracle website](https://www.oracle.com/java/technologies/downloads), [Clojure's official website](https://clojure.org/guides/install_clojure) and [Leiningen's website](https://leiningen.org/#install) for installation instructions.

2. Clone this repository and navigate to the project directory.

  ```
  git clone https://github.com/wfgemyd/Clojure_airlines.git
  ```

3.  Make sure that the required dependencies are connected correctly.

```
lein deps 
lein deps :tree
```

4. Make sure that you have Java Installed for Incanter graphs to display correctly. 

5. Ensure that the dataset "Flights_ICA1.csv" file is in the "src/data/airlines" directory. Alternatively, you can use your own dataset. Ensure it's in CSV format and contains necessary flight data fields.

6. Input the departure city, destination city and vector with information about people in your group into the **main-check-broker** function. Each person's information should be inside of a separate vector, which should contain the name of the person and year of birth. Alternatively, you can use the broker_team_2.csv file to test the function. 

7. Run the program using Clojure REPL. 

8. If you want the program to give an output, uncomment the print lines of the **main-check-broker** and **get-stats-return-budget** functions. 

## Example

### Scenario
Finding flights from Rijeka to Brno for the following group of people:

  ```
Harry Adams, 1982
Elsie Adams, 1992
Alfie Adams, 2017
Elsie Adams, 2014 
  ```

### Results
```
PREDICTED BUDGET IS:  728.0
TICKET PRICE IS:  700
WILL BE SOLD TO CUSTOMER:  700.0
PROFIT IS:  0.0
```

## Features 

- **Custom Flight Search**: Input your travel details and let our algorithm present you with the best ticket price options. 
    
- **Multiple Destinations**: Multiple route options can be explored to reach your desired location.
  
- **Graph representation of flight networks**: The flight connections and cities are presented in a graph data structure for a clear representation of the flight network.

- **Historical Data Analysis Visualization**: The data visualizations are presented in the code using the **Incanter** library and in the **assets** folder.
  
- **Customizable and Extensible Code Structure**: Tailor the application to your needs or contribute to its growth!

## Limitations and Future Improvements 

- **Single Data Source**: The program currently supports only one CSV data file as a data source for flight information.
  
- **Error Handling**: The program currently does not handle missing fields or invalid inputs (e.g., non-existent cities, negative budget values) in the dataset gracefully. Ensure that the CSV file has no missing or invalid values.
  
- **Limited Filtering Options**: Currently, our program is designed to output the most and least expensive flight connections.

- **Localization**: Currently, the program only supports datasets and inputs in English. Internationalization features, such as multi-language support, are not yet implemented.

## References 

- Higginbotham, D. (2015). Clojure for the Brave and True: Learn the Ultimate Language and Become a Better Programmer. No Starch Press. ISBN: 9781593275914. url: https://books.google.cz/books?id=mQLPCgAAQBAJ
  
- ClojureDocs (2023). Community-powered Clojure documentation. url: https://clojuredocs.org/

- Cormen, T.H., C.E. Leiserson, R.L. Rivest, and C. Stein (2001). Introduction To Algorithms. Mit Electrical Engineering and Computer Science. MIT Press. isbn: 9780262032933. url: https://books.google.cz/books?id=NLngYyWFl_YC

- Meier, C. (2015). Living Clojure: An Introduction and Training Plan for Developers. O’Reilly Media. isbn: 9781491909294. URL: https://books.google.cz/books?id=b4odCAAAQBAJ.
  
- Karumanchi, N. (2011). Data Structures and Algorithms Made Easy: Data Structure and Algorithmic Puzzles. CareerMonk Publications. ISBN: 9780615459813. url: https://books.google.cz/books?id=FPIznwEACAAJ

## License

Clojure Airlines is licensed under the Eclipse Public License 2.0. See the LICENSE file for more details.
