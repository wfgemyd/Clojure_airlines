import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from datetime import datetime

# Load the dataset
file_path = 'sales_team_4.csv'  
data = pd.read_csv(file_path, delimiter=',')

# Split the 'NAME' column into 'FIRST_NAME' and 'SURNAME'
data[['FIRST_NAME', 'SURNAME']] = data['NAME'].str.split(' ', expand=True)
data = data.drop(columns=['NAME'])

# Sort the data
data = data.sort_values(by=['SURNAME', 'DEPARTURE', 'DESTINATION', 'PAID'])

# Function to categorize into families or tours
def categorize_customers(df):
    current_year = datetime.now().year
    df['age'] = current_year - df['YOB']
    df['GROUP_TYPE'] = 'Group'  # Default to group

    for _, group in df.groupby(['DEPARTURE', 'DESTINATION', 'PAID']):
        surnames = group['SURNAME'].unique()
        
        for surname in surnames:
            family = group[group['SURNAME'] == surname]
            adults = family[family['age'] >= 18]
            kids = family[family['age'] < 18]

            if len(adults) >= 2 and len(kids) >= 1:
                df.loc[family.index, 'GROUP_TYPE'] = 'Family'

    return df

# Categorize customers
data = categorize_customers(data)

# Calculate basic statistics for each group type
statistics = data.groupby(['GROUP_TYPE', 'DEPARTURE', 'DESTINATION'])['PAID'].agg(['max', 'min', 'mean', 'count']).reset_index()

# Display results
print("\nStatistics for Each Route and Group Type:")
print(statistics.head(50))



# Calculate success rates for selling the most expensive tickets
def calculate_success_rates(df):
    max_price = df.groupby(['DEPARTURE', 'DESTINATION'])['PAID'].transform('max')
    df['MAX_PRICE_SOLD'] = df['PAID'] == max_price
    success_rates = df[df['MAX_PRICE_SOLD']].groupby(['GROUP_TYPE', 'DEPARTURE', 'DESTINATION']).size() / df.groupby(['GROUP_TYPE', 'DEPARTURE', 'DESTINATION']).size()
    return success_rates.reset_index(name='SUCCESS_RATE')

success_rates = calculate_success_rates(data)

# Predict future sales
def predict_future_sales(df):
    max_price = df.groupby(['DEPARTURE', 'DESTINATION'])['PAID'].transform('max')
    df['HIGH_VALUE_SALE'] = df['PAID'] == max_price
    future_sales = df[df['HIGH_VALUE_SALE']].groupby(['GROUP_TYPE', 'DEPARTURE', 'DESTINATION']).size() / df.groupby(['GROUP_TYPE', 'DEPARTURE', 'DESTINATION']).size()
    return future_sales.reset_index(name='FUTURE_SALES_PROJECTION')

future_sales_prediction = predict_future_sales(data)

# Display results
print("\nStatistics for Each Route and Group Type:")
print(statistics.head(50))
print("\nSuccess Rates for Most Expensive Tickets:")
print(success_rates.head(50))
print("\nFuture Sales Projection:")
print(future_sales_prediction.head(50))

def calculate_purchase_probability_with_existing_analysis(df, increase_percentage, statistics, success_rates, future_sales_prediction):
    # Calculate the new maximum price after the increase
    df['NEW_MAX_PRICE'] = df['PAID'] + (df['PAID'] * increase_percentage / 100)
    
    # Merge with existing statistics, success rates, and future sales predictions
    merged_df = df.merge(statistics[['GROUP_TYPE', 'DEPARTURE', 'DESTINATION', 'max']], on=['GROUP_TYPE', 'DEPARTURE', 'DESTINATION'], how='left')
    merged_df = merged_df.merge(success_rates, on=['GROUP_TYPE', 'DEPARTURE', 'DESTINATION'], how='left')
    merged_df = merged_df.merge(future_sales_prediction, on=['GROUP_TYPE', 'DEPARTURE', 'DESTINATION'], how='left')

    # Fill NaN values in SUCCESS_RATE and FUTURE_SALES_PROJECTION with 0
    merged_df['SUCCESS_RATE'].fillna(0, inplace=True)
    merged_df['FUTURE_SALES_PROJECTION'].fillna(0, inplace=True)

    # Calculate the probability of purchase
    def calculate_probability(row):
        if row['NEW_MAX_PRICE'] <= row['max']:
            return row['SUCCESS_RATE'] * row['FUTURE_SALES_PROJECTION']
        return 0

    merged_df['PROBABILITY_WITH_INCREASE'] = merged_df.apply(calculate_probability, axis=1)
    # Filter out rows where the probability is 0
    filtered_df = merged_df[merged_df['PROBABILITY_WITH_INCREASE'] > 0]

    return filtered_df[['GROUP_TYPE', 'DEPARTURE', 'DESTINATION', 'PROBABILITY_WITH_INCREASE']].drop_duplicates()

# Example usage of the function
purchase_probability_no_increase = calculate_purchase_probability_with_existing_analysis(data, -2, statistics, success_rates, future_sales_prediction)
print(purchase_probability_no_increase.head(50))
