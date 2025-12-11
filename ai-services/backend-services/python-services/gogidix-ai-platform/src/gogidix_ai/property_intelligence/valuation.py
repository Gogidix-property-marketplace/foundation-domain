"""
Property Valuation Model

Advanced ML models for property price prediction
with feature engineering and explainability.
"""

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from typing import Dict, List, Optional, Tuple, Any
import logging
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import joblib
import shap
from pathlib import Path

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger

logger = get_logger(__name__)


class PropertyValuationModel:
    """
    Property valuation model using ensemble of ML techniques
    with feature engineering and explainability.
    """

    def __init__(self, model_path: Optional[str] = None):
        """Initialize the property valuation model."""
        self.settings = get_settings()
        self.model_path = model_path or self.settings.PROPERTY_VALUATION_MODEL_PATH
        self.device = torch.device("cuda" if torch.settings.is_gpu_available() else "cpu")

        # Initialize models
        self.gb_model = None
        self.rf_model = None
        self.nn_model = None

        # Feature processors
        self.scalers = {}
        self.encoders = {}
        self.feature_columns = []

        # SHAP explainer
        self.explainer = None

        # Model metadata
        self.model_metadata = {
            "version": "1.0.0",
            "mae": 0.0,
            "mape": 0.0,
            "r2": 0.0,
            "features_used": [],
            "training_data_size": 0,
            "last_updated": None,
        }

        self._initialize_models()

    def _initialize_models(self):
        """Initialize the ML models."""
        # Gradient Boosting Regressor
        self.gb_model = GradientBoostingRegressor(
            n_estimators=200,
            learning_rate=0.05,
            max_depth=6,
            min_samples_split=5,
            min_samples_leaf=2,
            random_state=42,
            subsample=0.8,
        )

        # Random Forest Regressor
        self.rf_model = RandomForestRegressor(
            n_estimators=200,
            max_depth=15,
            min_samples_split=5,
            min_samples_leaf=2,
            random_state=42,
            n_jobs=-1,
        )

        # Neural Network
        self.nn_model = PropertyNeuralNetwork(
            input_size=50,  # Will be updated based on features
            hidden_layers=[128, 64, 32],
            dropout_rate=0.2,
        ).to(self.device)

    def load_model(self):
        """Load trained models from disk."""
        try:
            model_path = Path(self.model_path)
            if not model_path.exists():
                raise FileNotFoundError(f"Model path {self.model_path} does not exist")

            # Load ensemble models
            self.gb_model = joblib.load(model_path / "gradient_boosting.pkl")
            self.rf_model = joblib.load(model_path / "random_forest.pkl")

            # Load neural network
            nn_checkpoint = torch.load(model_path / "neural_network.pth")
            self.nn_model.load_state_dict(nn_checkpoint["model_state_dict"])
            self.nn_model.eval()

            # Load feature processors
            self.scalers = joblib.load(model_path / "scalers.pkl")
            self.encoders = joblib.load(model_path / "encoders.pkl")
            self.feature_columns = joblib.load(model_path / "feature_columns.pkl")

            # Load model metadata
            self.model_metadata = joblib.load(model_path / "metadata.pkl")

            # Initialize SHAP explainer
            self.explainer = shap.TreeExplainer(self.gb_model)

            logger.info(
                "Models loaded successfully",
                path=self.model_path,
                version=self.model_metadata["version"],
                mae=self.model_metadata["mae"],
            )

        except Exception as e:
            logger.error(
                "Failed to load models",
                error=str(e),
                path=self.model_path,
            )
            raise

    def save_model(self):
        """Save trained models to disk."""
        try:
            model_path = Path(self.model_path)
            model_path.mkdir(parents=True, exist_ok=True)

            # Save ensemble models
            joblib.dump(self.gb_model, model_path / "gradient_boosting.pkl")
            joblib.dump(self.rf_model, model_path / "random_forest.pkl")

            # Save neural network
            torch.save(
                {"model_state_dict": self.nn_model.state_dict()},
                model_path / "neural_network.pth",
            )

            # Save feature processors
            joblib.dump(self.scalers, model_path / "scalers.pkl")
            joblib.dump(self.encoders, model_path / "encoders.pkl")
            joblib.dump(self.feature_columns, model_path / "feature_columns.pkl")

            # Save model metadata
            joblib.dump(self.model_metadata, model_path / "metadata.pkl")

            logger.info(
                "Models saved successfully",
                path=self.model_path,
            )

        except Exception as e:
            logger.error(
                "Failed to save models",
                error=str(e),
                path=self.model_path,
            )
            raise

    def train(self, training_data: pd.DataFrame, validation_data: Optional[pd.DataFrame] = None):
        """
        Train the property valuation models.

        Args:
            training_data: Training dataset with features and target
            validation_data: Optional validation dataset
        """
        logger.info(
            "Starting model training",
            training_samples=len(training_data),
            validation_samples=len(validation_data) if validation_data is not None else 0,
        )

        try:
            # Prepare features
            X_train, y_train = self._prepare_data(training_data)

            if validation_data is not None:
                X_val, y_val = self._prepare_data(validation_data, fit_encoders=False)
            else:
                X_val = None
                y_val = None

            # Train Gradient Boosting
            logger.info("Training Gradient Boosting model")
            self.gb_model.fit(X_train, y_train)

            # Train Random Forest
            logger.info("Training Random Forest model")
            self.rf_model.fit(X_train, y_train)

            # Train Neural Network
            logger.info("Training Neural Network model")
            self._train_neural_network(X_train, y_train, X_val, y_val)

            # Create ensemble
            self._create_ensemble(X_train, y_train, X_val, y_val)

            # Initialize SHAP explainer
            logger.info("Initializing SHAP explainer")
            self.explainer = shap.TreeExplainer(self.gb_model)

            # Update model metadata
            self.model_metadata["training_data_size"] = len(training_data)
            self.model_metadata["features_used"] = self.feature_columns
            self.model_metadata["last_updated"] = pd.Timestamp.now().isoformat()

            # Calculate and store performance metrics
            train_predictions = self.predict(X_train.to_dict("records"))
            self.model_metadata["mae"] = mean_absolute_error(y_train, train_predictions)
            self.model_metadata["mape"] = self._calculate_mape(y_train, train_predictions)
            self.model_metadata["r2"] = r2_score(y_train, train_predictions)

            logger.info(
                "Model training completed",
                mae=self.model_metadata["mae"],
                mape=self.model_metadata["mape"],
                r2=self.model_metadata["r2"],
            )

        except Exception as e:
            logger.error(
                "Model training failed",
                error=str(e),
            )
            raise

    def predict(
        self,
        properties: List[Dict[str, Any]],
        include_explanation: bool = False,
    ) -> List[float]:
        """
        Predict property values.

        Args:
            properties: List of property feature dictionaries
            include_explanation: Whether to include SHAP explanations

        Returns:
            List of predicted property values
        """
        try:
            # Convert to DataFrame
            df = pd.DataFrame(properties)
            X = self._prepare_features(df)

            # Get predictions from each model
            gb_pred = self.gb_model.predict(X)
            rf_pred = self.rf_model.predict(X)
            nn_pred = self._predict_nn(X)

            # Ensemble predictions
            ensemble_pred = (
                0.4 * gb_pred +  # Give more weight to gradient boosting
                0.3 * rf_pred +
                0.3 * nn_pred
            )

            results = ensemble_pred.tolist()

            if include_explanation:
                explanations = self.explain_predictions(X)
                results = [
                    {"prediction": pred, "explanation": expl}
                    for pred, expl in zip(results, explanations)
                ]

            return results

        except Exception as e:
            logger.error(
                "Prediction failed",
                error=str(e),
            )
            raise

    def _prepare_data(
        self,
        data: pd.DataFrame,
        target_column: str = "price",
        fit_encoders: bool = True,
    ) -> Tuple[pd.DataFrame, pd.Series]:
        """Prepare data for training."""
        # Feature engineering
        data = self._engineer_features(data)

        # Separate features and target
        features = data.drop(columns=[target_column])
        target = data[target_column]

        # Encode categorical features
        if fit_encoders:
            features = self._encode_features(features, fit=True)
        else:
            features = self._encode_features(features, fit=False)

        # Scale numerical features
        if fit_encoders:
            features = self._scale_features(features, fit=True)
        else:
            features = self._scale_features(features, fit=False)

        # Store feature columns
        if fit_encoders:
            self.feature_columns = features.columns.tolist()

        return features, target

    def _prepare_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Prepare features for prediction."""
        df = self._engineer_features(df)
        df = self._encode_features(df, fit=False)
        df = self._scale_features(df, fit=False)

        # Ensure all expected columns are present
        for col in self.feature_columns:
            if col not in df.columns:
                df[col] = 0

        return df[self.feature_columns]

    def _engineer_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Engineer features from raw data."""
        df = df.copy()

        # Price per square foot
        if "square_feet" in df.columns and "price" in df.columns:
            df["price_per_sqft"] = df["price"] / df["square_feet"]

        # Age of property
        if "year_built" in df.columns:
            current_year = pd.Timestamp.now().year
            df["property_age"] = current_year - df["year_built"]
            df["age_squared"] = df["property_age"] ** 2

        # Room ratios
        if "bedrooms" in df.columns and "bathrooms" in df.columns:
            df["bedroom_bathroom_ratio"] = df["bedrooms"] / (df["bathrooms"] + 1)
            df["total_rooms"] = df["bedrooms"] + df["bathrooms"]

        # Location features
        if "latitude" in df.columns and "longitude" in df.columns:
            # Distance from city center (example)
            city_center_lat, city_center_lon = 37.7749, -122.4194  # San Francisco
            df["distance_from_center"] = np.sqrt(
                (df["latitude"] - city_center_lat) ** 2 +
                (df["longitude"] - city_center_lon) ** 2
            )

        # Interaction terms
        if "square_feet" in df.columns and "bedrooms" in df.columns:
            df["sqft_per_bedroom"] = df["square_feet"] / (df["bedrooms"] + 1)

        # Log transformations for skewed features
        for col in ["square_feet", "lot_size", "price"]:
            if col in df.columns:
                df[f"log_{col}"] = np.log1p(df[col])

        return df

    def _encode_features(self, df: pd.DataFrame, fit: bool = True) -> pd.DataFrame:
        """Encode categorical features."""
        df = df.copy()

        categorical_columns = [
            "property_type",
            "location",
            "neighborhood",
            "architecture_style",
            "heating_type",
            "cooling_type",
        ]

        for col in categorical_columns:
            if col in df.columns:
                if fit:
                    if col not in self.encoders:
                        self.encoders[col] = LabelEncoder()
                    df[col] = self.encoders[col].fit_transform(df[col].astype(str))
                else:
                    if col in self.encoders:
                        # Handle unseen categories
                        df[col] = df[col].astype(str)
                        mask = ~df[col].isin(self.encoders[col].classes_)
                        df.loc[mask, col] = self.encoders[col].classes_[0]
                        df[col] = self.encoders[col].transform(df[col])

        return df

    def _scale_features(self, df: pd.DataFrame, fit: bool = True) -> pd.DataFrame:
        """Scale numerical features."""
        df = df.copy()

        numerical_columns = df.select_dtypes(include=[np.number]).columns

        for col in numerical_columns:
            if fit:
                if col not in self.scalers:
                    self.scalers[col] = StandardScaler()
                df[col] = self.scalers[col].fit_transform(df[[col]]).flatten()
            else:
                if col in self.scalers:
                    df[col] = self.scalers[col].transform(df[[col]]).flatten()

        return df

    def _train_neural_network(
        self,
        X_train: pd.DataFrame,
        y_train: pd.Series,
        X_val: Optional[pd.DataFrame] = None,
        y_val: Optional[pd.Series] = None,
    ):
        """Train the neural network model."""
        # Convert to tensors
        X_train_tensor = torch.FloatTensor(X_train.values).to(self.device)
        y_train_tensor = torch.FloatTensor(y_train.values).to(self.device)

        if X_val is not None and y_val is not None:
            X_val_tensor = torch.FloatTensor(X_val.values).to(self.device)
            y_val_tensor = torch.FloatTensor(y_val.values).to(self.device)

        # Update input size
        self.nn_model.input_size = X_train.shape[1]

        # Training parameters
        criterion = nn.MSELoss()
        optimizer = torch.optim.Adam(
            self.nn_model.parameters(),
            lr=0.001,
            weight_decay=1e-5,
        )
        scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(
            optimizer,
            mode="min",
            factor=0.5,
            patience=10,
        )

        # Training loop
        epochs = 100
        batch_size = 32
        n_batches = len(X_train) // batch_size

        for epoch in range(epochs):
            self.nn_model.train()
            epoch_loss = 0

            for i in range(n_batches):
                start = i * batch_size
                end = start + batch_size

                batch_X = X_train_tensor[start:end]
                batch_y = y_train_tensor[start:end]

                optimizer.zero_grad()
                outputs = self.nn_model(batch_X)
                loss = criterion(outputs.squeeze(), batch_y)
                loss.backward()
                torch.nn.utils.clip_grad_norm_(self.nn_model.parameters(), 1.0)
                optimizer.step()

                epoch_loss += loss.item()

            # Validation
            if X_val is not None and y_val is not None:
                self.nn_model.eval()
                with torch.no_grad():
                    val_outputs = self.nn_model(X_val_tensor)
                    val_loss = criterion(val_outputs.squeeze(), y_val_tensor)
                    scheduler.step(val_loss)

            if epoch % 10 == 0:
                logger.info(
                    f"Neural Network Epoch {epoch}/{epochs}",
                    loss=epoch_loss / n_batches,
                )

    def _predict_nn(self, X: pd.DataFrame) -> np.ndarray:
        """Predict using neural network."""
        self.nn_model.eval()
        X_tensor = torch.FloatTensor(X.values).to(self.device)

        with torch.no_grad():
            predictions = self.nn_model(X_tensor)
            return predictions.cpu().numpy().flatten()

    def _create_ensemble(
        self,
        X_train: pd.DataFrame,
        y_train: pd.Series,
        X_val: Optional[pd.DataFrame] = None,
        y_val: Optional[pd.Series] = None,
    ):
        """Create weighted ensemble of models."""
        # Calculate validation weights based on performance
        weights = {
            "gradient_boosting": 0.4,
            "random_forest": 0.3,
            "neural_network": 0.3,
        }

        if X_val is not None and y_val is not None:
            # Adjust weights based on validation performance
            gb_pred = self.gb_model.predict(X_val)
            rf_pred = self.rf_model.predict(X_val)
            nn_pred = self._predict_nn(X_val)

            gb_score = r2_score(y_val, gb_pred)
            rf_score = r2_score(y_val, rf_pred)
            nn_score = r2_score(y_val, nn_pred)

            total_score = gb_score + rf_score + nn_score
            weights["gradient_boosting"] = gb_score / total_score
            weights["random_forest"] = rf_score / total_score
            weights["neural_network"] = nn_score / total_score

            logger.info(
                "Ensemble weights calculated",
                weights=weights,
            )

    def explain_predictions(
        self,
        X: pd.DataFrame,
        num_samples: int = 100,
    ) -> List[Dict[str, float]]:
        """Generate SHAP explanations for predictions."""
        try:
            # Sample for faster computation
            if len(X) > num_samples:
                X_sample = X.sample(n=num_samples, random_state=42)
            else:
                X_sample = X

            # Calculate SHAP values
            shap_values = self.explainer.shap_values(X_sample)

            # Create explanations
            explanations = []
            for i, row in enumerate(shap_values):
                explanation = {
                    "base_value": self.explainer.expected_value,
                    "features": {},
                }

                for j, col in enumerate(X_sample.columns):
                    explanation["features"][col] = {
                        "value": float(row[j]),
                        "impact": float(row[j]),
                    }

                explanations.append(explanation)

            return explanations

        except Exception as e:
            logger.error(
                "Failed to generate explanations",
                error=str(e),
            )
            return []

    def _calculate_mape(self, y_true: np.ndarray, y_pred: np.ndarray) -> float:
        """Calculate Mean Absolute Percentage Error."""
        mask = y_true != 0
        return np.mean(np.abs((y_true[mask] - y_pred[mask]) / y_true[mask])) * 100

    def get_feature_importance(self) -> Dict[str, float]:
        """Get feature importance from the models."""
        try:
            # Get feature importance from Gradient Boosting
            gb_importance = self.gb_model.feature_importances_
            rf_importance = self.rf_model.feature_importances_

            # Average importance
            importance = (gb_importance + rf_importance) / 2

            return dict(zip(self.feature_columns, importance))

        except Exception as e:
            logger.error(
                "Failed to get feature importance",
                error=str(e),
            )
            return {}


class PropertyNeuralNetwork(nn.Module):
    """Neural network for property valuation."""

    def __init__(
        self,
        input_size: int,
        hidden_layers: List[int],
        dropout_rate: float = 0.2,
    ):
        super().__init__()
        self.input_size = input_size
        self.layers = nn.ModuleList()

        # Build layers
        layer_sizes = [input_size] + hidden_layers + [1]
        for i in range(len(layer_sizes) - 1):
            self.layers.append(nn.Linear(layer_sizes[i], layer_sizes[i + 1]))
            if i < len(layer_sizes) - 2:
                self.layers.append(nn.ReLU())
                self.layers.append(nn.Dropout(dropout_rate))

    def forward(self, x):
        """Forward pass."""
        for layer in self.layers:
            x = layer(x)
        return x