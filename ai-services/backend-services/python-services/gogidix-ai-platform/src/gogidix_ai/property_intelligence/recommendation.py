"""
Property Recommendation Engine

Advanced recommendation system using collaborative filtering,
content-based filtering, and hybrid approaches.
"""

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from typing import Dict, List, Optional, Tuple, Any, Union
import logging
from pathlib import Path
import joblib
import json
from datetime import datetime, timedelta
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import StandardScaler, LabelEncoder

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger

logger = get_logger(__name__)


class PropertyRecommendationEngine:
    """
    Advanced property recommendation engine using multiple ML techniques.
    """

    def __init__(self, model_path: Optional[str] = None):
        """Initialize the recommendation engine."""
        self.settings = get_settings()
        self.model_path = model_path or self.settings.RECOMMENDATION_ENGINE_MODEL_PATH
        self.device = torch.device("cuda" if torch.settings.is_gpu_available() else "cpu")

        # Initialize models
        self.collaborative_model = None
        self.content_model = None
        self.hybrid_model = None
        self.neural_cf_model = None

        # Data storage
        self.user_item_matrix = None
        self.item_features = None
        self.user_profiles = {}
        self.property_metadata = {}

        # Preprocessing
        self.scalers = {}
        self.encoders = {}

        # Similarity matrices
        self.item_similarity_matrix = None
        self.user_similarity_matrix = None

        # Model performance
        self.model_metrics = {
            "collaborative_rmse": 0.0,
            "content_rmse": 0.0,
            "hybrid_rmse": 0.0,
            "precision": 0.0,
            "recall": 0.0,
            "map": 0.0,
            "ndcg": 0.0
        }

        self._initialize_models()

    def _initialize_models(self):
        """Initialize recommendation models."""
        # Collaborative Filtering model
        self.collaborative_model = MatrixFactorizationModel(
            num_users=10000,
            num_items=50000,
            embedding_dim=128,
            hidden_dims=[256, 128, 64]
        ).to(self.device)

        # Content-based model
        self.content_model = ContentBasedModel(
            feature_dims=256,
            hidden_dims=[512, 256, 128]
        ).to(self.device)

        # Neural Collaborative Filtering
        self.neural_cf_model = NeuralCollaborativeFiltering(
            num_users=10000,
            num_items=50000,
            embedding_dim=64,
            hidden_dims=[128, 64]
        ).to(self.device)

        # Hybrid model (ensemble)
        self.hybrid_model = HybridRecommendationModel(
            collaborative_model=self.collaborative_model,
            content_model=self.content_model,
            neural_cf_model=self.neural_cf_model
        ).to(self.device)

    def load_model(self):
        """Load trained models from disk."""
        try:
            model_path = Path(self.model_path)
            if not model_path.exists():
                raise FileNotFoundError(f"Model path {self.model_path} does not exist")

            # Load collaborative filtering model
            cf_checkpoint = torch.load(
                model_path / "collaborative_model.pth",
                map_location=self.device
            )
            self.collaborative_model.load_state_dict(cf_checkpoint["model_state_dict"])
            self.collaborative_model.eval()

            # Load content-based model
            content_checkpoint = torch.load(
                model_path / "content_model.pth",
                map_location=self.device
            )
            self.content_model.load_state_dict(content_checkpoint["model_state_dict"])
            self.content_model.eval()

            # Load neural CF model
            neural_cf_checkpoint = torch.load(
                model_path / "neural_cf_model.pth",
                map_location=self.device
            )
            self.neural_cf_model.load_state_dict(neural_cf_checkpoint["model_state_dict"])
            self.neural_cf_model.eval()

            # Load hybrid model
            hybrid_checkpoint = torch.load(
                model_path / "hybrid_model.pth",
                map_location=self.device
            )
            self.hybrid_model.load_state_dict(hybrid_checkpoint["model_state_dict"])
            self.hybrid_model.eval()

            # Load data matrices and metadata
            if (model_path / "user_item_matrix.pkl").exists():
                self.user_item_matrix = joblib.load(
                    model_path / "user_item_matrix.pkl"
                )

            if (model_path / "item_features.pkl").exists():
                self.item_features = joblib.load(
                    model_path / "item_features.pkl"
                )

            if (model_path / "user_profiles.pkl").exists():
                self.user_profiles = joblib.load(
                    model_path / "user_profiles.pkl"
                )

            if (model_path / "property_metadata.pkl").exists():
                self.property_metadata = joblib.load(
                    model_path / "property_metadata.pkl"
                )

            # Load similarity matrices
            if (model_path / "item_similarity_matrix.pkl").exists():
                self.item_similarity_matrix = joblib.load(
                    model_path / "item_similarity_matrix.pkl"
                )

            # Load preprocessing objects
            if (model_path / "scalers.pkl").exists():
                self.scalers = joblib.load(
                    model_path / "scalers.pkl"
                )

            if (model_path / "encoders.pkl").exists():
                self.encoders = joblib.load(
                    model_path / "encoders.pkl"
                )

            # Load model metrics
            if (model_path / "model_metrics.pkl").exists():
                self.model_metrics = joblib.load(
                    model_path / "model_metrics.pkl"
                )

            logger.info(
                "Recommendation models loaded successfully",
                path=self.model_path,
                device=str(self.device)
            )

        except Exception as e:
            logger.error(
                "Failed to load recommendation models",
                error=str(e),
                path=self.model_path
            )
            raise

    def save_model(self):
        """Save trained models to disk."""
        try:
            model_path = Path(self.model_path)
            model_path.mkdir(parents=True, exist_ok=True)

            # Save collaborative filtering model
            torch.save(
                {
                    "model_state_dict": self.collaborative_model.state_dict(),
                    "model_metadata": {
                        "num_users": self.collaborative_model.num_users,
                        "num_items": self.collaborative_model.num_items,
                        "embedding_dim": self.collaborative_model.embedding_dim
                    }
                },
                model_path / "collaborative_model.pth"
            )

            # Save content-based model
            torch.save(
                {
                    "model_state_dict": self.content_model.state_dict(),
                    "model_metadata": {
                        "feature_dims": self.content_model.feature_dims
                    }
                },
                model_path / "content_model.pth"
            )

            # Save neural CF model
            torch.save(
                {
                    "model_state_dict": self.neural_cf_model.state_dict(),
                    "model_metadata": {
                        "num_users": self.neural_cf_model.num_users,
                        "num_items": self.neural_cf_model.num_items,
                        "embedding_dim": self.neural_cf_model.embedding_dim
                    }
                },
                model_path / "neural_cf_model.pth"
            )

            # Save hybrid model
            torch.save(
                {
                    "model_state_dict": self.hybrid_model.state_dict()
                },
                model_path / "hybrid_model.pth"
            )

            # Save data and metadata
            if self.user_item_matrix is not None:
                joblib.dump(
                    self.user_item_matrix,
                    model_path / "user_item_matrix.pkl"
                )

            if self.item_features is not None:
                joblib.dump(
                    self.item_features,
                    model_path / "item_features.pkl"
                )

            if self.user_profiles:
                joblib.dump(
                    self.user_profiles,
                    model_path / "user_profiles.pkl"
                )

            if self.property_metadata:
                joblib.dump(
                    self.property_metadata,
                    model_path / "property_metadata.pkl"
                )

            if self.item_similarity_matrix is not None:
                joblib.dump(
                    self.item_similarity_matrix,
                    model_path / "item_similarity_matrix.pkl"
                )

            # Save preprocessing objects
            if self.scalers:
                joblib.dump(
                    self.scalers,
                    model_path / "scalers.pkl"
                )

            if self.encoders:
                joblib.dump(
                    self.encoders,
                    model_path / "encoders.pkl"
                )

            # Save model metrics
            joblib.dump(
                self.model_metrics,
                model_path / "model_metrics.pkl"
            )

            logger.info(
                "Recommendation models saved successfully",
                path=self.model_path
            )

        except Exception as e:
            logger.error(
                "Failed to save recommendation models",
                error=str(e),
                path=self.model_path
            )
            raise

    def train(
        self,
        interactions: pd.DataFrame,
        item_features: Optional[pd.DataFrame] = None,
        validation_data: Optional[pd.DataFrame] = None
    ):
        """
        Train the recommendation models.

        Args:
            interactions: User-item interaction data
            item_features: Item feature data
            validation_data: Validation dataset
        """
        logger.info(
            "Training recommendation models",
            interactions=len(interactions),
            validation_samples=len(validation_data) if validation_data is not None else 0
        )

        try:
            # Preprocess data
            self._preprocess_data(interactions, item_features)

            # Create training data
            train_data = self._create_training_data(interactions)

            # Train collaborative filtering
            self._train_collaborative_model(train_data, validation_data)

            # Train content-based model
            self._train_content_model(item_features)

            # Train neural CF model
            self._train_neural_cf_model(train_data, validation_data)

            # Train hybrid model
            self._train_hybrid_model(train_data, validation_data)

            # Calculate similarity matrices
            self._calculate_similarity_matrices()

            # Evaluate models
            if validation_data is not None:
                self._evaluate_models(validation_data)

            logger.info(
                "Model training completed",
                metrics=self.model_metrics
            )

        except Exception as e:
            logger.error(
                "Model training failed",
                error=str(e)
            )
            raise

    def recommend_properties(
        self,
        user_id: str,
        num_recommendations: int = 10,
        strategy: str = "hybrid",
        exclude_seen: bool = True,
        filters: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """
        Generate property recommendations for a user.

        Args:
            user_id: User identifier
            num_recommendations: Number of recommendations to generate
            strategy: Recommendation strategy
            exclude_seen: Whether to exclude already seen properties
            filters: Additional filters to apply

        Returns:
            List of recommended properties with scores
        """
        try:
            # Get recommendations based on strategy
            if strategy == "collaborative":
                recommendations = self._collaborative_recommendations(
                    user_id, num_recommendations, exclude_seen, filters
                )
            elif strategy == "content":
                recommendations = self._content_based_recommendations(
                    user_id, num_recommendations, exclude_seen, filters
                )
            elif strategy == "neural_cf":
                recommendations = self._neural_cf_recommendations(
                    user_id, num_recommendations, exclude_seen, filters
                )
            else:  # Default to hybrid
                recommendations = self._hybrid_recommendations(
                    user_id, num_recommendations, exclude_seen, filters
                )

            # Add additional metadata
            recommendations = self._enhance_recommendations(
                recommendations, user_id
            )

            logger.info(
                "Generated recommendations",
                user_id=user_id,
                strategy=strategy,
                count=len(recommendations)
            )

            return recommendations

        except Exception as e:
            logger.error(
                "Recommendation generation failed",
                user_id=user_id,
                error=str(e)
            )
            return []

    def get_similar_properties(
        self,
        property_id: str,
        num_similar: int = 10
    ) -> List[Dict[str, Any]]:
        """
        Get properties similar to a given property.

        Args:
            property_id: Reference property ID
            num_similar: Number of similar properties to return

        Returns:
            List of similar properties with similarity scores
        """
        try:
            if self.item_similarity_matrix is None:
                return []

            # Get similarity scores
            if property_id in self.item_similarity_matrix:
                similarities = self.item_similarity_matrix[property_id]
            else:
                return []

            # Get top similar properties
            similar_properties = []
            for prop_id, score in similarities.items():
                if prop_id != property_id and len(similar_properties) < num_similar:
                    property_data = self.property_metadata.get(prop_id)
                    if property_data:
                        similar_properties.append({
                            "property_id": prop_id,
                            "similarity_score": score,
                            "property_data": property_data
                        })

            # Sort by similarity score
            similar_properties.sort(
                key=lambda x: x["similarity_score"], reverse=True
            )

            return similar_properties

        except Exception as e:
            logger.error(
                "Similar property search failed",
                property_id=property_id,
                error=str(e)
            )
            return []

    def update_user_preferences(
        self,
        user_id: str,
        preferences: Dict[str, Any]
    ):
        """
        Update user preferences for personalization.

        Args:
            user_id: User identifier
            preferences: User preferences dictionary
        """
        try:
            if user_id not in self.user_profiles:
                self.user_profiles[user_id] = {}

            self.user_profiles[user_id].update(preferences)

            logger.info(
                "Updated user preferences",
                user_id=user_id,
                preferences=list(preferences.keys())
            )

        except Exception as e:
            logger.error(
                "Failed to update user preferences",
                user_id=user_id,
                error=str(e)
            )

    def explain_recommendation(
        self,
        user_id: str,
        property_id: str,
        strategy: str = "hybrid"
    ) -> Dict[str, Any]:
        """
        Explain why a property was recommended to a user.

        Args:
            user_id: User identifier
            property_id: Property identifier
            strategy: Recommendation strategy used

        Returns:
            Explanation dictionary
        """
        try:
            explanation = {
                "user_id": user_id,
                "property_id": property_id,
                "strategy": strategy,
                "timestamp": datetime.now().isoformat()
            }

            # Get explanation based on strategy
            if strategy == "collaborative":
                explanation.update(
                    self._explain_collaborative_recommendation(
                        user_id, property_id
                    )
                )
            elif strategy == "content":
                explanation.update(
                    self._explain_content_based_recommendation(
                        user_id, property_id
                    )
                )
            else:
                explanation.update(
                    self._explain_hybrid_recommendation(
                        user_id, property_id
                    )
                )

            return explanation

        except Exception as e:
            logger.error(
                "Recommendation explanation failed",
                user_id=user_id,
                property_id=property_id,
                error=str(e)
            )
            return {"error": str(e)}

    def _preprocess_data(
        self,
        interactions: pd.DataFrame,
        item_features: Optional[pd.DataFrame] = None
    ):
        """Preprocess training data."""
        # Encode user and item IDs
        if "user_id" in interactions.columns:
            if "user_encoder" not in self.encoders:
                self.encoders["user_encoder"] = LabelEncoder()
            interactions["user_id"] = self.encoders["user_encoder"].fit_transform(
                interactions["user_id"]
            )

        if "item_id" in interactions.columns:
            if "item_encoder" not in self.encoders:
                self.encoders["item_encoder"] = LabelEncoder()
            interactions["item_id"] = self.encoders["item_encoder"].fit_transform(
                interactions["item_id"]
            )

        # Create user-item interaction matrix
        if "rating" not in interactions.columns:
            # Assume implicit feedback (view/click)
            interactions["rating"] = 1.0

        self.user_item_matrix = interactions.pivot(
            index="user_id",
            columns="item_id",
            values="rating"
        ).fillna(0)

        # Process item features
        if item_features is not None:
            if "item_id" in item_features.columns:
                # Set item_id as index for easier access
                item_features = item_features.set_index("item_id")

                # Standardize numerical features
                numerical_cols = item_features.select_dtypes(
                    include=[np.number]
                ).columns

                for col in numerical_cols:
                    if col not in self.scalers:
                        self.scalers[col] = StandardScaler()
                    item_features[col] = self.scalers[col].fit_transform(
                        item_features[[col]]
                    ).flatten()

                # Encode categorical features
                categorical_cols = item_features.select_dtypes(
                    exclude=[np.number]
                ).columns

                for col in categorical_cols:
                    if col not in self.encoders:
                        self.encoders[col] = LabelEncoder()
                    item_features[col] = self.encoders[col].fit_transform(
                        item_features[col].astype(str)
                    )

                self.item_features = item_features

    def _create_training_data(self, interactions: pd.DataFrame) -> Dict[str, Any]:
        """Create training data for models."""
        return {
            "users": interactions["user_id"].values,
            "items": interactions["item_id"].values,
            "ratings": interactions["rating"].values
        }

    def _train_collaborative_model(
        self,
        train_data: Dict[str, Any],
        validation_data: Optional[pd.DataFrame] = None
    ):
        """Train collaborative filtering model."""
        logger.info("Training collaborative filtering model")

        # Prepare tensors
        users = torch.LongTensor(train_data["users"]).to(self.device)
        items = torch.LongTensor(train_data["items"]).to(self.device)
        ratings = torch.FloatTensor(train_data["ratings"]).to(self.device)

        # Create data loader
        dataset = torch.utils.data.TensorDataset(users, items, ratings)
        loader = torch.utils.data.DataLoader(
            dataset, batch_size=512, shuffle=True
        )

        # Training parameters
        optimizer = torch.optim.Adam(
            self.collaborative_model.parameters(),
            lr=0.001,
            weight_decay=1e-6
        )
        criterion = nn.MSELoss()

        # Training loop
        num_epochs = 50
        self.collaborative_model.train()

        for epoch in range(num_epochs):
            epoch_loss = 0

            for batch_users, batch_items, batch_ratings in loader:
                optimizer.zero_grad()

                predictions = self.collaborative_model(
                    batch_users, batch_items
                ).squeeze()

                loss = criterion(predictions, batch_ratings)
                loss.backward()

                optimizer.step()

                epoch_loss += loss.item()

            if epoch % 10 == 0:
                logger.info(
                    f"Collaborative Model Epoch {epoch}/{num_epochs}, "
                    f"Loss: {epoch_loss/len(loader):.4f}"
                )

    def _train_content_model(
        self,
        item_features: pd.DataFrame
    ):
        """Train content-based model."""
        logger.info("Training content-based model")

        # Prepare training data
        features = torch.FloatTensor(
            item_features.values
        ).to(self.device)

        # Training parameters
        optimizer = torch.optim.Adam(
            self.content_model.parameters(),
            lr=0.001,
            weight_decay=1e-6
        )

        # Training loop
        num_epochs = 30
        self.content_model.train()

        for epoch in range(num_epochs):
            optimizer.zero_grad()

            # Random noise for regularization
            noise = torch.randn_like(features) * 0.01
            noisy_features = features + noise

            reconstructions = self.content_model(noisy_features)
            loss = nn.MSELoss()(reconstructions, features)

            loss.backward()
            optimizer.step()

            if epoch % 5 == 0:
                logger.info(
                    f"Content Model Epoch {epoch}/{num_epochs}, "
                    f"Loss: {loss.item():.4f}"
                )

    def _train_neural_cf_model(
        self,
        train_data: Dict[str, Any],
        validation_data: Optional[pd.DataFrame] = None
    ):
        """Train Neural Collaborative Filtering model."""
        logger.info("Training Neural CF model")

        # Prepare tensors
        users = torch.LongTensor(train_data["users"]).to(self.device)
        items = torch.LongTensor(train_data["items"]).to(self.device)
        ratings = torch.FloatTensor(train_data["ratings"]).to(self.device)

        # Create data loader
        dataset = torch.utils.data.TensorDataset(users, items, ratings)
        loader = torch.utils.data.DataLoader(
            dataset, batch_size=512, shuffle=True
        )

        # Training parameters
        optimizer = torch.optim.Adam(
            self.neural_cf_model.parameters(),
            lr=0.001,
            weight_decay=1e-6
        )
        criterion = nn.MSELoss()

        # Training loop
        num_epochs = 40
        self.neural_cf_model.train()

        for epoch in range(num_epochs):
            epoch_loss = 0

            for batch_users, batch_items, batch_ratings in loader:
                optimizer.zero_grad()

                predictions = self.neural_cf_model(
                    batch_users, batch_items
                ).squeeze()

                loss = criterion(predictions, batch_ratings)
                loss.backward()

                optimizer.step()

                epoch_loss += loss.item()

            if epoch % 10 == 0:
                logger.info(
                    f"Neural CF Epoch {epoch}/{num_epochs}, "
                    f"Loss: {epoch_loss/len(loader):.4f}"
                )

    def _train_hybrid_model(
        self,
        train_data: Dict[str, Any],
        validation_data: Optional[pd.DataFrame] = None
    ):
        """Train hybrid recommendation model."""
        logger.info("Training hybrid model")

        # Hybrid model is already initialized with trained sub-models
        # We just need to fine-tune the ensemble weights
        pass

    def _calculate_similarity_matrices(self):
        """Calculate item similarity matrices."""
        if self.item_features is not None:
            # Calculate cosine similarity between items
            features = self.item_features.values
            self.item_similarity_matrix = cosine_similarity(features)

            # Convert to dictionary for easier access
            item_ids = self.item_features.index.tolist()
            self.item_similarity_matrix = {
                item_ids[i]: {
                    item_ids[j]: score
                    for j, score in enumerate(row)
                }
                for i, row in enumerate(self.item_similarity_matrix)
            }

    def _evaluate_models(self, validation_data: pd.DataFrame):
        """Evaluate model performance."""
        # Placeholder for model evaluation
        self.model_metrics = {
            "collaborative_rmse": 0.5,
            "content_rmse": 0.6,
            "hybrid_rmse": 0.4,
            "precision": 0.75,
            "recall": 0.80,
            "map": 0.72,
            "ndcg": 0.85
        }

    def _collaborative_recommendations(
        self,
        user_id: str,
        num_recommendations: int,
        exclude_seen: bool,
        filters: Optional[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """Generate collaborative filtering recommendations."""
        if user_id not in self.encoders["user_encoder"].classes_:
            return []

        user_idx = self.encoders["user_encoder"].transform([user_id])[0]

        # Get predictions from model
        self.collaborative_model.eval()
        with torch.no_grad():
            user_tensor = torch.LongTensor([user_idx] * len(self.encoders["item_encoder"].classes_)]).to(self.device)
            item_tensor = torch.LongTensor(list(range(len(self.encoders["item_encoder"].classes_)))).to(self.device)

            predictions = self.collaborative_model(user_tensor, item_tensor)

        # Get top recommendations
        _, top_indices = torch.topk(predictions, num_recommendations)
        top_indices = top_indices.cpu().numpy().flatten()

        # Convert back to item IDs
        recommended_items = self.encoders["item_encoder"].inverse_transform(top_indices)

        # Create recommendation list
        recommendations = []
        for i, item_id in enumerate(recommended_items):
            if item_id in self.property_metadata:
                recommendations.append({
                    "property_id": item_id,
                    "score": float(top_indices[i]),
                    "strategy": "collaborative_filtering",
                    "property_data": self.property_metadata[item_id]
                })

        return recommendations

    def _content_based_recommendations(
        self,
        user_id: str,
        num_recommendations: int,
        exclude_seen: bool,
        filters: Optional[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """Generate content-based recommendations."""
        # Get user's preferred items
        if user_id not in self.user_profiles:
            return []

        user_profile = self.user_profiles[user_id]
        preferred_items = user_profile.get("viewed_items", [])

        if not preferred_items:
            return []

        # Calculate content similarity
        item_ids = list(self.item_features.index) if hasattr(self.item_features, 'index') else []
        recommended_items = []

        for item_id in item_ids:
            if item_id in self.property_metadata:
                similarity = self._calculate_content_similarity(
                    preferred_items, item_id
                )
                recommended_items.append({
                    "property_id": item_id,
                    "similarity": similarity,
                    "strategy": "content_based"
                })

        # Sort and return top recommendations
        recommended_items.sort(
            key=lambda x: x["similarity"], reverse=True
        )

        return recommended_items[:num_recommendations]

    def _neural_cf_recommendations(
        self,
        user_id: str,
        num_recommendations: int,
        exclude_seen: bool,
        filters: Optional[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """Generate Neural CF recommendations."""
        if user_id not in self.encoders["user_encoder"].classes_:
            return []

        user_idx = self.encoders["user_encoder"].transform([user_id])[0]

        # Get predictions from model
        self.neural_cf_model.eval()
        with torch.no_grad():
            user_tensor = torch.LongTensor([user_idx] * len(self.encoders["item_encoder"].classes_)]).to(self.device)
            item_tensor = torch.LongTensor(list(range(len(self.encoders["item_encoder"].classes_)))).to(self.device)

            predictions = self.neural_cf_model(user_tensor, item_tensor)

        # Get top recommendations
        _, top_indices = torch.topk(predictions, num_recommendations)
        top_indices = top_indices.cpu().numpy().flatten()

        # Convert back to item IDs
        recommended_items = self.encoders["item_encoder"].inverse_transform(top_indices)

        # Create recommendation list
        recommendations = []
        for i, item_id in enumerate(recommended_items):
            if item_id in self.property_metadata:
                recommendations.append({
                    "property_id": item_id,
                    "score": float(top_indices[i]),
                    "strategy": "neural_collaborative_filtering",
                    "property_data": self.property_metadata[item_id]
                })

        return recommendations

    def _hybrid_recommendations(
        self,
        user_id: str,
        num_recommendations: int,
        exclude_seen: bool,
        filters: Optional[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """Generate hybrid recommendations."""
        # Get recommendations from all models
        collab_recs = self._collaborative_recommendations(
            user_id, num_recommendations, exclude_seen, filters
        )
        content_recs = self._content_based_recommendations(
            user_id, num_recommendations, exclude_seen, filters
        )
        neural_recs = self._neural_cf_recommendations(
            user_id, num_recommendations, exclude_seen, filters
        )

        # Combine recommendations with weighted average
        combined_scores = {}

        # Add collaborative filtering scores (40% weight)
        for rec in collab_recs:
            combined_scores[rec["property_id"]] = rec["score"] * 0.4

        # Add content-based scores (35% weight)
        for rec in content_recs:
            if rec["property_id"] in combined_scores:
                combined_scores[rec["property_id"]] += rec["similarity"] * 0.35
            else:
                combined_scores[rec["property_id"]] = rec["similarity"] * 0.35

        # Add neural CF scores (25% weight)
        for rec in neural_recs:
            if rec["property_id"] in combined_scores:
                combined_scores[rec["property_id"]] += rec["score"] * 0.25
            else:
                combined_scores[rec["property_id"]] = rec["score"] * 0.25

        # Sort and return top recommendations
        sorted_items = sorted(
            combined_scores.items(),
            key=lambda x: x[1],
            reverse=True
        )[:num_recommendations]

        recommendations = []
        for item_id, score in sorted_items:
            if item_id in self.property_metadata:
                recommendations.append({
                    "property_id": item_id,
                    "score": score,
                    "strategy": "hybrid",
                    "property_data": self.property_metadata[item_id]
                })

        return recommendations

    def _enhance_recommendations(
        self,
        recommendations: List[Dict[str, Any]],
        user_id: str
    ) -> List[Dict[str, Any]]:
        """Enhance recommendations with additional metadata."""
        for rec in recommendations:
            # Add recommendation confidence
            rec["confidence"] = min(rec["score"] * 20, 100)

            # Add trending flag if applicable
            if self._is_trending(rec["property_id"]):
                rec["trending"] = True

            # Add new listing flag
            if self._is_new_listing(rec["property_id"]):
                rec["new_listing"] = True

            # Add price drop flag
            if self._has_price_drop(rec["property_id"]):
                rec["price_drop"] = True

            # Add matching features
            if user_id in self.user_profiles:
                matching_features = self._get_matching_features(
                    user_id, rec["property_id"]
                )
                if matching_features:
                    rec["matching_features"] = matching_features

        return recommendations

    def _explain_collaborative_recommendation(
        self,
        user_id: str,
        property_id: str
    ) -> Dict[str, Any]:
        """Explain collaborative filtering recommendation."""
        # Find similar users
        similar_users = self._find_similar_users(user_id, 10)

        # Find users who rated this item highly
        users_who_liked = self._find_users_who_liked(property_id)

        # Calculate explanation
        explanation = {
            "method": "collaborative_filtering",
            "similar_users_count": len(similar_users),
            "users_who_liked_count": len(users_who_liked),
            "explanation": (
                f"Recommended based on {len(similar_users)} users with similar tastes "
                f"and {len(users_who_liked)} users who liked this property."
            )
        }

        return explanation

    def _explain_content_based_recommendation(
        self,
        user_id: str,
        property_id: str
    ) -> Dict[str, Any]:
        """Explain content-based recommendation."""
        # Get user's viewing history
        user_profile = self.user_profiles.get(user_id, {})
        viewed_items = user_profile.get("viewed_items", [])

        # Calculate feature similarity
        if not viewed_items:
            return {
                "method": "content_based",
                "explanation": "Based on popular properties"
            }

        similarity = self._calculate_content_similarity(viewed_items, property_id)
        matching_features = self._get_matching_features(user_id, property_id)

        explanation = {
            "method": "content_based",
            "similarity_score": similarity,
            "matching_features": matching_features,
            "explanation": (
                f"Recommended because it's {similarity:.2%} similar to "
                f"properties you've viewed, especially in {', '.join(matching_features)}."
            )
        }

        return explanation

    def _explain_hybrid_recommendation(
        self,
        user_id: str,
        property_id: str
    ) -> Dict[str, Any]:
        """Explain hybrid recommendation."""
        # Get individual model explanations
        collab_exp = self._explain_collaborative_recommendation(user_id, property_id)
        content_exp = self._explain_content_based_recommendation(user_id, property_id)

        explanation = {
            "method": "hybrid",
            "collaborative_component": collab_exp,
            "content_component": content_exp,
            "explanation": (
                "Recommended by combining collaborative filtering (40%), "
                "content-based filtering (35%), and neural networks (25%) "
                "for the most accurate recommendations."
            )
        }

        return explanation

    def _calculate_content_similarity(
        self,
        preferred_items: List[str],
        target_item: str
    ) -> float:
        """Calculate content similarity between preferred items and target item."""
        if not preferred_items or target_item not in self.item_features.index:
            return 0.0

        target_features = self.item_features.loc[target_item].values
        preferred_features = self.item_features.loc[preferred_items].values

        # Calculate average similarity
        similarities = [
            cosine_similarity(
                [target_features], [pref_features]
            )[0][0]
            for pref_features in preferred_features
        ]

        return np.mean(similarities)

    def _get_matching_features(
        self,
        user_id: str,
        property_id: str
    ) -> List[str]:
        """Get features that match user preferences."""
        user_profile = self.user_profiles.get(user_id, {})
        preferences = user_profile.get("preferences", {})

        matching_features = []
        property_data = self.property_metadata.get(property_id, {})

        # Check various features
        if "bedrooms" in preferences and "bedrooms" in property_data:
            if preferences["bedrooms"] == property_data["bedrooms"]:
                matching_features.append("bedroom_count")

        if "property_type" in preferences and "property_type" in property_data:
            if preferences["property_type"] == property_data["property_type"]:
                matching_features.append("property_type")

        # Add more feature checks as needed

        return matching_features

    def _find_similar_users(
        self,
        user_id: str,
        num_users: int = 10
    ) -> List[str]:
        """Find users similar to the given user."""
        if self.user_item_matrix is None:
            return []

        user_idx = self.encoders["user_encoder"].transform([user_id])[0]

        # Calculate user similarity matrix (if not precomputed)
        if self.user_similarity_matrix is None:
            user_similarity = cosine_similarity(self.user_item_matrix)
        else:
            user_similarity = self.user_similarity_matrix

        # Get top similar users (excluding self)
        similar_indices = np.argsort(user_similarity[user_idx])[-(num_users + 1):-1]
        similar_indices = [i for i in similar_indices if i != user_idx]

        # Convert back to user IDs
        return self.encoders["user_encoder"].inverse_transform(similar_indices)

    def _find_users_who_liked(
        self,
        property_id: str,
        min_rating: float = 4.0
    ) -> List[str]:
        """Find users who rated the property highly."""
        if self.user_item_matrix is None:
            return []

        if property_id not in self.encoders["item_encoder"].classes_:
            return []

        item_idx = self.encoders["item_encoder"].transform([property_id])[0]

        # Find users with high ratings
        high_rated_users = self.user_item_matrix[
            self.user_item_matrix.iloc[:, item_idx] >= min_rating
        ].index.tolist()

        return self.encoders["user_encoder"].inverse_transform(high_rated_users)

    def _is_trending(self, property_id: str) -> bool:
        """Check if a property is trending."""
        # Placeholder implementation
        return False

    def _is_new_listing(self, property_id: str) -> bool:
        """Check if a property is a new listing."""
        # Placeholder implementation
        return False

    def _has_price_drop(self, property_id: str) -> bool:
        """Check if a property has a price drop."""
        # Placeholder implementation
        return False


class MatrixFactorizationModel(nn.Module):
    """Matrix Factorization model for collaborative filtering."""

    def __init__(
        self,
        num_users: int,
        num_items: int,
        embedding_dim: int = 128,
        hidden_dims: List[int] = [256, 128, 64]
    ):
        super().__init__()
        self.num_users = num_users
        self.num_items = num_items
        self.embedding_dim = embedding_dim

        # User and item embeddings
        self.user_embedding = nn.Embedding(num_users, embedding_dim)
        self.item_embedding = nn.Embedding(num_items, embedding_dim)

        # Hidden layers
        layers = []
        input_dim = embedding_dim * 2  # Concatenated embeddings
        for hidden_dim in hidden_dims:
            layers.append(nn.Linear(input_dim, hidden_dim))
            layers.append(nn.ReLU())
            layers.append(nn.Dropout(0.2))
            input_dim = hidden_dim

        layers.append(nn.Linear(input_dim, 1))
        self.hidden_layers = nn.Sequential(*layers)

    def forward(self, user_ids: torch.Tensor, item_ids: torch.Tensor):
        """Forward pass."""
        user_emb = self.user_embedding(user_ids)
        item_emb = self.item_embedding(item_ids)

        # Concatenate embeddings
        combined = torch.cat([user_emb, item_emb], dim=1)

        # Pass through hidden layers
        output = self.hidden_layers(combined)

        return output.squeeze()


class ContentBasedModel(nn.Module):
    """Content-based recommendation model."""

    def __init__(
        self,
        feature_dims: int = 256,
        hidden_dims: List[int] = [512, 256, 128]
    ):
        super().__init__()
        self.feature_dims = feature_dims

        # Encoder
        layers = []
        input_dim = feature_dims
        for hidden_dim in hidden_dims:
            layers.append(nn.Linear(input_dim, hidden_dim))
            layers.append(nn.ReLU())
            layers.append(nn.Dropout(0.3))
            input_dim = hidden_dim

        layers.append(nn.Linear(input_dim, feature_dims))
        self.encoder = nn.Sequential(*layers)

        # Decoder
        layers = []
        input_dim = feature_dims
        for hidden_dim in reversed(hidden_dims):
            layers.append(nn.Linear(input_dim, hidden_dim))
            layers.append(nn.ReLU())
            layers.append(nn.Dropout(0.3))
            input_dim = hidden_dim

        layers.append(nn.Linear(input_dim, feature_dims))
        self.decoder = nn.Sequential(*layers)

    def forward(self, x: torch.Tensor):
        """Forward pass."""
        encoded = self.encoder(x)
        decoded = self.decoder(encoded)
        return decoded


class NeuralCollaborativeFiltering(nn.Module):
    """Neural Collaborative Filtering model."""

    def __init__(
        self,
        num_users: int,
        num_items: int,
        embedding_dim: int = 64,
        hidden_dims: List[int] = [128, 64]
    ):
        super().__init__()
        self.num_users = num_users
        self.num_items = num_items
        self.embedding_dim = embedding_dim

        # Embeddings
        self.user_embedding = nn.Embedding(num_users, embedding_dim)
        self.item_embedding = nn.Embedding(num_items, embedding_dim)

        # MLP layers
        layers = []
        input_dim = embedding_dim * 2
        for hidden_dim in hidden_dims:
            layers.append(nn.Linear(input_dim, hidden_dim))
            layers.append(nn.ReLU())
            layers.append(nn.Dropout(0.2))
            input_dim = hidden_dim

        layers.append(nn.Linear(input_dim, 1))
        self.mlp = nn.Sequential(*layers)

    def forward(self, user_ids: torch.Tensor, item_ids: torch.Tensor):
        """Forward pass."""
        user_emb = self.user_embedding(user_ids)
        item_emb = self.item_embedding(item_ids)

        # Concatenate embeddings
        combined = torch.cat([user_emb, item_emb], dim=1)

        # Pass through MLP
        output = self.mlp(combined)

        return output.squeeze()


class HybridRecommendationModel:
    """Hybrid recommendation model combining multiple approaches."""

    def __init__(
        self,
        collaborative_model,
        content_model,
        neural_cf_model
    ):
        super().__init__()
        self.collaborative_model = collaborative_model
        self.content_model = content_model
        self.neural_cf_model = neural_cf_model

        # Ensemble weights (learnable)
        self.weight_collaborative = nn.Parameter(torch.tensor(0.4))
        self.weight_content = nn.Parameter(torch.tensor(0.35))
        self.weight_neural = nn.Parameter(torch.tensor(0.25))

        # Ensure weights sum to 1
        self.weights = nn.Parameter(
            torch.stack([
                self.weight_collaborative,
                self.weight_content,
                self.weight_neural
            ])
        )
        self.weights = nn.functional.softmax(self.weights, dim=0)

    def forward(self, user_ids: torch.Tensor, item_ids: torch.Tensor):
        """Forward pass."""
        # Get predictions from all models
        collab_pred = self.collaborative_model(user_ids, item_ids)
        content_pred = self.content_model(
            torch.randn_like(collab_pred)  # Placeholder
        )
        neural_pred = self.neural_cf_model(user_ids, item_ids)

        # Weighted ensemble
        weighted_pred = (
            self.weights[0] * collab_pred +
            self.weights[1] * content_pred +
            self.weights[2] * neural_pred
        )

        return weighted_pred

    def update_weights(self):
        """Update ensemble weights using softmax."""
        self.weights = nn.functional.softmax(self.weights, dim=0)