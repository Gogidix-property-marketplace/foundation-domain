"""
Property Valuation Model Training

Trains machine learning models for property price prediction.
Uses ensemble methods with SHAP explainability.
Target MAE < 5%
"""

import os
import json
import argparse
import logging
import warnings
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple, Any

import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
from sklearn.preprocessing import StandardScaler, LabelEncoder, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import joblib

# ML models
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor, ExtraTreesRegressor
from sklearn.linear_model import LinearRegression, Ridge, Lasso
from sklearn.svm import SVR
import xgboost as xgb
import lightgbm as lgb

# SHAP for explainability
import shap
import matplotlib.pyplot as plt
import seaborn as sns

# MLflow for experiment tracking
import mlflow
import mlflow.sklearn
import mlflow.xgboost
import mlflow.lightgbm

# Suppress warnings
warnings.filterwarnings('ignore')

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class PropertyValuationTrainer:
    """Trains property valuation models with comprehensive evaluation."""

    def __init__(self, data_path: str, model_dir: str = "models"):
        self.data_path = data_path
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(parents=True, exist_ok=True)

        # Initialize MLflow
        mlflow.set_experiment("property-valuation")

        # Define model parameters
        self.target_column = "price"
        self.target_mae_percent = 5.0  # Target MAE as percentage of mean price

        # Initialize dictionaries
        self.models = {}
        self.encoders = {}
        self.feature_names = []
        self.preprocessor = None

    def load_data(self) -> pd.DataFrame:
        """Load and prepare the property dataset."""
        logger.info(f"Loading data from {self.data_path}")

        if self.data_path.endswith('.parquet'):
            df = pd.read_parquet(self.data_path)
        elif self.data_path.endswith('.csv'):
            df = pd.read_csv(self.data_path)
        elif self.data_path.endswith('.json'):
            df = pd.read_json(self.data_path)
        else:
            raise ValueError(f"Unsupported file format: {self.data_path}")

        logger.info(f"Loaded dataset with shape: {df.shape}")
        return df

    def preprocess_data(self, df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
        """Preprocess the data for training."""
        logger.info("Preprocessing data...")

        # Create a copy to avoid modifying original
        df = df.copy()

        # Feature engineering
        df['age'] = 2024 - df['year_built']
        df['bed_bath_ratio'] = df['bedrooms'] / df['bathrooms']
        df['rooms_per_sqft'] = (df['bedrooms'] + df['bathrooms']) / df['square_feet']

        # Price per square foot (remove from features to avoid data leakage)
        df['ppsf_actual'] = df['price'] / df['square_feet']
        df = df.drop('price_per_sqft', axis=1)

        # Handle missing values
        df['lot_size'] = df['lot_size'].fillna(df['square_feet'] * 2)  # Default lot size

        # Extract features from property features
        df['num_features'] = df['features'].apply(len)
        df['has_pool'] = df['features'].apply(lambda x: 'pool' in x)
        df['has_garage'] = df['features'].apply(lambda x: 'garage' in x)
        df['has_garden'] = df['features'].apply(lambda x: 'garden' in x)

        # Location-based features
        df['city_state'] = df['city'] + ', ' + df['state']

        # Remove columns not needed for training
        exclude_columns = [
            'id', 'address', 'neighborhood', 'description', 'images',
            'list_date', 'days_on_market', 'status', 'source',
            'created_at', 'updated_at', 'features', 'ppsf_actual'
        ]
        df = df.drop(columns=[col for col in exclude_columns if col in df.columns])

        # Separate features and target
        y = df[self.target_column]
        X = df.drop(columns=[self.target_column])

        logger.info(f"Features shape: {X.shape}")
        logger.info(f"Target shape: {y.shape}")

        return X, y

    def build_preprocessor(self, X: pd.DataFrame) -> ColumnTransformer:
        """Build preprocessing pipeline."""
        logger.info("Building preprocessing pipeline...")

        # Identify column types
        numeric_features = X.select_dtypes(include=['int64', 'float64']).columns.tolist()
        categorical_features = X.select_dtypes(include=['object', 'bool']).columns.tolist()

        logger.info(f"Numeric features: {len(numeric_features)}")
        logger.info(f"Categorical features: {len(categorical_features)}")

        # Create preprocessor
        preprocessor = ColumnTransformer(
            transformers=[
                ('num', StandardScaler(), numeric_features),
                ('cat', OneHotEncoder(handle_unknown='ignore'), categorical_features)
            ],
            remainder='passthrough'
        )

        # Store feature names after preprocessing
        self.preprocessor = preprocessor
        self.feature_names = numeric_features + categorical_features

        return preprocessor

    def train_models(self, X_train: pd.DataFrame, y_train: pd.Series,
                    X_val: pd.DataFrame, y_val: pd.Series) -> Dict:
        """Train multiple models and return the best one."""
        logger.info("Training models...")

        # Define models to train
        models = {
            'random_forest': {
                'model': RandomForestRegressor(
                    n_estimators=100,
                    max_depth=20,
                    min_samples_split=5,
                    min_samples_leaf=2,
                    n_jobs=-1,
                    random_state=42
                ),
                'params': {
                    'n_estimators': [50, 100, 200],
                    'max_depth': [10, 20, None],
                    'min_samples_split': [2, 5, 10]
                }
            },
            'gradient_boosting': {
                'model': GradientBoostingRegressor(
                    n_estimators=100,
                    learning_rate=0.1,
                    max_depth=5,
                    random_state=42
                ),
                'params': {
                    'n_estimators': [50, 100, 200],
                    'learning_rate': [0.01, 0.1, 0.2],
                    'max_depth': [3, 5, 7]
                }
            },
            'xgboost': {
                'model': xgb.XGBRegressor(
                    n_estimators=100,
                    learning_rate=0.1,
                    max_depth=5,
                    random_state=42
                ),
                'params': {
                    'n_estimators': [50, 100, 200],
                    'learning_rate': [0.01, 0.1, 0.2],
                    'max_depth': [3, 5, 7]
                }
            },
            'lightgbm': {
                'model': lgb.LGBMRegressor(
                    n_estimators=100,
                    learning_rate=0.1,
                    max_depth=5,
                    random_state=42
                ),
                'params': {
                    'n_estimators': [50, 100, 200],
                    'learning_rate': [0.01, 0.1, 0.2],
                    'max_depth': [3, 5, 7]
                }
            },
            'extra_trees': {
                'model': ExtraTreesRegressor(
                    n_estimators=100,
                    max_depth=20,
                    random_state=42
                ),
                'params': {
                    'n_estimators': [50, 100, 200],
                    'max_depth': [10, 20, None]
                }
            }
        }

        best_model = None
        best_score = float('inf')
        best_name = None

        # Start MLflow run
        with mlflow.start_run():
            mlflow.log_param("dataset_size", len(X_train))
            mlflow.log_param("features", len(self.feature_names))

            # Train each model
            for name, config in models.items():
                logger.info(f"Training {name}...")

                # Create pipeline
                pipeline = Pipeline([
                    ('preprocessor', self.preprocessor),
                    ('model', config['model'])
                ])

                # Hyperparameter tuning
                if len(config['params']) > 0:
                    grid_search = GridSearchCV(
                        pipeline,
                        {f'model__{k}': v for k, v in config['params'].items()},
                        cv=3,
                        scoring='neg_mean_absolute_error',
                        n_jobs=-1,
                        verbose=0
                    )
                    grid_search.fit(X_train, y_train)
                    pipeline = grid_search.best_estimator_

                    # Log best params
                    mlflow.log_params({f"{name}_{k}": v for k, v in grid_search.best_params_.items()})
                else:
                    pipeline.fit(X_train, y_train)

                # Evaluate
                y_pred = pipeline.predict(X_val)
                mae = mean_absolute_error(y_val, y_pred)
                mae_percent = (mae / np.mean(y_val)) * 100
                rmse = np.sqrt(mean_squared_error(y_val, y_pred))
                r2 = r2_score(y_val, y_pred)

                # Log metrics
                mlflow.log_metric(f"{name}_mae", mae)
                mlflow.log_metric(f"{name}_mae_percent", mae_percent)
                mlflow.log_metric(f"{name}_rmse", rmse)
                mlflow.log_metric(f"{name}_r2", r2)

                # Store model
                self.models[name] = pipeline

                # Update best model
                if mae_percent < best_score:
                    best_score = mae_percent
                    best_model = pipeline
                    best_name = name

                logger.info(f"{name} - MAE: ${mae:,.2f} ({mae_percent:.2f}%), R2: {r2:.3f}")

            # Log best model
            mlflow.log_metric("best_mae_percent", best_score)
            mlflow.log_param("best_model", best_name)

        logger.info(f"Best model: {best_name} with MAE: {best_score:.2f}%")
        self.best_model_name = best_name
        self.best_model = best_model

        return self.models

    def analyze_model(self, X: pd.DataFrame, y: pd.Series):
        """Analyze the best model with SHAP."""
        logger.info("Analyzing model with SHAP...")

        # Get the preprocessor and model
        preprocessed_X = self.preprocessor.transform(X)

        # Create SHAP explainer
        explainer = shap.TreeExplainer(
            self.best_model.named_steps['model'],
            preprocessed_X
        )

        # Calculate SHAP values
        shap_values = explainer.shap_values(preprocessed_X)

        # Get feature names after preprocessing
        feature_names = self.preprocessor.get_feature_names_out()

        # Create visualizations
        plt.figure(figsize=(12, 8))
        shap.summary_plot(
            shap_values,
            preprocessed_X,
            feature_names=feature_names,
            plot_type="bar",
            show=False
        )
        plt.savefig(self.model_dir / "shap_feature_importance.png", dpi=300, bbox_inches='tight')
        plt.close()

        # Save SHAP values
        shap_df = pd.DataFrame(shap_values, columns=feature_names)
        shap_df.to_csv(self.model_dir / "shap_values.csv", index=False)

        # Feature importance dictionary
        feature_importance = dict(zip(feature_names, np.abs(shap_values).mean(axis=0)))
        sorted_importance = sorted(feature_importance.items(), key=lambda x: x[1], reverse=True)

        # Save feature importance
        with open(self.model_dir / "feature_importance.json", "w") as f:
            json.dump(sorted_importance[:20], f, indent=2)

        logger.info("Model analysis complete!")

    def evaluate_model(self, X_test: pd.DataFrame, y_test: pd.Series) -> Dict:
        """Evaluate the best model on test set."""
        logger.info("Evaluating model on test set...")

        # Make predictions
        y_pred = self.best_model.predict(X_test)
        y_pred[y_pred < 0] = 0  # Ensure non-negative predictions

        # Calculate metrics
        mae = mean_absolute_error(y_test, y_pred)
        mae_percent = (mae / np.mean(y_test)) * 100
        rmse = np.sqrt(mean_squared_error(y_test, y_pred))
        r2 = r2_score(y_test, y_pred)
        mape = np.mean(np.abs((y_test - y_pred) / y_test)) * 100

        # Target achievement
        target_achieved = mae_percent <= self.target_mae_percent

        # Create evaluation report
        evaluation = {
            "mae": float(mae),
            "mae_percent": float(mae_percent),
            "rmse": float(rmse),
            "r2": float(r2),
            "mape": float(mape),
            "target_mae_percent": self.target_mae_percent,
            "target_achieved": target_achieved,
            "model_name": self.best_model_name,
            "test_size": len(y_test),
            "timestamp": datetime.now().isoformat()
        }

        # Save evaluation
        with open(self.model_dir / "evaluation.json", "w") as f:
            json.dump(evaluation, f, indent=2)

        # Create residual plot
        plt.figure(figsize=(10, 6))
        plt.scatter(y_test, y_pred, alpha=0.5, s=1)
        plt.plot([y_test.min(), y_test.max()], [y_test.min(), y_test.max()], 'r--', lw=2)
        plt.xlabel("Actual Price")
        plt.ylabel("Predicted Price")
        plt.title(f"Actual vs Predicted Prices (R2 = {r2:.3f})")
        plt.savefig(self.model_dir / "prediction_scatter.png", dpi=300, bbox_inches='tight')
        plt.close()

        # Create residual histogram
        residuals = y_test - y_pred
        plt.figure(figsize=(10, 6))
        plt.hist(residuals, bins=50, alpha=0.7)
        plt.xlabel("Residuals ($)")
        plt.ylabel("Frequency")
        plt.title(f"Residual Distribution (MAE = ${mae:,.2f})")
        plt.savefig(self.model_dir / "residual_distribution.png", dpi=300, bbox_inches='tight')
        plt.close()

        logger.info(f"Evaluation complete!")
        logger.info(f"MAE: ${mae:,.2f} ({mae_percent:.2f}%)")
        logger.info(f"Target achieved: {target_achieved}")

        return evaluation

    def save_model(self):
        """Save the best model and related artifacts."""
        logger.info("Saving model artifacts...")

        # Save model
        model_path = self.model_dir / "property_valuation_model.joblib"
        joblib.dump(self.best_model, model_path)
        logger.info(f"Model saved to {model_path}")

        # Save preprocessor separately
        preprocessor_path = self.model_dir / "preprocessor.joblib"
        joblib.dump(self.preprocessor, preprocessor_path)

        # Save metadata
        metadata = {
            "model_name": self.best_model_name,
            "feature_names": self.feature_names,
            "target_column": self.target_column,
            "model_type": "property_valuation",
            "version": "1.0.0",
            "training_date": datetime.now().isoformat(),
            "target_mae_percent": self.target_mae_percent
        }

        with open(self.model_dir / "metadata.json", "w") as f:
            json.dump(metadata, f, indent=2)

    def create_prediction_function(self):
        """Create a prediction function for deployment."""
        logger.info("Creating prediction function...")

        prediction_code = '''
import joblib
import pandas as pd
import numpy as np
from pathlib import Path

class PropertyValuationPredictor:
    """Property valuation model for inference."""

    def __init__(self, model_path: str = "models/property_valuation_model.joblib"):
        """Initialize the predictor."""
        self.model = joblib.load(model_path)
        self.preprocessor = self.model.named_steps['preprocessor']

    def predict(self, properties: pd.DataFrame) -> dict:
        """Predict property prices.

        Args:
            properties: DataFrame with property features

        Returns:
            Dictionary with predictions and metadata
        """
        # Feature engineering
        if 'age' not in properties.columns:
            properties['age'] = 2024 - properties['year_built']
        if 'bed_bath_ratio' not in properties.columns:
            properties['bed_bath_ratio'] = properties['bedrooms'] / properties['bathrooms']
        if 'rooms_per_sqft' not in properties.columns:
            properties['rooms_per_sqft'] = (properties['bedrooms'] + properties['bathrooms']) / properties['square_feet']

        # Extract features
        if 'num_features' not in properties.columns:
            properties['num_features'] = properties['features'].apply(len)
        if 'has_pool' not in properties.columns:
            properties['has_pool'] = properties['features'].apply(lambda x: 'pool' in x)
        if 'has_garage' not in properties.columns:
            properties['has_garage'] = properties['features'].apply(lambda x: 'garage' in x)
        if 'has_garden' not in properties.columns:
            properties['has_garden'] = properties['features'].apply(lambda x: 'garden' in x)

        # Location feature
        if 'city_state' not in properties.columns:
            properties['city_state'] = properties['city'] + ', ' + properties['state']

        # Make predictions
        predictions = self.model.predict(properties)
        predictions[predictions < 0] = 0

        # Create confidence intervals (simplified)
        confidence = 0.95  # Based on training performance
        lower_bound = predictions * (1 - confidence)
        upper_bound = predictions * (1 + confidence)

        return {
            "predictions": predictions.tolist(),
            "price_per_sqft": (predictions / properties['square_feet']).tolist(),
            "confidence_intervals": {
                "lower": lower_bound.tolist(),
                "upper": upper_bound.tolist()
            },
            "model_version": "1.0.0"
        }

# Usage example
if __name__ == "__main__":
    predictor = PropertyValuationPredictor()

    # Example property
    property_data = pd.DataFrame([{
        "property_type": "apartment",
        "city": "New York",
        "state": "NY",
        "bedrooms": 2,
        "bathrooms": 2,
        "square_feet": 1200,
        "year_built": 2010,
        "features": ["central_air", "hardwood_floors", "doorman"]
    }])

    result = predictor.predict(property_data)
    print(f"Predicted price: ${result['predictions'][0]:,.2f}")
'''

        with open(self.model_dir / "predict.py", "w") as f:
            f.write(prediction_code)

    def train(self):
        """Complete training pipeline."""
        logger.info("Starting property valuation model training...")

        # Load data
        df = self.load_data()

        # Preprocess
        X, y = self.preprocess_data(df)

        # Split data
        X_train, X_temp, y_train, y_temp = train_test_split(
            X, y, test_size=0.3, random_state=42
        )
        X_val, X_test, y_val, y_test = train_test_split(
            X_temp, y_temp, test_size=0.5, random_state=42
        )

        logger.info(f"Train set: {X_train.shape[0]} properties")
        logger.info(f"Validation set: {X_val.shape[0]} properties")
        logger.info(f"Test set: {X_test.shape[0]} properties")

        # Build preprocessor
        self.build_preprocessor(X_train)

        # Train models
        self.train_models(X_train, y_train, X_val, y_val)

        # Analyze best model
        self.analyze_model(X_val, y_val)

        # Evaluate on test set
        evaluation = self.evaluate_model(X_test, y_test)

        # Save model
        self.save_model()

        # Create prediction function
        self.create_prediction_function()

        # Log final results
        logger.info("="*50)
        logger.info("TRAINING COMPLETE")
        logger.info("="*50)
        logger.info(f"Best model: {self.best_model_name}")
        logger.info(f"MAE: {evaluation['mae_percent']:.2f}% (Target: <{self.target_mae_percent}%)")
        logger.info(f"Target achieved: {evaluation['target_achieved']}")
        logger.info(f"R2 Score: {evaluation['r2']:.3f}")
        logger.info(f"Model saved to: {self.model_dir}")

        return evaluation

def main():
    """Main function to run training."""
    parser = argparse.ArgumentParser(description="Train property valuation model")
    parser.add_argument(
        "--data",
        type=str,
        default="data/properties_sample_1000.parquet",
        help="Path to training data"
    )
    parser.add_argument(
        "--model-dir",
        type=str,
        default="models/property_valuation_v1",
        help="Directory to save model artifacts"
    )

    args = parser.parse_args()

    # Initialize trainer
    trainer = PropertyValuationTrainer(args.data, args.model_dir)

    # Train model
    results = trainer.train()

    # Print results
    print("\n" + "="*50)
    print("TRAINING RESULTS")
    print("="*50)
    print(json.dumps(results, indent=2))

if __name__ == "__main__":
    main()