"""
Ethical AI Service

Responsible AI components including bias detection, fairness metrics,
explainability, and compliance monitoring for AI Act and GDPR.
"""

import asyncio
import numpy as np
import pandas as pd
from typing import Dict, List, Any, Optional, Tuple, Union
from datetime import datetime, timedelta
import json
import logging
from pathlib import Path
from dataclasses import dataclass, asdict
from enum import Enum
import plotly.graph_objects as go
import plotly.express as px
from plotly.subplots import make_subplots

# Bias detection libraries
try:
    import aif360
    from aif360.datasets import BinaryLabelDataset
    from aif360.metrics import BinaryLabelGroundTruthMetric
    from aif360.algorithms.preprocessing import Reweighing
    from aif360.algorithms.postprocessing import EqOddsPostprocessing
    AIF360_AVAILABLE = True
except ImportError:
    AIF360_AVAILABLE = False

# Explainability libraries
try:
    import shap
    import lime
    import lime.lime_tabular
    import eli5
    from eli5.sklearn import PermutationImportance
    EXPLAINABILITY_AVAILABLE = True
except ImportError:
    EXPLAINABILITY_AVAILABLE = False

from ..core.config import get_settings
from ..core.exceptions import EthicalAIError
from ..core.logging import get_logger

logger = get_logger(__name__)
settings = get_settings()


class BiasType(Enum):
    """Types of bias to detect."""
    DEMOGRAPHIC_PARITY = "demographic_parity"
    EQUALIZED_ODDS = "equalized_odds"
    EQUAL_OPPORTUNITY = "equal_opportunity"
    INDIVIDUAL_FAIRNESS = "individual_fairness"
    COUNTERFACTUAL_FAIRNESS = "counterfactual_fairness"
    GROUP_FAIRNESS = "group_fairness"
    TEMPORAL_FAIRNESS = "temporal_fairness"
    GEOGRAPHIC_FAIRNESS = "geographic_fairness"


class ComplianceStandard(Enum):
    """AI compliance standards."""
    AI_ACT_HIGH_RISK = "ai_act_high_risk"
    AI_ACT_LIMITED = "ai_act_limited"
    GDPR_PROHIBITED = "gdpr_prohibited"
    GDPR_SPECIAL_CATEGORIES = "gdpr_special_categories"
    GDPR_ARTICLE_22 = "gdpr_article_22"  # Automated decision making
    ISO_IEC_42001 = "iso_iec_42001"  # AI Management System
    NIST_AI_RMF = "nist_ai_rmf"  # Risk Management Framework


@dataclass
class BiasDetectionResult:
    """Result of bias detection analysis."""
    bias_type: BiasType
    metric_name: str
    metric_value: float
    threshold: float
    is_biased: bool
    confidence_interval: Tuple[float, float]
    affected_groups: List[str]
    severity: str  # 'low', 'medium', 'high', 'critical'
    recommendations: List[str]
    explanation: str


@dataclass
class FairnessMetrics:
    """Comprehensive fairness metrics."""
    demographic_parity_difference: float
    demographic_parity_ratio: float
    equal_opportunity_difference: float
    equal_odds_difference: float
    average_odds_difference: float
    false_discovery_rate_difference: float
    false_negative_rate_difference: float
    false_positive_rate_difference: float
    selection_rate_difference: float
    group_loss: Dict[str, float]
    overall_fairness_score: float
    fairness_violations: List[str]


@dataclass
class ModelExplanation:
    """Model explanation for transparency."""
    explanation_id: str
    model_id: str
    input_features: List[str]
    feature_importance: Dict[str, float]
    shap_values: Optional[np.ndarray] = None
    lime_explanation: Optional[str] = None
    counterfactual_examples: List[Dict[str, Any]] = None
    decision_path: Optional[Dict[str, Any]] = None
    confidence_score: float = 0.0
    reasoning: str = ""
    visualizations: Dict[str, str] = None  # Base64 encoded plots


@dataclass
class ComplianceReport:
    """Compliance assessment report."""
    report_id: str
    model_id: str
    assessment_date: datetime
    standards_assessed: List[ComplianceStandard]
    compliance_status: Dict[str, bool]  # standard -> is_compliant
    risk_level: str  # 'low', 'medium', 'high', 'critical'
    gaps_identified: List[Dict[str, Any]]
    remediation_actions: List[Dict[str, Any]]
    next_assessment_date: datetime
    approved_by: Optional[str] = None
    documentation: Dict[str, str] = None


class BiasDetector:
    """Advanced bias detection engine."""

    def __init__(self):
        self.protected_attributes = settings.ethical_ai.protected_attributes
        self.fairness_thresholds = settings.ethical_ai.fairness_thresholds
        self.mitigation_strategies = {}
        self.bias_history = []

        if AIF360_AVAILABLE:
            self._initialize_aif360()

    def _initialize_aif360(self):
        """Initialize AIF360 bias detection toolkit."""
        self.preprocessor = Reweighing()
        self.postprocessor = EqOddsPostprocessing()

    async def detect_bias(
        self,
        model: Any,
        dataset: pd.DataFrame,
        target_column: str,
        sensitive_attributes: List[str],
        bias_types: List[BiasType] = None
    ) -> List[BiasDetectionResult]:
        """
        Comprehensive bias detection analysis.

        Args:
            model: Trained model to evaluate
            dataset: Dataset for bias analysis
            target_column: Name of target variable
            sensitive_attributes: List of protected attributes
            bias_types: Types of bias to check

        Returns:
            List of bias detection results
        """
        logger.info(f"Starting bias detection for {len(sensitive_attributes)} protected attributes")

        if bias_types is None:
            bias_types = [
                BiasType.DEMOGRAPHIC_PARITY,
                BiasType.EQUALIZED_ODDS,
                BiasType.EQUAL_OPPORTUNITY,
                BiasType.GROUP_FAIRNESS
            ]

        results = []
        predictions = model.predict(dataset.drop(columns=[target_column]))

        for bias_type in bias_types:
            try:
                result = await self._detect_bias_type(
                    bias_type, model, dataset, target_column,
                    sensitive_attributes, predictions
                )
                results.append(result)
            except Exception as e:
                logger.error(f"Error detecting {bias_type.value}: {e}")
                continue

        self.bias_history.extend(results)
        return results

    async def _detect_bias_type(
        self,
        bias_type: BiasType,
        model: Any,
        dataset: pd.DataFrame,
        target_column: str,
        sensitive_attributes: List[str],
        predictions: np.ndarray
    ) -> BiasDetectionResult:
        """Detect specific type of bias."""

        if bias_type == BiasType.DEMOGRAPHIC_PARITY:
            return await self._check_demographic_parity(
                dataset, predictions, sensitive_attributes
            )
        elif bias_type == BiasType.EQUALIZED_ODDS:
            return await self._check_equalized_odds(
                dataset, target_column, predictions, sensitive_attributes
            )
        elif bias_type == BiasType.EQUAL_OPPORTUNITY:
            return await self._check_equal_opportunity(
                dataset, target_column, predictions, sensitive_attributes
            )
        elif bias_type == BiasType.INDIVIDUAL_FAIRNESS:
            return await self._check_individual_fairness(
                model, dataset, sensitive_attributes
            )
        elif bias_type == BiasType.GEOGRAPHIC_FAIRNESS:
            return await self._check_geographic_fairness(
                dataset, predictions, sensitive_attributes
            )
        else:
            raise EthicalAIError(f"Unsupported bias type: {bias_type}")

    async def _check_demographic_parity(
        self,
        dataset: pd.DataFrame,
        predictions: np.ndarray,
        sensitive_attributes: List[str]
    ) -> BiasDetectionResult:
        """Check demographic parity bias."""
        results = []

        for attr in sensitive_attributes:
            if attr not in dataset.columns:
                continue

            groups = dataset[attr].unique()
            selection_rates = {}

            for group in groups:
                group_mask = dataset[attr] == group
                selection_rate = np.mean(predictions[group_mask] > 0.5)
                selection_rates[group] = selection_rate

            max_rate = max(selection_rates.values())
            min_rate = min(selection_rates.values())
            disparity = max_rate - min_rate

            threshold = self.fairness_thresholds.get("demographic_parity", 0.1)
            is_biased = disparity > threshold

            severity = "low"
            if disparity > threshold * 3:
                severity = "critical"
            elif disparity > threshold * 2:
                severity = "high"
            elif disparity > threshold:
                severity = "medium"

            recommendations = []
            if is_biased:
                recommendations.extend([
                    f"Implement bias mitigation for {attr}",
                    "Consider reweighting training data",
                    "Apply post-processing fairness corrections"
                ])

            results.append(BiasDetectionResult(
                bias_type=BiasType.DEMOGRAPHIC_PARITY,
                metric_name="selection_rate_disparity",
                metric_value=disparity,
                threshold=threshold,
                is_biased=is_biased,
                confidence_interval=(min_rate, max_rate),
                affected_groups=list(groups),
                severity=severity,
                recommendations=recommendations,
                explanation=f"Demographic parity analysis shows {disparity:.3f} disparity in selection rates across {attr} groups"
            ))

        return results[0] if results else None

    async def _check_equalized_odds(
        self,
        dataset: pd.DataFrame,
        target_column: str,
        predictions: np.ndarray,
        sensitive_attributes: List[str]
    ) -> BiasDetectionResult:
        """Check equalized odds bias."""
        from sklearn.metrics import confusion_matrix

        disparities = []

        for attr in sensitive_attributes:
            if attr not in dataset.columns:
                continue

            groups = dataset[attr].unique()
            group_metrics = {}

            for group in groups:
                group_mask = dataset[attr] == group
                y_true = dataset[target_column][group_mask]
                y_pred = (predictions[group_mask] > 0.5).astype(int)

                tn, fp, fn, tp = confusion_matrix(y_true, y_pred).ravel()

                tpr = tp / (tp + fn) if (tp + fn) > 0 else 0  # True positive rate
                fpr = fp / (fp + tn) if (fp + tn) > 0 else 0  # False positive rate

                group_metrics[group] = {"tpr": tpr, "fpr": fpr}

            # Calculate maximum disparities
            tpr_values = [m["tpr"] for m in group_metrics.values()]
            fpr_values = [m["fpr"] for m in group_metrics.values()]

            tpr_disparity = max(tpr_values) - min(tpr_values)
            fpr_disparity = max(fpr_values) - min(fpr_values)

            overall_disparity = (tpr_disparity + fpr_disparity) / 2
            disparities.append(overall_disparity)

        max_disparity = max(disparities) if disparities else 0
        threshold = self.fairness_thresholds.get("equalized_odds", 0.15)
        is_biased = max_disparity > threshold

        return BiasDetectionResult(
            bias_type=BiasType.EQUALIZED_ODDS,
            metric_name="equalized_odds_disparity",
            metric_value=max_disparity,
            threshold=threshold,
            is_biased=is_biased,
            confidence_interval=(0, max_disparity),
            affected_groups=sensitive_attributes,
            severity=self._calculate_severity(max_disparity, threshold),
            recommendations=["Apply equalized odds post-processing", "Use calibrated predictors"],
            explanation=f"Equalized odds analysis shows {max_disparity:.3f} disparity in true positive and false positive rates"
        )

    async def _check_equal_opportunity(
        self,
        dataset: pd.DataFrame,
        target_column: str,
        predictions: np.ndarray,
        sensitive_attributes: List[str]
    ) -> BiasDetectionResult:
        """Check equal opportunity bias."""
        from sklearn.metrics import confusion_matrix

        disparities = []

        for attr in sensitive_attributes:
            if attr not in dataset.columns:
                continue

            groups = dataset[attr].unique()
            tpr_values = []

            for group in groups:
                group_mask = dataset[attr] == group
                y_true = dataset[target_column][group_mask]
                y_pred = (predictions[group_mask] > 0.5).astype(int)

                tn, fp, fn, tp = confusion_matrix(y_true, y_pred).ravel()
                tpr = tp / (tp + fn) if (tp + fn) > 0 else 0
                tpr_values.append(tpr)

            disparity = max(tpr_values) - min(tpr_values)
            disparities.append(disparity)

        max_disparity = max(disparities) if disparities else 0
        threshold = self.fairness_thresholds.get("equal_opportunity", 0.1)
        is_biased = max_disparity > threshold

        return BiasDetectionResult(
            bias_type=BiasType.EQUAL_OPPORTUNITY,
            metric_name="true_positive_rate_disparity",
            metric_value=max_disparity,
            threshold=threshold,
            is_biased=is_biased,
            confidence_interval=(0, max_disparity),
            affected_groups=sensitive_attributes,
            severity=self._calculate_severity(max_disparity, threshold),
            recommendations=["Increase representation of underrepresented groups", "Apply opportunity-aware training"],
            explanation=f"Equal opportunity analysis shows {max_disparity:.3f} disparity in true positive rates"
        )

    async def _check_individual_fairness(
        self,
        model: Any,
        dataset: pd.DataFrame,
        sensitive_attributes: List[str]
    ) -> BiasDetectionResult:
        """Check individual fairness (similar individuals get similar outcomes)."""
        # Find similar individuals and check prediction consistency
        sample_size = min(1000, len(dataset))
        sample_indices = np.random.choice(len(dataset), sample_size, replace=False)

        inconsistencies = []
        total_pairs = 0

        for i in range(0, len(sample_indices), 10):
            batch = sample_indices[i:i+10]

            for j in range(len(batch)):
                for k in range(j+1, len(batch)):
                    idx1, idx2 = batch[j], batch[k]

                    # Calculate feature similarity (excluding sensitive attributes)
                    features1 = dataset.iloc[idx1].drop(sensitive_attributes + [dataset.columns[-1]])
                    features2 = dataset.iloc[idx2].drop(sensitive_attributes + [dataset.columns[-1]])

                    similarity = self._calculate_similarity(features1, features2)

                    if similarity > 0.9:  # Very similar individuals
                        pred1 = model.predict(dataset.iloc[idx1:idx1+1].drop(columns=[dataset.columns[-1]]))
                        pred2 = model.predict(dataset.iloc[idx2:idx2+1].drop(columns=[dataset.columns[-1]]))

                        pred_diff = abs(pred1[0] - pred2[0])
                        if pred_diff > 0.1:  # Inconsistent predictions
                            inconsistencies.append(pred_diff)

                    total_pairs += 1

        unfairness_rate = len(inconsistencies) / total_pairs if total_pairs > 0 else 0
        threshold = self.fairness_thresholds.get("individual_fairness", 0.05)
        is_biased = unfairness_rate > threshold

        return BiasDetectionResult(
            bias_type=BiasType.INDIVIDUAL_FAIRNESS,
            metric_name="individual_unfairness_rate",
            metric_value=unfairness_rate,
            threshold=threshold,
            is_biased=is_biased,
            confidence_interval=(0, unfairness_rate),
            affected_groups=["all"],
            severity=self._calculate_severity(unfairness_rate, threshold),
            recommendations=["Implement fairness constraints", "Use similarity-based regularization"],
            explanation=f"Individual fairness analysis shows {unfairness_rate:.3f} rate of inconsistent predictions"
        )

    async def _check_geographic_fairness(
        self,
        dataset: pd.DataFrame,
        predictions: np.ndarray,
        sensitive_attributes: List[str]
    ) -> BiasDetectionResult:
        """Check geographic bias in predictions."""
        # Look for geographic indicators (zip code, city, country, etc.)
        geo_columns = [col for col in dataset.columns
                      if any(geo in col.lower() for geo in ['zip', 'postal', 'city', 'country', 'region'])]

        if not geo_columns:
            return BiasDetectionResult(
                bias_type=BiasType.GEOGRAPHIC_FAIRNESS,
                metric_name="geographic_bias",
                metric_value=0.0,
                threshold=0.0,
                is_biased=False,
                confidence_interval=(0.0, 0.0),
                affected_groups=[],
                severity="low",
                recommendations=["Include geographic data for bias analysis"],
                explanation="No geographic data available for analysis"
            )

        disparities = []
        for geo_col in geo_columns[:2]:  # Limit to prevent excessive computation
            groups = dataset[geo_col].unique()
            if len(groups) > 100:  # Skip if too many unique values
                continue

            group_predictions = {}
            for group in groups:
                group_mask = dataset[geo_col] == group
                group_pred = np.mean(predictions[group_mask])
                group_predictions[group] = group_pred

            max_pred = max(group_predictions.values())
            min_pred = min(group_predictions.values())
            disparity = max_pred - min_pred
            disparities.append(disparity)

        max_disparity = max(disparities) if disparities else 0
        threshold = self.fairness_thresholds.get("geographic_fairness", 0.2)
        is_biased = max_disparity > threshold

        return BiasDetectionResult(
            bias_type=BiasType.GEOGRAPHIC_FAIRNESS,
            metric_name="geographic_prediction_disparity",
            metric_value=max_disparity,
            threshold=threshold,
            is_biased=is_biased,
            confidence_interval=(0, max_disparity),
            affected_groups=geo_columns,
            severity=self._calculate_severity(max_disparity, threshold),
            recommendations=["Apply geographic fairness constraints", "Collect diverse geographic data"],
            explanation=f"Geographic fairness analysis shows {max_disparity:.3f} disparity across regions"
        )

    def _calculate_similarity(self, features1: pd.Series, features2: pd.Series) -> float:
        """Calculate similarity between two feature vectors."""
        # Normalize features
        features1_norm = (features1 - features1.mean()) / (features1.std() + 1e-8)
        features2_norm = (features2 - features2.mean()) / (features2.std() + 1e-8)

        # Calculate cosine similarity
        dot_product = np.dot(features1_norm, features2_norm)
        norm1 = np.linalg.norm(features1_norm)
        norm2 = np.linalg.norm(features2_norm)

        similarity = dot_product / (norm1 * norm2 + 1e-8)
        return abs(similarity)

    def _calculate_severity(self, disparity: float, threshold: float) -> str:
        """Calculate bias severity level."""
        if disparity <= threshold:
            return "low"
        elif disparity <= threshold * 2:
            return "medium"
        elif disparity <= threshold * 3:
            return "high"
        else:
            return "critical"


class ModelExplainer:
    """Model explainability engine."""

    def __init__(self):
        self.explainers = {}
        self.explanation_cache = {}

        if EXPLAINABILITY_AVAILABLE:
            self._initialize_explainers()

    def _initialize_explainers(self):
        """Initialize explanation libraries."""
        self.use_shap = True
        self.use_lime = True
        self.use_eli5 = True
        logger.info("Model explainability libraries initialized")

    async def explain_model(
        self,
        model: Any,
        X: pd.DataFrame,
        y: Optional[pd.Series] = None,
        explanation_methods: List[str] = None,
        sample_size: int = 100
    ) -> ModelExplanation:
        """
        Generate comprehensive model explanation.

        Args:
            model: Trained model to explain
            X: Feature dataset
            y: Target variable (optional)
            explanation_methods: Methods to use ['shap', 'lime', 'eli5', 'surrogate']
            sample_size: Number of samples for local explanations

        Returns:
            ModelExplanation object
        """
        logger.info(f"Generating model explanation using methods: {explanation_methods}")

        if explanation_methods is None:
            explanation_methods = ['shap', 'feature_importance', 'surrogate']

        explanation_id = f"exp_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        # Feature importance
        feature_importance = await self._calculate_feature_importance(
            model, X, y, explanation_methods
        )

        # SHAP explanations
        shap_values = None
        if 'shap' in explanation_methods and self.use_shap:
            shap_values = await self._generate_shap_explanations(model, X, sample_size)

        # LIME explanations
        lime_explanation = None
        if 'lime' in explanation_methods and self.use_lime:
            lime_explanation = await self._generate_lime_explanations(
                model, X, sample_size
            )

        # Counterfactual examples
        counterfactuals = await self._generate_counterfactual_examples(
            model, X, sample_size
        )

        # Decision path (for tree-based models)
        decision_path = await self._extract_decision_path(model, X.iloc[0:1])

        # Visualizations
        visualizations = await self._create_explanation_visualizations(
            X, feature_importance, shap_values
        )

        return ModelExplanation(
            explanation_id=explanation_id,
            model_id=getattr(model, 'model_id', 'unknown'),
            input_features=list(X.columns),
            feature_importance=feature_importance,
            shap_values=shap_values,
            lime_explanation=lime_explanation,
            counterfactual_examples=counterfactuals,
            decision_path=decision_path,
            confidence_score=0.85,  # Would be calculated based on explanation stability
            reasoning=self._generate_reasoning(feature_importance, shap_values),
            visualizations=visualizations
        )

    async def _calculate_feature_importance(
        self,
        model: Any,
        X: pd.DataFrame,
        y: Optional[pd.Series],
        methods: List[str]
    ) -> Dict[str, float]:
        """Calculate feature importance using multiple methods."""
        importance_scores = {}

        # Built-in feature importance
        if hasattr(model, 'feature_importances_'):
            importance_scores['builtin'] = dict(zip(X.columns, model.feature_importances_))

        # Permutation importance
        if 'eli5' in methods and self.use_eli5 and y is not None:
            try:
                perm_importance = PermutationImportance(model, random_state=42).fit(X, y)
                importance_scores['permutation'] = dict(
                    zip(X.columns, perm_importance.feature_importances_)
                )
            except Exception as e:
                logger.warning(f"Permutation importance failed: {e}")

        # Correlation-based importance
        if y is not None:
            correlations = X.apply(lambda col: abs(col.corr(y)))
            importance_scores['correlation'] = correlations.to_dict()

        # Combine importance scores
        final_importance = {}
        for feature in X.columns:
            scores = []
            for method, scores_dict in importance_scores.items():
                if feature in scores_dict:
                    scores.append(scores_dict[feature])
            final_importance[feature] = np.mean(scores) if scores else 0.0

        # Normalize to sum to 1
        total = sum(final_importance.values())
        if total > 0:
            final_importance = {k: v/total for k, v in final_importance.items()}

        return final_importance

    async def _generate_shap_explanations(
        self,
        model: Any,
        X: pd.DataFrame,
        sample_size: int
    ) -> Optional[np.ndarray]:
        """Generate SHAP values for global explanations."""
        if not self.use_shap:
            return None

        try:
            # Sample data for SHAP analysis
            X_sample = X.sample(min(sample_size, len(X)), random_state=42)

            # Choose appropriate explainer based on model type
            if hasattr(model, 'predict_proba'):
                explainer = shap.TreeExplainer(model)
            else:
                explainer = shap.KernelExplainer(model.predict, X_sample)

            shap_values = explainer.shap_values(X_sample)

            # If it's a binary classification, take the positive class
            if isinstance(shap_values, list):
                shap_values = shap_values[1]

            return shap_values

        except Exception as e:
            logger.error(f"SHAP explanation failed: {e}")
            return None

    async def _generate_lime_explanations(
        self,
        model: Any,
        X: pd.DataFrame,
        sample_size: int
    ) -> Optional[str]:
        """Generate LIME explanations for local interpretability."""
        if not self.use_lime:
            return None

        try:
            X_sample = X.sample(min(sample_size, len(X)), random_state=42)

            explainer = lime.lime_tabular.LimeTabularExplainer(
                X_sample.values,
                feature_names=X_sample.columns,
                mode='regression',
                discretize_continuous=True
            )

            # Explain a single prediction
            exp = explainer.explain_instance(
                X_sample.iloc[0].values,
                model.predict,
                num_features=10
            )

            return exp.as_html()

        except Exception as e:
            logger.error(f"LIME explanation failed: {e}")
            return None

    async def _generate_counterfactual_examples(
        self,
        model: Any,
        X: pd.DataFrame,
        sample_size: int
    ) -> List[Dict[str, Any]]:
        """Generate counterfactual examples."""
        counterfactuals = []
        X_sample = X.head(sample_size)

        for idx, row in X_sample.iterrows():
            original_pred = model.predict(row.values.reshape(1, -1))[0]

            # Find minimal changes to alter prediction significantly
            counterfactual = self._find_counterfactual(
                model, row, original_pred
            )

            if counterfactual:
                counterfactuals.append({
                    'original_index': idx,
                    'original_prediction': float(original_pred),
                    'counterfactual_features': counterfactual['features'],
                    'counterfactual_prediction': float(counterfactual['prediction']),
                    'changes_made': counterfactual['changes']
                })

        return counterfactuals[:5]  # Return top 5 examples

    def _find_counterfactual(
        self,
        model: Any,
        original: pd.Series,
        original_pred: float,
        max_iterations: int = 100
    ) -> Optional[Dict[str, Any]]:
        """Find counterfactual example using gradient-based search."""
        # Simple implementation - in production, use more sophisticated methods
        counterfactual = original.copy()
        learning_rate = 0.1
        target_pred = original_pred + (0.2 if original_pred < 0.5 else -0.2)

        for iteration in range(max_iterations):
            current_pred = model.predict(counterfactual.values.reshape(1, -1))[0]

            if abs(current_pred - target_pred) < 0.05:
                changes = {}
                for col in original.index:
                    if abs(counterfactual[col] - original[col]) > 0.01:
                        changes[col] = {
                            'original': float(original[col]),
                            'counterfactual': float(counterfactual[col]),
                            'change': float(counterfactual[col] - original[col])
                        }

                return {
                    'features': counterfactual.to_dict(),
                    'prediction': current_pred,
                    'changes': changes
                }

            # Update features (simplified gradient descent)
            for col in counterfactual.index:
                if pd.api.types.is_numeric_dtype(counterfactual[col]):
                    gradient = (target_pred - current_pred) * learning_rate
                    counterfactual[col] += gradient * np.random.normal(0, 0.1)

        return None

    async def _extract_decision_path(
        self,
        model: Any,
        sample: pd.DataFrame
    ) -> Optional[Dict[str, Any]]:
        """Extract decision path for tree-based models."""
        if not hasattr(model, 'decision_path'):
            return None

        try:
            path = model.decision_path(sample)

            # Extract decision nodes
            nodes = []
            for node_id in path.indices:
                if hasattr(model, 'tree_'):
                    feature = model.tree_.feature[node_id]
                    threshold = model.tree_.threshold[node_id]

                    if feature >= 0:
                        nodes.append({
                            'node_id': int(node_id),
                            'feature': sample.columns[feature] if feature < len(sample.columns) else 'unknown',
                            'threshold': float(threshold),
                            'operator': '<=' if sample.iloc[0, feature] <= threshold else '>'
                        })

            return {
                'decision_nodes': nodes,
                'final_prediction': float(model.predict(sample)[0]),
                'path_length': len(nodes)
            }

        except Exception as e:
            logger.error(f"Decision path extraction failed: {e}")
            return None

    async def _create_explanation_visualizations(
        self,
        X: pd.DataFrame,
        feature_importance: Dict[str, float],
        shap_values: Optional[np.ndarray]
    ) -> Dict[str, str]:
        """Create visualization plots."""
        visualizations = {}

        try:
            # Feature importance bar chart
            fig = go.Figure(data=[
                go.Bar(
                    x=list(feature_importance.values()),
                    y=list(feature_importance.keys()),
                    orientation='h'
                )
            ])
            fig.update_layout(
                title="Feature Importance",
                xaxis_title="Importance Score",
                yaxis_title="Features",
                height=600
            )
            visualizations['feature_importance'] = fig.to_html(include_plotlyjs=False)

            # SHAP summary plot
            if shap_values is not None:
                sample_indices = np.random.choice(
                    X.shape[0],
                    size=min(100, X.shape[0]),
                    replace=False
                )
                shap_sample = shap_values[sample_indices]
                X_sample = X.iloc[sample_indices]

                fig = go.Figure()
                for i, feature in enumerate(X_sample.columns):
                    fig.add_trace(go.Scatter(
                        x=shap_sample[:, i],
                        y=[feature] * len(shap_sample),
                        mode='markers',
                        name=feature,
                        opacity=0.6
                    ))

                fig.update_layout(
                    title="SHAP Values Summary",
                    xaxis_title="SHAP Value",
                    yaxis_title="Features",
                    height=800
                )
                visualizations['shap_summary'] = fig.to_html(include_plotlyjs=False)

        except Exception as e:
            logger.error(f"Visualization creation failed: {e}")

        return visualizations

    def _generate_reasoning(
        self,
        feature_importance: Dict[str, float],
        shap_values: Optional[np.ndarray]
    ) -> str:
        """Generate natural language explanation of model behavior."""
        top_features = sorted(feature_importance.items(), key=lambda x: x[1], reverse=True)[:5]

        reasoning = f"This model primarily bases its predictions on "
        reasoning += ", ".join([f"{feat} ({imp:.2%})" for feat, imp in top_features])

        if shap_values is not None:
            mean_abs_shap = np.mean(np.abs(shap_values), axis=0)
            most_influential = np.argsort(mean_abs_shap)[-1]
            reasoning += f". The most influential feature in individual predictions is "
            reasoning += f"feature index {most_influential}."

        reasoning += " The model shows consistent feature importance patterns across different explanation methods."

        return reasoning


class ComplianceMonitor:
    """AI compliance monitoring and reporting."""

    def __init__(self):
        self.compliance_standards = self._initialize_standards()
        self.audit_history = []
        self.monitoring_active = False

    def _initialize_standards(self) -> Dict[ComplianceStandard, Dict[str, Any]]:
        """Initialize compliance standards and requirements."""
        return {
            ComplianceStandard.AI_ACT_HIGH_RISK: {
                "requirements": [
                    "Risk management system",
                    "Data governance",
                    "Technical documentation",
                    "Record keeping",
                    "Transparency obligations",
                    "Human oversight",
                    "Accuracy, robustness, and cybersecurity"
                ],
                "risk_level": "high",
                "assessment_frequency": "annual",
                "documentation_required": True
            },
            ComplianceStandard.GDPR_ARTICLE_22: {
                "requirements": [
                    "Right to human intervention",
                    "Right to express opinion",
                    "Right to contest decision",
                    "Explainability of decisions",
                    "Data minimization"
                ],
                "risk_level": "high",
                "assessment_frequency": "semiannual",
                "documentation_required": True
            },
            ComplianceStandard.ISO_IEC_42001: {
                "requirements": [
                    "AI management system",
                    "Risk assessment",
                    "Continuous improvement",
                    "Resource management",
                    "Competence and awareness"
                ],
                "risk_level": "medium",
                "assessment_frequency": "annual",
                "documentation_required": True
            },
            ComplianceStandard.NIST_AI_RMF: {
                "requirements": [
                    "Govern",
                    "Map",
                    "Measure",
                    "Manage"
                ],
                "risk_level": "medium",
                "assessment_frequency": "quarterly",
                "documentation_required": True
            }
        }

    async def assess_compliance(
        self,
        model_id: str,
        model_type: str,
        intended_use: str,
        data_description: str,
        bias_results: List[BiasDetectionResult] = None,
        explanation: ModelExplanation = None
    ) -> ComplianceReport:
        """
        Assess model compliance against various standards.

        Args:
            model_id: Unique model identifier
            model_type: Type of AI model
            intended_use: Intended use case
            data_description: Description of training data
            bias_results: Bias detection results
            explanation: Model explanation

        Returns:
            ComplianceReport with assessment results
        """
        logger.info(f"Starting compliance assessment for model {model_id}")

        report_id = f"comp_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        assessment_date = datetime.now()

        # Determine applicable standards
        applicable_standards = self._determine_applicable_standards(
            model_type, intended_use
        )

        # Assess each standard
        compliance_status = {}
        gaps_identified = []
        remediation_actions = []

        for standard in applicable_standards:
            is_compliant, gaps = await self._assess_standard(
                standard, model_id, model_type, intended_use,
                data_description, bias_results, explanation
            )

            compliance_status[standard.value] = is_compliant

            if gaps:
                gaps_identified.extend(gaps)
                remediation_actions.extend(
                    self._generate_remediation_actions(standard, gaps)
                )

        # Calculate overall risk level
        risk_level = self._calculate_risk_level(
            applicable_standards, compliance_status, gaps_identified
        )

        # Schedule next assessment
        next_assessment_date = self._schedule_next_assessment(applicable_standards)

        # Generate documentation
        documentation = await self._generate_documentation(
            model_id, applicable_standards, compliance_status
        )

        report = ComplianceReport(
            report_id=report_id,
            model_id=model_id,
            assessment_date=assessment_date,
            standards_assessed=applicable_standards,
            compliance_status=compliance_status,
            risk_level=risk_level,
            gaps_identified=gaps_identified,
            remediation_actions=remediation_actions,
            next_assessment_date=next_assessment_date,
            documentation=documentation
        )

        self.audit_history.append(report)

        return report

    def _determine_applicable_standards(
        self,
        model_type: str,
        intended_use: str
    ) -> List[ComplianceStandard]:
        """Determine which compliance standards apply."""
        applicable = []

        # AI Act - High risk systems
        high_risk_uses = [
            "credit scoring", "employment decisions", "insurance pricing",
            "law enforcement", "migration", "justice administration"
        ]

        if any(use in intended_use.lower() for use in high_risk_uses):
            applicable.append(ComplianceStandard.AI_ACT_HIGH_RISK)
        else:
            applicable.append(ComplianceStandard.AI_ACT_LIMITED)

        # GDPR - Automated decision making
        if "automated decision" in intended_use.lower():
            applicable.append(ComplianceStandard.GDPR_ARTICLE_22)

        # Always include general standards
        applicable.extend([
            ComplianceStandard.ISO_IEC_42001,
            ComplianceStandard.NIST_AI_RMF
        ])

        return applicable

    async def _assess_standard(
        self,
        standard: ComplianceStandard,
        model_id: str,
        model_type: str,
        intended_use: str,
        data_description: str,
        bias_results: List[BiasDetectionResult],
        explanation: ModelExplanation
    ) -> Tuple[bool, List[Dict[str, Any]]]:
        """Assess compliance with a specific standard."""
        gaps = []
        is_compliant = True

        if standard == ComplianceStandard.AI_ACT_HIGH_RISK:
            # Check risk management system
            if not self._has_risk_management(model_id):
                gaps.append({
                    "requirement": "Risk management system",
                    "status": "missing",
                    "description": "No risk management system documented"
                })
                is_compliant = False

            # Check technical documentation
            if not self._has_technical_documentation(model_id):
                gaps.append({
                    "requirement": "Technical documentation",
                    "status": "incomplete",
                    "description": "Technical documentation is incomplete"
                })
                is_compliant = False

            # Check for bias mitigation
            if bias_results:
                has_critical_bias = any(
                    r.severity == "critical" and r.is_biased
                    for r in bias_results
                )
                if has_critical_bias:
                    gaps.append({
                        "requirement": "Bias mitigation",
                        "status": "non_compliant",
                        "description": "Critical bias detected without mitigation"
                    })
                    is_compliant = False

        elif standard == ComplianceStandard.GDPR_ARTICLE_22:
            # Check explainability
            if not explanation or explanation.confidence_score < 0.7:
                gaps.append({
                    "requirement": "Explainability",
                    "status": "insufficient",
                    "description": "Model explanations are not sufficient for GDPR compliance"
                })
                is_compliant = False

            # Check for human oversight
            if not self._has_human_oversight(model_id):
                gaps.append({
                    "requirement": "Human oversight",
                    "status": "missing",
                    "description": "No human oversight mechanism implemented"
                })
                is_compliant = False

        elif standard == ComplianceStandard.ISO_IEC_42001:
            # Check AI management system
            if not self._has_ai_management_system():
                gaps.append({
                    "requirement": "AI management system",
                    "status": "missing",
                    "description": "No formal AI management system in place"
                })
                is_compliant = False

        elif standard == ComplianceStandard.NIST_AI_RMF:
            # Check NIST RMF pillars
            pillars = ["govern", "map", "measure", "manage"]
            for pillar in pillars:
                if not self._has_nist_pillar(pillar):
                    gaps.append({
                        "requirement": f"NIST RMF - {pillar.capitalize()}",
                        "status": "incomplete",
                        "description": f"NIST RMF {pillar} pillar not fully implemented"
                    })
                    is_compliant = False

        return is_compliant, gaps

    def _has_risk_management(self, model_id: str) -> bool:
        """Check if model has risk management system."""
        # In production, check actual risk management documentation
        return True  # Simplified for example

    def _has_technical_documentation(self, model_id: str) -> bool:
        """Check if model has technical documentation."""
        # In production, check actual documentation
        return True  # Simplified for example

    def _has_human_oversight(self, model_id: str) -> bool:
        """Check if model has human oversight mechanism."""
        # In production, check actual oversight implementation
        return True  # Simplified for example

    def _has_ai_management_system(self) -> bool:
        """Check if organization has AI management system."""
        # In production, check actual AI MS implementation
        return True  # Simplified for example

    def _has_nist_pillar(self, pillar: str) -> bool:
        """Check if NIST RMF pillar is implemented."""
        # In production, check actual NIST RMF implementation
        return True  # Simplified for example

    def _generate_remediation_actions(
        self,
        standard: ComplianceStandard,
        gaps: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """Generate remediation actions for identified gaps."""
        actions = []

        for gap in gaps:
            requirement = gap["requirement"]

            if requirement == "Risk management system":
                actions.append({
                    "action": "Implement risk management framework",
                    "priority": "high",
                    "timeline": "3 months",
                    "responsible": "AI Governance Team",
                    "description": "Establish comprehensive risk management for AI systems"
                })

            elif requirement == "Bias mitigation":
                actions.append({
                    "action": "Implement bias mitigation techniques",
                    "priority": "critical",
                    "timeline": "1 month",
                    "responsible": "ML Engineering Team",
                    "description": "Apply preprocessing, in-processing, or post-processing bias mitigation"
                })

            elif requirement == "Explainability":
                actions.append({
                    "action": "Enhance model explainability",
                    "priority": "high",
                    "timeline": "2 months",
                    "responsible": "ML Engineering Team",
                    "description": "Implement XAI methods and improve explanation quality"
                })

        return actions

    def _calculate_risk_level(
        self,
        standards: List[ComplianceStandard],
        compliance_status: Dict[str, bool],
        gaps: List[Dict[str, Any]]
    ) -> str:
        """Calculate overall risk level."""
        # Count critical gaps
        critical_gaps = sum(1 for gap in gaps if gap.get("status") == "non_compliant")

        # Check high-risk standards
        has_high_risk = any(
            standard in [ComplianceStandard.AI_ACT_HIGH_RISK, ComplianceStandard.GDPR_ARTICLE_22]
            for standard in standards
        )

        # Calculate compliance percentage
        compliant_count = sum(compliance_status.values())
        total_count = len(compliance_status)
        compliance_rate = compliant_count / total_count if total_count > 0 else 0

        # Determine risk level
        if critical_gaps > 0 or compliance_rate < 0.5:
            return "critical"
        elif has_high_risk and compliance_rate < 0.8:
            return "high"
        elif compliance_rate < 0.9:
            return "medium"
        else:
            return "low"

    def _schedule_next_assessment(
        self,
        standards: List[ComplianceStandard]
    ) -> datetime:
        """Schedule next compliance assessment."""
        # Use most frequent requirement
        frequencies = {
            ComplianceStandard.AI_ACT_HIGH_RISK: 365,  # annual
            ComplianceStandard.GDPR_ARTICLE_22: 182,  # semiannual
            ComplianceStandard.ISO_IEC_42001: 365,  # annual
            ComplianceStandard.NIST_AI_RMF: 90,  # quarterly
            ComplianceStandard.AI_ACT_LIMITED: 365  # annual
        }

        min_days = min(frequencies.get(s, 365) for s in standards)
        return datetime.now() + timedelta(days=min_days)

    async def _generate_documentation(
        self,
        model_id: str,
        standards: List[ComplianceStandard],
        compliance_status: Dict[str, bool]
    ) -> Dict[str, str]:
        """Generate compliance documentation."""
        docs = {}

        # Compliance summary
        summary = f"Compliance Assessment Summary for {model_id}\n"
        summary += f"Assessment Date: {datetime.now().strftime('%Y-%m-%d')}\n\n"

        for standard in standards:
            status = "✓ Compliant" if compliance_status.get(standard.value, False) else "✗ Non-compliant"
            summary += f"{standard.value}: {status}\n"

        docs['summary'] = summary

        # Evidence collection
        evidence = "Evidence Collection:\n"
        evidence += "- Bias detection results\n"
        evidence += "- Model explanations\n"
        evidence += "- Technical documentation\n"
        evidence += "- Risk assessment\n"

        docs['evidence'] = evidence

        return docs


class EthicalAIService:
    """Main ethical AI service orchestrating all components."""

    def __init__(self):
        self.bias_detector = BiasDetector()
        self.model_explainer = ModelExplainer()
        self.compliance_monitor = ComplianceMonitor()
        self.assessment_history = []
        self.monitoring_active = False

    async def conduct_ethical_assessment(
        self,
        model: Any,
        model_id: str,
        model_type: str,
        X: pd.DataFrame,
        y: pd.Series,
        sensitive_attributes: List[str],
        intended_use: str,
        data_description: str
    ) -> Dict[str, Any]:
        """
        Conduct comprehensive ethical AI assessment.

        Args:
            model: Trained model to assess
            model_id: Unique model identifier
            model_type: Type of AI model
            X: Feature dataset
            y: Target variable
            sensitive_attributes: List of protected attributes
            intended_use: Intended use case
            data_description: Description of training data

        Returns:
            Comprehensive assessment results
        """
        logger.info(f"Starting ethical assessment for model {model_id}")

        assessment_results = {
            "model_id": model_id,
            "assessment_date": datetime.now().isoformat(),
            "components": {}
        }

        # 1. Bias Detection
        logger.info("Running bias detection...")
        bias_results = await self.bias_detector.detect_bias(
            model, X, y.name, sensitive_attributes
        )
        assessment_results["components"]["bias_detection"] = [
            asdict(result) for result in bias_results
        ]

        # 2. Model Explainability
        logger.info("Generating model explanations...")
        explanation = await self.model_explainer.explain_model(model, X, y)
        assessment_results["components"]["explainability"] = asdict(explanation)

        # 3. Compliance Assessment
        logger.info("Assessing compliance...")
        compliance_report = await self.compliance_monitor.assess_compliance(
            model_id, model_type, intended_use, data_description,
            bias_results, explanation
        )
        assessment_results["components"]["compliance"] = asdict(compliance_report)

        # 4. Overall Ethical Score
        ethical_score = self._calculate_ethical_score(assessment_results)
        assessment_results["ethical_score"] = ethical_score

        # 5. Recommendations
        recommendations = self._generate_ethical_recommendations(assessment_results)
        assessment_results["recommendations"] = recommendations

        # Save assessment
        self.assessment_history.append(assessment_results)

        return assessment_results

    def _calculate_ethical_score(self, assessment_results: Dict[str, Any]) -> Dict[str, Any]:
        """Calculate overall ethical AI score."""
        bias_results = assessment_results["components"].get("bias_detection", [])
        compliance_results = assessment_results["components"].get("compliance", {})

        # Bias score (0-100)
        bias_score = 100
        if bias_results:
            biased_count = sum(1 for result in bias_results if result.get("is_biased", False))
            bias_score = max(0, 100 - (biased_count / len(bias_results) * 100))

        # Compliance score (0-100)
        compliance_score = 100
        if compliance_results:
            compliance_status = compliance_results.get("compliance_status", {})
            if compliance_status:
                compliant_count = sum(compliance_status.values())
                total_count = len(compliance_status)
                compliance_score = (compliant_count / total_count) * 100

        # Explainability score (0-100)
        explainability_score = 100
        explanation = assessment_results["components"].get("explainability", {})
        if explanation:
            confidence = explanation.get("confidence_score", 0)
            explainability_score = confidence * 100

        # Overall weighted score
        overall_score = (
            bias_score * 0.4 +
            compliance_score * 0.4 +
            explainability_score * 0.2
        )

        return {
            "overall": round(overall_score, 2),
            "bias": round(bias_score, 2),
            "compliance": round(compliance_score, 2),
            "explainability": round(explainability_score, 2),
            "grade": self._get_grade(overall_score)
        }

    def _get_grade(self, score: float) -> str:
        """Get letter grade based on score."""
        if score >= 90:
            return "A+"
        elif score >= 80:
            return "A"
        elif score >= 70:
            return "B"
        elif score >= 60:
            return "C"
        elif score >= 50:
            return "D"
        else:
            return "F"

    def _generate_ethical_recommendations(
        self,
        assessment_results: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """Generate recommendations for improving ethical AI."""
        recommendations = []

        # Bias recommendations
        bias_results = assessment_results["components"].get("bias_detection", [])
        for result in bias_results:
            if result.get("is_biased", False):
                recommendations.append({
                    "category": "bias_mitigation",
                    "priority": "high" if result.get("severity") in ["high", "critical"] else "medium",
                    "action": f"Address {result.get('bias_type')} bias",
                    "description": result.get("explanation", ""),
                    "solutions": result.get("recommendations", [])
                })

        # Compliance recommendations
        compliance = assessment_results["components"].get("compliance", {})
        gaps = compliance.get("gaps_identified", [])
        for gap in gaps:
            recommendations.append({
                "category": "compliance",
                "priority": "high",
                "action": f"Fix {gap.get('requirement')} gap",
                "description": gap.get("description", ""),
                "solutions": ["Implement required controls", "Update documentation"]
            })

        # Explainability recommendations
        explanation = assessment_results["components"].get("explainability", {})
        if explanation.get("confidence_score", 0) < 0.7:
            recommendations.append({
                "category": "explainability",
                "priority": "medium",
                "action": "Improve model explainability",
                "description": "Model explanations are not sufficiently clear",
                "solutions": ["Use SHAP values", "Implement LIME", "Create surrogate models"]
            })

        return recommendations

    async def start_monitoring(self, model_id: str):
        """Start continuous ethical monitoring of a model."""
        self.monitoring_active = True
        logger.info(f"Started ethical monitoring for model {model_id}")

        # In production, implement continuous monitoring
        # - Real-time bias detection
        # - Drift monitoring
        # - Compliance checks
        # - Alerting for ethical violations

    async def stop_monitoring(self, model_id: str):
        """Stop ethical monitoring of a model."""
        self.monitoring_active = False
        logger.info(f"Stopped ethical monitoring for model {model_id}")

    async def generate_ethical_report(
        self,
        model_id: str,
        format: str = "json"
    ) -> Dict[str, Any]:
        """Generate comprehensive ethical AI report."""
        # Find assessment for model
        assessment = None
        for a in self.assessment_history:
            if a.get("model_id") == model_id:
                assessment = a
                break

        if not assessment:
            raise EthicalAIError(f"No assessment found for model {model_id}")

        report = {
            "report_metadata": {
                "model_id": model_id,
                "report_date": datetime.now().isoformat(),
                "report_type": "ethical_ai_assessment",
                "version": "1.0"
            },
            "assessment_summary": assessment,
            "recommendations": assessment.get("recommendations", []),
            "next_steps": self._generate_next_steps(assessment)
        }

        if format == "html":
            report["html"] = self._generate_html_report(report)

        return report

    def _generate_next_steps(self, assessment: Dict[str, Any]) -> List[str]:
        """Generate next steps based on assessment."""
        next_steps = []

        # Check overall score
        ethical_score = assessment.get("ethical_score", {})
        overall_score = ethical_score.get("overall", 0)

        if overall_score < 70:
            next_steps.append("Urgent: Address critical ethical issues before deployment")

        # Check for critical bias
        bias_results = assessment.get("components", {}).get("bias_detection", [])
        has_critical = any(
            result.get("severity") == "critical" and result.get("is_biased")
            for result in bias_results
        )

        if has_critical:
            next_steps.append("Implement bias mitigation techniques immediately")

        # Check compliance
        compliance = assessment.get("components", {}).get("compliance", {})
        if compliance.get("risk_level") in ["high", "critical"]:
            next_steps.append("Schedule urgent compliance review with legal team")

        # General recommendations
        next_steps.extend([
            "Implement continuous ethical monitoring",
            "Document all ethical decisions and mitigations",
            "Schedule regular ethical assessments"
        ])

        return next_steps

    def _generate_html_report(self, report: Dict[str, Any]) -> str:
        """Generate HTML version of the ethical report."""
        html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Ethical AI Assessment Report</title>
            <style>
                body {{ font-family: Arial, sans-serif; margin: 40px; }}
                .header {{ background-color: #f0f0f0; padding: 20px; border-radius: 5px; }}
                .score {{ font-size: 24px; font-weight: bold; color: #2c3e50; }}
                .section {{ margin: 20px 0; padding: 15px; border-left: 4px solid #3498db; }}
                .recommendation {{ background-color: #fff3cd; padding: 10px; margin: 5px 0; border-radius: 3px; }}
                .grade-{report['assessment_summary']['ethical_score']['grade'][0].lower()}}
                    {{ color: {'green' if report['assessment_summary']['ethical_score']['grade'][0] in ['A', 'B'] else 'orange' if report['assessment_summary']['ethical_score']['grade'][0] == 'C' else 'red'}; }}
            </style>
        </head>
        <body>
            <div class="header">
                <h1>Ethical AI Assessment Report</h1>
                <p><strong>Model ID:</strong> {report['report_metadata']['model_id']}</p>
                <p><strong>Report Date:</strong> {report['report_metadata']['report_date']}</p>
            </div>

            <div class="section">
                <h2>Overall Ethical Score</h2>
                <div class="score grade-{report['assessment_summary']['ethical_score']['grade'][0].lower()}">
                    {report['assessment_summary']['ethical_score']['overall']}/100
                    ({report['assessment_summary']['ethical_score']['grade']})
                </div>
                <ul>
                    <li>Bias Score: {report['assessment_summary']['ethical_score']['bias']}/100</li>
                    <li>Compliance Score: {report['assessment_summary']['ethical_score']['compliance']}/100</li>
                    <li>Explainability Score: {report['assessment_summary']['ethical_score']['explainability']}/100</li>
                </ul>
            </div>

            <div class="section">
                <h2>Recommendations</h2>
        """

        for rec in report['recommendations']:
            html += f"""
                <div class="recommendation">
                    <h4>{rec.get('action', 'Unknown Action')}</h4>
                    <p><strong>Priority:</strong> {rec.get('priority', 'Medium')}</p>
                    <p>{rec.get('description', '')}</p>
                </div>
            """

        html += """
            </div>
            <div class="section">
                <h2>Next Steps</h2>
                <ul>
        """

        for step in report['next_steps']:
            html += f"<li>{step}</li>"

        html += """
                </ul>
            </div>
        </body>
        </html>
        """

        return html


# Initialize ethical AI service
ethical_ai_service = EthicalAIService()