import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from scipy import stats
import statsmodels.api as sm
from scipy.stats import chi2_contingency
import numpy as np
from scipy.stats import ttest_ind


file_path = 'C:/Users/daniel/Downloads/broker_team_4.csv'  
data = pd.read_csv(file_path, delimiter=',')
data.head()

# Split the 'NAME' column into two columns 'FIRST_NAME' and 'SURNAME' using "," as the delimiter
data[['FIRST_NAME', 'SURNAME']] = data['NAME'].str.split(' ', expand=True)

# Dropping the original 'NAME' column as it's no longer needed
data = data.drop(columns=['NAME'])

# Display the modified dataframe to confirm the changes
data.head()


# Sorting the dataframe by 'SURNAME', 'DEPARTURE', and 'PAID'
sorted_data = data.sort_values(by=['SURNAME', 'DEPARTURE', 'PAID'])

# Display the sorted dataframe to confirm the changes
sorted_data.head()


# Defining a function to identify if an individual is an adult or a child
def identify_age_group(row):
    if row['YOB'] <= 2005:  # Considering the dataset is from around 2023
        return 'Adult'
    else:
        return 'Child'


# Applying the function to the dataframe
sorted_data['AGE_GROUP'] = sorted_data.apply(identify_age_group, axis=1)

# Grouping by family: same surname, departure, destination, paid, and a mix of adults and children
family_groups = sorted_data.groupby(['SURNAME', 'DEPARTURE', 'DESTINATION', 'PAID']).apply(
    lambda x: ('Family' if any(x['AGE_GROUP'] == 'Adult') and any(x['AGE_GROUP'] == 'Child') else 'Group')
)
family_groups

# Merging the family and group identifiers into the original dataframe
sorted_data['STATUS'] = sorted_data.set_index(['SURNAME', 'DEPARTURE', 'DESTINATION', 'PAID']).index.map(family_groups)
sorted_data.head()
# Display the updated dataframe to confirm the changes
sorted_data.head(35)

# Counting the number of individuals in groups and families
group_count = sorted_data[sorted_data['STATUS'] == 'Group'].shape[0]
family_count = sorted_data[sorted_data['STATUS'] == 'Family'].shape[0]

# Preparing data for the bar chart
categories = ['Groups', 'Families']
counts = [group_count, family_count]

# Creating the bar chart
plt.figure(figsize=(10, 6))
plt.bar(categories, counts, color=['blue', 'green'])
plt.title('Comparison of Group vs Family Travelers')
plt.xlabel('Category')
plt.ylabel('Number of Individuals')
plt.xticks(categories)
plt.grid(axis='y')

# Display the bar chart
plt.show()

# Creating a new column to identify whether the individual is in a group or family
sorted_data['TRAVEL_TYPE'] = sorted_data.apply(
    lambda x: 'Family' if x['STATUS'] == 'Family' else ('Group' if x['STATUS'] == 'Group' else 'Individual'),
    axis=1
)

# Creating a pivot table to count the number of individuals by travel type, departure, and destination
pivot_table = sorted_data.pivot_table(
    index=['DEPARTURE', 'DESTINATION'], 
    columns='TRAVEL_TYPE', 
    aggfunc='size', 
    fill_value=0
)

# Plotting the data
plt.figure(figsize=(15, 10))
sns.heatmap(pivot_table, annot=True, fmt="d", cmap="YlGnBu", linewidths=.5)
plt.title('Number of Travelers by Type, Departure, and Destination')
plt.ylabel('Departure → Destination')
plt.xlabel('Travel Type')
plt.xticks(rotation=45)

# Show the plot
plt.show()

















