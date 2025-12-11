# Property Dataset

This directory contains the synthetic property dataset for training and testing the AI models.

## Dataset Overview

- **Total Records**: 100,000 properties
- **Time Period**: Current listings from 2020-2024
- **Geographic Coverage**: 10 major US cities
- **Property Types**: Apartments, houses, condos, townhouses, villas, studios
- **Features**: 45+ property features and amenities

## Data Schema

| Column | Type | Description |
|--------|------|-------------|
| id | string | Unique property identifier |
| property_type | string | Type of property (apartment, house, etc.) |
| address | string | Full property address |
| neighborhood | string | Neighborhood name |
| city | string | City name |
| state | string | State abbreviation |
| latitude | float | GPS latitude |
| longitude | float | GPS longitude |
| bedrooms | integer | Number of bedrooms |
| bathrooms | integer | Number of bathrooms |
| square_feet | integer | Living area in square feet |
| lot_size | float | Lot size in square feet (optional) |
| year_built | integer | Year property was built |
| price | float | Listing price in USD |
| price_per_sqft | float | Price per square foot |
| features | array | List of property features |
| description | text | Property description |
| images | array | URLs to property images |
| list_date | datetime | Date property was listed |
| days_on_market | integer | Days property has been on market |
| status | string | Listing status (active, pending, sold) |
| source | string | Data source (MLS, Zillow, etc.) |
| created_at | datetime | Record creation timestamp |
| updated_at | datetime | Record update timestamp |

## Files

### Dataset Files
- `properties_YYYYMMDD_HHMMSS.parquet` - Main dataset (Parquet format)
- `properties_YYYYMMDD_HHMMSS.csv` - Main dataset (CSV format)
- `properties_YYYYMMDD_HHMMSS.json` - Main dataset (JSON format)
- `properties_sample_1000.*` - Sample dataset with 1,000 records

### Reference Files
- `synthetic_data_generator.py` - Script to generate synthetic data
- `properties_statistics_YYYYMMDD_HHMMSS.json` - Dataset statistics
- `data_schema.json` - Detailed data schema definition
- `README.md` - This file

## Data Statistics

### Price Distribution
- **Minimum**: $45,000
- **Maximum**: $4,500,000
- **Average**: $485,000
- **Median**: $380,000

### Property Distribution
- **Apartments**: 35%
- **Houses**: 30%
- **Condos**: 20%
- **Townhouses**: 10%
- **Villas**: 3%
- **Studios**: 2%

### Geographic Coverage
1. New York, NY - 20,000 properties
2. Los Angeles, CA - 15,000 properties
3. Chicago, IL - 10,000 properties
4. Houston, TX - 8,000 properties
5. Phoenix, AZ - 7,000 properties
6. And 5 additional cities...

## Usage Examples

### Loading the Dataset (Python)

```python
import pandas as pd

# Load Parquet file (recommended)
df = pd.read_parquet("data/properties_20240125_120000.parquet")

# Load CSV file
df = pd.read_csv("data/properties_20240125_120000.csv")

# Load JSON file
df = pd.read_json("data/properties_20240125_120000.json")

# Display basic info
print(df.info())
print(df.describe())
```

### Loading with Dask for Large Datasets

```python
import dask.dataframe as dd

# Load with Dask for out-of-core processing
ddf = dd.read_parquet("data/properties_20240125_120000.parquet")

# Compute statistics
print(ddf.price.mean().compute())
```

### SQL Database Import

```python
from sqlalchemy import create_engine
import pandas as pd

# Create database connection
engine = create_engine('postgresql://user:pass@localhost/gogidix_ai')

# Load and import data
df = pd.read_parquet("data/properties_20240125_120000.parquet")
df.to_sql('properties', engine, if_exists='replace', index=False)
```

### Machine Learning Preparation

```python
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import pandas as pd

# Load dataset
df = pd.read_parquet("data/properties_20240125_120000.parquet")

# Feature engineering
df['price_per_sqft'] = df['price'] / df['square_feet']
df['age'] = 2024 - df['year_built']
df['bed_bath_ratio'] = df['bedrooms'] / df['bathrooms']

# Encode categorical variables
df_encoded = pd.get_dummies(df, columns=['property_type', 'city', 'state'])

# Prepare features and target
features = df_encoded.drop(['price', 'id', 'address', 'description', 'images'], axis=1)
target = df_encoded['price']

# Split dataset
X_train, X_test, y_train, y_test = train_test_split(
    features, target, test_size=0.2, random_state=42
)

print(f"Training set: {X_train.shape[0]} properties")
print(f"Test set: {X_test.shape[0]} properties")
```

## Data Quality

The synthetic dataset is designed to be realistic and consistent:

- **Price correlations**: Prices correlate with location, size, and features
- **Geographic consistency**: Prices reflect real market differences between cities
- **Feature logic**: Properties have appropriate features for their type
- **Temporal consistency**: Newer properties tend to have higher prices
- **Market dynamics**: Days on market follows realistic distribution

## Data Generation

To regenerate or modify the dataset:

```bash
cd data
python synthetic_data_generator.py
```

You can modify the generator to:
- Change the number of records
- Add new cities or neighborhoods
- Adjust price distributions
- Add new property features
- Modify correlation patterns

## Data Limitations

1. **Synthetic Data**: This is simulated data, not real listings
2. **Simplified Features**: Real properties have more complex features
3. **Market Dynamics**: Simplified market behavior
4. **Image URLs**: Placeholder URLs, not real images
5. **Temporal Scope**: Limited to current market conditions

## Integration with AI Models

### Property Valuation Model
```python
# Example features for valuation model
valuation_features = [
    'square_feet', 'bedrooms', 'bathrooms', 'year_built',
    'property_type', 'city', 'neighborhood',
    'price_per_sqft', 'lot_size', 'features_count'
]
```

### Image Analysis
Images are simulated. Replace with real property images:
1. Collect real property images
2. Upload to image storage
3. Update image URLs in dataset
4. Train computer vision models

### Recommendation Engine
```python
# User interaction data (to be collected)
user_interactions = {
    'user_id': 'user123',
    'property_id': 'PROP_123456',
    'interaction_type': 'view',  # view, save, contact
    'timestamp': '2024-01-25T10:30:00Z'
}
```

## Data Privacy

- No personally identifiable information (PII)
- All addresses are synthetic
- Phone numbers and emails are not included
- Owner information is anonymized

## Updates

The dataset should be updated:
- **Weekly**: Add new synthetic listings
- **Monthly**: Adjust price trends
- **Quarterly**: Refresh market conditions
- **Annually**: Complete dataset regeneration

## Support

For questions about the dataset:
- Create an issue in the repository
- Contact the data team at data@gogidix.com
- Check the documentation at docs.gogidix.com/data