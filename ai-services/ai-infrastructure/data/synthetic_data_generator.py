"""
Synthetic Property Data Generator

Generates realistic property data for local development and testing.
Creates 100K+ records with various property types, locations, and features.
"""

import json
import random
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Dict, List, Tuple
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class PropertyDataGenerator:
    """Generates synthetic real estate property data."""

    def __init__(self, num_records: int = 100000):
        self.num_records = num_records
        self.cities = self._load_cities()
        self.property_types = ["apartment", "house", "condo", "townhouse", "villa", "studio"]
        self.neighborhoods = self._generate_neighborhoods()

    def _load_cities(self) -> List[Dict]:
        """Load major cities with real price multipliers."""
        return [
            {"name": "New York", "state": "NY", "price_multiplier": 2.5, "coordinates": (40.7128, -74.0060)},
            {"name": "Los Angeles", "state": "CA", "price_multiplier": 1.8, "coordinates": (34.0522, -118.2437)},
            {"name": "Chicago", "state": "IL", "price_multiplier": 1.2, "coordinates": (41.8781, -87.6298)},
            {"name": "Houston", "state": "TX", "price_multiplier": 0.9, "coordinates": (29.7604, -95.3698)},
            {"name": "Phoenix", "state": "AZ", "price_multiplier": 0.8, "coordinates": (33.4484, -112.0740)},
            {"name": "Philadelphia", "state": "PA", "price_multiplier": 1.1, "coordinates": (39.9526, -75.1652)},
            {"name": "San Antonio", "state": "TX", "price_multiplier": 0.7, "coordinates": (29.4241, -98.4936)},
            {"name": "San Diego", "state": "CA", "price_multiplier": 1.9, "coordinates": (32.7157, -117.1611)},
            {"name": "Dallas", "state": "TX", "price_multiplier": 0.95, "coordinates": (32.7767, -96.7970)},
            {"name": "San Jose", "state": "CA", "price_multiplier": 2.2, "coordinates": (37.3382, -121.8863)}
        ]

    def _generate_neighborhoods(self) -> Dict[str, List[str]]:
        """Generate neighborhood names for each city."""
        neighborhoods = {}
        for city in self.cities:
            # Generate realistic neighborhood names
            prefixes = ["Downtown", "Uptown", "Midtown", "West", "East", "North", "South"]
            suffixes = ["Side", "Hills", "Heights", "Valley", "Park", "Grove", "Bay", "Village"]

            city_neighborhoods = []
            for _ in range(random.randint(10, 20)):
                if random.random() < 0.3:
                    # Use actual neighborhood name patterns
                    name = random.choice(["Greenwich", "SoHo", "TriBeCa", "Beverly", "Malibu", "Santa Monica"])
                else:
                    prefix = random.choice(prefixes)
                    suffix = random.choice(suffixes)
                    name = f"{prefix} {suffix}"
                city_neighborhoods.append(name)

            neighborhoods[city["name"]] = city_neighborhoods

        return neighborhoods

    def _generate_base_price(self, property_type: str, city: Dict) -> float:
        """Generate base price based on property type and city."""
        base_prices = {
            "apartment": 350000,
            "house": 500000,
            "condo": 400000,
            "townhouse": 450000,
            "villa": 800000,
            "studio": 250000
        }

        base = base_prices[property_type] * city["price_multiplier"]

        # Add random variation
        variation = random.gauss(1.0, 0.2)
        return base * variation

    def _generate_address(self, city: Dict) -> str:
        """Generate a realistic address."""
        street_numbers = list(range(100, 9999, 2))
        street_names = [
            "Main St", "Oak Ave", "Elm St", "Maple Dr", "Cedar Ln",
            "Park Ave", "5th Ave", "Broadway", "Washington St", "Market St",
            "Church St", "State St", "Franklin Ave", "Madison Ave", "Lexington Ave"
        ]

        number = random.choice(street_numbers)
        street = random.choice(street_names)
        neighborhood = random.choice(self.neighborhoods[city["name"]])

        return f"{number} {street}, {neighborhood}, {city['name']}, {city['state']}"

    def _generate_coordinates(self, city: Dict, address: str) -> Tuple[float, float]:
        """Generate coordinates near the city center."""
        lat_center, lon_center = city["coordinates"]

        # Add random offset (approximately 10km radius)
        lat_offset = random.gauss(0, 0.05)
        lon_offset = random.gauss(0, 0.05)

        return lat_center + lat_offset, lon_center + lon_offset

    def _generate_features(self, property_type: str) -> List[str]:
        """Generate property features based on type."""
        all_features = [
            "central_air", "hardwood_floors", "garage", "pool", "garden",
            "balcony", "fireplace", "basement", "attic", "patio",
            "gym", "spa", "theater_room", "wine_cellar", "smart_home",
            "solar_panels", "elevator", "concierge", "doorman", "in_unit_laundry"
        ]

        # Select features based on property type
        if property_type == "villa":
            # More features for luxury properties
            num_features = random.randint(8, 15)
        elif property_type in ["apartment", "condo"]:
            num_features = random.randint(3, 8)
        else:
            num_features = random.randint(4, 10)

        return random.sample(all_features, min(num_features, len(all_features)))

    def _generate_images_urls(self, property_id: str) -> List[str]:
        """Generate fake image URLs for a property."""
        num_images = random.randint(5, 20)
        return [
            f"https://images.gogidix.com/properties/{property_id}/image_{i+1}.jpg"
            for i in range(num_images)
        ]

    def generate_property_record(self) -> Dict:
        """Generate a single property record."""
        property_type = random.choice(self.property_types)
        city = random.choice(self.cities)

        # Generate property details
        bedrooms = {
            "studio": 0,
            "apartment": random.randint(1, 4),
            "condo": random.randint(1, 3),
            "house": random.randint(2, 6),
            "townhouse": random.randint(2, 5),
            "villa": random.randint(3, 8)
        }[property_type]

        bathrooms = bedrooms + random.choice([-1, 0, 0, 1])
        bathrooms = max(1, bathrooms)

        square_feet = {
            "studio": random.randint(300, 800),
            "apartment": random.randint(600, 3000),
            "condo": random.randint(800, 3500),
            "house": random.randint(1200, 6000),
            "townhouse": random.randint(1000, 4000),
            "villa": random.randint(2000, 10000)
        }[property_type]

        year_built = random.randint(1950, 2023)
        lot_size = square_feet * random.uniform(1, 5) if property_type in ["house", "villa"] else None

        # Generate location details
        address = self._generate_address(city)
        latitude, longitude = self._generate_coordinates(city, address)
        neighborhood = address.split(", ")[1]

        # Generate pricing
        base_price = self._generate_base_price(property_type, city)

        # Adjust price based on features
        price_per_sqft = base_price / square_feet

        # Add premium for newer properties
        if year_built > 2010:
            price_per_sqft *= 1.1

        # Add premium for more bedrooms/bathrooms
        if bedrooms >= 4:
            price_per_sqft *= 1.15
        if bathrooms >= 3:
            price_per_sqft *= 1.1

        # Calculate final price
        price = square_feet * price_per_sqft * random.uniform(0.9, 1.1)

        # Generate market information
        days_on_market = int(random.expovariate(1/30))  # Exponential distribution with mean 30 days
        list_date = datetime.now() - timedelta(days=days_on_market)

        # Generate description
        descriptions = [
            f"Beautiful {bedrooms} bedroom, {bathrooms} bathroom {property_type} in {neighborhood}. "
            f"Features {random.choice(['stunning', 'gorgeous', 'modern', 'elegant'])} "
            f"{random.choice(['views', 'finishes', 'amenities', 'layout'])}. "
            f"Located in {random.choice(['prime', 'desirable', 'convenient', 'quiet'])} location.",

            f"Spacious {square_feet} sq ft {property_type} with {bedrooms} bedrooms. "
            f"Perfect for {random.choice(['families', 'professionals', 'students', 'retirees'])}. "
            f"Close to {random.choice(['schools', 'shopping', 'transportation', 'parks'])}.",

            f"Recently {random.choice(['renovated', 'updated', 'remodeled'])} {property_type}. "
            f"Features {random.choice(['hardwood floors', 'granite countertops', 'stainless steel appliances'])}. "
            f"Don't miss this {random.choice(['opportunity', 'gem', 'find', 'deal'])}!"
        ]

        description = random.choice(descriptions)

        # Generate property ID
        property_id = f"PROP_{random.randint(100000, 999999)}"

        # Compile record
        record = {
            "id": property_id,
            "property_type": property_type,
            "address": address,
            "neighborhood": neighborhood,
            "city": city["name"],
            "state": city["state"],
            "latitude": latitude,
            "longitude": longitude,
            "bedrooms": bedrooms,
            "bathrooms": bathrooms,
            "square_feet": square_feet,
            "lot_size": lot_size,
            "year_built": year_built,
            "price": round(price, 2),
            "price_per_sqft": round(price_per_sqft, 2),
            "features": self._generate_features(property_type),
            "description": description,
            "images": self._generate_images_urls(property_id),
            "list_date": list_date.isoformat(),
            "days_on_market": days_on_market,
            "status": random.choice(["active", "pending", "sold"]) if days_on_market > 0 else "active",
            "source": random.choice(["MLS", "Zillow", "Redfin", "Realtor.com", "Direct"]),
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }

        return record

    def generate_dataset(self) -> pd.DataFrame:
        """Generate the complete dataset."""
        logger.info(f"Generating {self.num_records} property records...")

        records = []
        for i in range(self.num_records):
            if (i + 1) % 10000 == 0:
                logger.info(f"Generated {i + 1} records...")

            record = self.generate_property_record()
            records.append(record)

        df = pd.DataFrame(records)
        logger.info(f"Dataset generated successfully! Shape: {df.shape}")

        return df

    def save_dataset(self, df: pd.DataFrame, format: str = "parquet"):
        """Save dataset in specified format."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        if format == "parquet":
            file_path = f"data/properties_{timestamp}.parquet"
            df.to_parquet(file_path, index=False)
        elif format == "csv":
            file_path = f"data/properties_{timestamp}.csv"
            df.to_csv(file_path, index=False)
        elif format == "json":
            file_path = f"data/properties_{timestamp}.json"
            df.to_json(file_path, orient="records", indent=2)

        logger.info(f"Dataset saved to: {file_path}")

        # Also save a smaller sample for quick testing
        sample_df = df.sample(n=min(1000, len(df)), random_state=42)
        sample_path = f"data/properties_sample_1000.{format}"

        if format == "parquet":
            sample_df.to_parquet(sample_path, index=False)
        elif format == "csv":
            sample_df.to_csv(sample_path, index=False)
        elif format == "json":
            sample_df.to_json(sample_path, orient="records", indent=2)

        logger.info(f"Sample dataset saved to: {sample_path}")

        return file_path, sample_path

    def generate_summary_statistics(self, df: pd.DataFrame):
        """Generate and save summary statistics."""
        stats = {
            "total_properties": len(df),
            "property_types": df["property_type"].value_counts().to_dict(),
            "cities": df["city"].value_counts().head(10).to_dict(),
            "price_stats": {
                "min": float(df["price"].min()),
                "max": float(df["price"].max()),
                "mean": float(df["price"].mean()),
                "median": float(df["price"].median()),
                "std": float(df["price"].std())
            },
            "square_feet_stats": {
                "min": int(df["square_feet"].min()),
                "max": int(df["square_feet"].max()),
                "mean": float(df["square_feet"].mean()),
                "median": float(df["square_feet"].median())
            },
            "avg_price_per_sqft_by_city": df.groupby("city")["price_per_sqft"].mean().to_dict(),
            "avg_bedrooms": float(df["bedrooms"].mean()),
            "avg_bathrooms": float(df["bathrooms"].mean()),
            "avg_year_built": float(df["year_built"].mean()),
            "status_distribution": df["status"].value_counts().to_dict()
        }

        # Save statistics
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        stats_path = f"data/properties_statistics_{timestamp}.json"

        with open(stats_path, "w") as f:
            json.dump(stats, f, indent=2)

        logger.info(f"Statistics saved to: {stats_path}")

        # Print summary
        print("\n" + "="*50)
        print("DATASET SUMMARY")
        print("="*50)
        print(f"Total Properties: {stats['total_properties']:,}")
        print(f"Avg Price: ${stats['price_stats']['mean']:,.2f}")
        print(f"Avg Price/Sqft: ${stats['price_stats']['mean'] / stats['square_feet_stats']['mean']:,.2f}")
        print(f"Avg Bedrooms: {stats['avg_bedrooms']:.1f}")
        print(f"Avg Bathrooms: {stats['avg_bathrooms']:.1f}")
        print(f"Avg Year Built: {int(stats['avg_year_built'])}")
        print(f"\nTop 5 Cities:")
        for city, count in list(stats['cities'].items())[:5]:
            print(f"  - {city}: {count:,} properties")

        return stats

def main():
    """Main function to generate synthetic property data."""
    # Create data directory if it doesn't exist
    import os
    os.makedirs("data", exist_ok=True)

    # Initialize generator
    generator = PropertyDataGenerator(num_records=100000)

    # Generate dataset
    df = generator.generate_dataset()

    # Save dataset in multiple formats
    generator.save_dataset(df, format="parquet")
    generator.save_dataset(df, format="csv")
    generator.save_dataset(df, format="json")

    # Generate and save statistics
    stats = generator.generate_summary_statistics(df)

    print("\n✅ Synthetic property dataset generation complete!")
    print(f"Generated {len(df)} properties for training and testing.")

if __name__ == "__main__":
    main()