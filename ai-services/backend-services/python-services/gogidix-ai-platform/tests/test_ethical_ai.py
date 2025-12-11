"""
Test Suite for Ethical AI Service

Comprehensive tests for bias detection, model explainability,
and compliance monitoring components.
"""

import pytest
import numpy as np
import pandas as pd
from unittest.mock import Mock, AsyncMock, patch
from datetime import datetime
from pathlib import Path

from src.gogidix_ai.ethical_ai.service import (
    EthicalAIService,
    BiasDetector,
    ModelExplainer,
    ComplianceMonitor,
    BiasType,
    ComplianceStandard,
    BiasDetectionResult,
    ModelExplanation,
    ComplianceReport
)
from src.gogidix_ai.core.exceptions import EthicalAIError, ComplianceError


class TestBiasDetector:
    """Test suite for BiasDetector component."""

    @pytest.fixture
    def bias_detector(self):
        """Create bias detector instance."""
        return BiasDetector()

    @pytest.fixture
    def sample_dataset(self):
        """Create sample dataset for testing."""
        np.random.seed(42)
        n_samples = 1000

        data = {
            'age': np.random.randint(18, 80, n_samples),
            'income': np.random.normal(50000, 15000, n_samples),
            'gender': np.random.choice(['male', 'female'], n_samples),
            'race': np.random.choice(['white', 'black', 'asian', 'hispanic'], n_samples),
            'credit_score': np.random.randint(300, 850, n_samples),
            'loan_approved': np.random.choice([0, 1], n_samples, p=[0.3, 0.7])
        }

        return pd.DataFrame(data)

    @pytest.fixture
    def mock_model(self):
        """Create mock model for testing."""
        model = Mock()
        model.predict = Mock(return_value=np.random.random(100))
        return model

    @pytest.mark.asyncio
    async def test_detect_demographic_parity(self, bias_detector, sample_dataset, mock_model):
        """Test demographic parity bias detection."""
        predictions = np.random.random(len(sample_dataset))
        sensitive_attributes = ['gender', 'race']

        result = await bias_detector._check_demographic_parity(
            sample_dataset, predictions, sensitive_attributes
        )

        assert result is not None
        assert isinstance(result, BiasDetectionResult)
        assert result.bias_type == BiasType.DEMOGRAPHIC_PARITY
        assert isinstance(result.metric_value, float)
        assert isinstance(result.is_biased, bool)
        assert len(result.affected_groups) > 0

    @pytest.mark.asyncio
    async def test_detect_equalized_odds(self, bias_detector, sample_dataset, mock_model):
        """Test equalized odds bias detection."""
        target_column = 'loan_approved'
        predictions = np.random.random(len(sample_dataset))
        sensitive_attributes = ['gender']

        result = await bias_detector._check_equalized_odds(
            sample_dataset, target_column, predictions, sensitive_attributes
        )

        assert result is not None
        assert isinstance(result, BiasDetectionResult)
        assert result.bias_type == BiasType.EQUALIZED_ODDS
        assert isinstance(result.metric_value, float)

    @pytest.mark.asyncio
    async def test_detect_geographic_fairness(self, bias_detector, mock_model):
        """Test geographic fairness bias detection."""
        # Create dataset with geographic information
        geo_data = pd.DataFrame({
            'zip_code': np.random.choice(['10001', '10002', '20001', '20002'], 500),
            'property_value': np.random.normal(300000, 100000, 500)
        })

        predictions = np.random.random(500)

        result = await bias_detector._check_geographic_fairness(
            geo_data, predictions, ['zip_code']
        )

        assert result is not None
        assert isinstance(result, BiasDetectionResult)
        assert result.bias_type == BiasType.GEOGRAPHIC_FAIRNESS

    @pytest.mark.asyncio
    async def test_detect_bias_comprehensive(self, bias_detector, sample_dataset, mock_model):
        """Test comprehensive bias detection."""
        sensitive_attributes = ['gender', 'race', 'age']
        bias_types = [
            BiasType.DEMOGRAPHIC_PARITY,
            BiasType.EQUALIZED_ODDS,
            BiasType.EQUAL_OPPORTUNITY
        ]

        results = await bias_detector.detect_bias(
            mock_model,
            sample_dataset,
            'loan_approved',
            sensitive_attributes,
            bias_types
        )

        assert isinstance(results, list)
        assert len(results) > 0
        for result in results:
            assert isinstance(result, BiasDetectionResult)

    def test_calculate_severity(self, bias_detector):
        """Test severity level calculation."""
        threshold = 0.1

        assert bias_detector._calculate_severity(0.05, threshold) == "low"
        assert bias_detector._calculate_severity(0.15, threshold) == "medium"
        assert bias_detector._calculate_severity(0.25, threshold) == "high"
        assert bias_detector._calculate_severity(0.4, threshold) == "critical"


class TestModelExplainer:
    """Test suite for ModelExplainer component."""

    @pytest.fixture
    def model_explainer(self):
        """Create model explainer instance."""
        return ModelExplainer()

    @pytest.fixture
    def sample_data(self):
        """Create sample features and target."""
        np.random.seed(42)
        n_samples = 500
        n_features = 10

        X = pd.DataFrame(
            np.random.randn(n_samples, n_features),
            columns=[f'feature_{i}' for i in range(n_features)]
        )
        y = pd.Series(np.random.randn(n_samples), name='target')

        return X, y

    @pytest.fixture
    def mock_model(self):
        """Create mock model with feature_importances_."""
        model = Mock()
        model.predict = Mock(return_value=np.random.randn(100))
        model.predict_proba = Mock(return_value=np.random.rand(100, 2))
        model.feature_importances_ = np.random.rand(10)
        model.decision_path = Mock(return_value=Mock(indices=[0, 1, 2]))
        return model

    @pytest.mark.asyncio
    async def test_calculate_feature_importance(self, model_explainer, sample_data, mock_model):
        """Test feature importance calculation."""
        X, y = sample_data

        importance = await model_explainer._calculate_feature_importance(
            mock_model, X, y, ['builtin', 'correlation']
        )

        assert isinstance(importance, dict)
        assert len(importance) == X.shape[1]
        assert all(isinstance(v, float) for v in importance.values())
        assert abs(sum(importance.values()) - 1.0) < 0.01  # Should sum to 1

    @pytest.mark.asyncio
    async def test_generate_counterfactual_examples(self, model_explainer, sample_data, mock_model):
        """Test counterfactual example generation."""
        X, y = sample_data

        counterfactuals = await model_explainer._generate_counterfactual_examples(
            mock_model, X, sample_size=10
        )

        assert isinstance(counterfactuals, list)
        assert len(counterfactuals) <= 5  # Should return top 5 examples

    @pytest.mark.asyncio
    @patch('src.gogidix_ai.ethical_ai.service.EXPLAINABILITY_AVAILABLE', True)
    async def test_explain_model(self, model_explainer, sample_data, mock_model):
        """Test comprehensive model explanation."""
        X, y = sample_data

        # Mock SHAP and LIME
        with patch('src.gogidix_ai.ethical_ai.service.shap') as mock_shap:
            mock_shap.TreeExplainer = Mock()
            mock_shap_values = np.random.randn(100, X.shape[1])
            mock_shap.TreeExplainer.return_value.shap_values = mock_shap_values

            explanation = await model_explainer.explain_model(
                mock_model, X, y, ['feature_importance'], sample_size=50
            )

            assert isinstance(explanation, ModelExplanation)
            assert explanation.model_id == 'unknown'
            assert len(explanation.input_features) == X.shape[1]
            assert explanation.feature_importance is not None
            assert explanation.reasoning is not None

    @pytest.mark.asyncio
    async def test_extract_decision_path(self, model_explainer, sample_data, mock_model):
        """Test decision path extraction for tree models."""
        X, _ = sample_data

        # Mock tree attributes
        mock_model.tree_.feature = [0, 1, 2, -2]
        mock_model.tree_.threshold = [0.5, 1.5, 2.5, 0]

        decision_path = await model_explainer._extract_decision_path(mock_model, X.iloc[0:1])

        assert decision_path is not None
        assert 'decision_nodes' in decision_path
        assert 'final_prediction' in decision_path


class TestComplianceMonitor:
    """Test suite for ComplianceMonitor component."""

    @pytest.fixture
    def compliance_monitor(self):
        """Create compliance monitor instance."""
        return ComplianceMonitor()

    def test_determine_applicable_standards(self, compliance_monitor):
        """Test determination of applicable compliance standards."""
        # High risk use case
        standards = compliance_monitor._determine_applicable_standards(
            model_type="credit_scoring",
            intended_use="Automated credit decision making for loan applications"
        )

        assert ComplianceStandard.AI_ACT_HIGH_RISK in standards
        assert ComplianceStandard.GDPR_ARTICLE_22 in standards

        # Low risk use case
        standards = compliance_monitor._determine_applicable_standards(
            model_type="recommendation",
            intended_use="Property recommendations for users"
        )

        assert ComplianceStandard.AI_ACT_LIMITED in standards

    @pytest.mark.asyncio
    async def test_assess_ai_act_compliance(self, compliance_monitor):
        """Test AI Act compliance assessment."""
        is_compliant, gaps = await compliance_monitor._assess_standard(
            ComplianceStandard.AI_ACT_HIGH_RISK,
            model_id="test_model",
            model_type="credit_scoring",
            intended_use="Credit decisions",
            data_description="Loan application data",
            bias_results=[],
            explanation=None
        )

        assert isinstance(is_compliant, bool)
        assert isinstance(gaps, list)

    @pytest.mark.asyncio
    async def test_assess_gdpr_compliance(self, compliance_monitor):
        """Test GDPR compliance assessment."""
        # Create mock explanation
        explanation = Mock()
        explanation.confidence_score = 0.8

        is_compliant, gaps = await compliance_monitor._assess_standard(
            ComplianceStandard.GDPR_ARTICLE_22,
            model_id="test_model",
            model_type="decision_support",
            intended_use="Automated decision making",
            data_description="User data",
            bias_results=[],
            explanation=explanation
        )

        assert isinstance(is_compliant, bool)
        assert isinstance(gaps, list)

    def test_calculate_risk_level(self, compliance_monitor):
        """Test risk level calculation."""
        # Critical risk
        risk = compliance_monitor._calculate_risk_level(
            standards=[ComplianceStandard.AI_ACT_HIGH_RISK],
            compliance_status={"ai_act_high_risk": False},
            gaps=[{"status": "non_compliant"}]
        )
        assert risk == "critical"

        # Low risk
        risk = compliance_monitor._calculate_risk_level(
            standards=[ComplianceStandard.ISO_IEC_42001],
            compliance_status={"iso_iec_42001": True},
            gaps=[]
        )
        assert risk == "low"

    @pytest.mark.asyncio
    async def test_assess_comprehensive(self, compliance_monitor):
        """Test comprehensive compliance assessment."""
        with patch.object(compliance_monitor, '_assess_standard') as mock_assess:
            mock_assess.return_value = (True, [])

            report = await compliance_monitor.assess_compliance(
                model_id="test_model",
                model_type="credit_scoring",
                intended_use="Automated credit decisions",
                data_description="Financial data"
            )

            assert isinstance(report, ComplianceReport)
            assert report.model_id == "test_model"
            assert isinstance(report.compliance_status, dict)
            assert isinstance(report.risk_level, str)


class TestEthicalAIService:
    """Test suite for main EthicalAIService."""

    @pytest.fixture
    def ethical_ai_service(self):
        """Create ethical AI service instance."""
        return EthicalAIService()

    @pytest.fixture
    def sample_data(self):
        """Create sample dataset."""
        np.random.seed(42)
        n_samples = 500

        X = pd.DataFrame({
            'feature_1': np.random.randn(n_samples),
            'feature_2': np.random.randn(n_samples),
            'feature_3': np.random.randn(n_samples),
            'gender': np.random.choice(['male', 'female'], n_samples),
            'age': np.random.randint(18, 80, n_samples)
        })

        y = pd.Series(np.random.choice([0, 1], n_samples), name='target')

        return X, y

    @pytest.fixture
    def mock_model(self):
        """Create mock model."""
        model = Mock()
        model.predict = Mock(return_value=np.random.random(500))
        model.model_id = "test_model"
        return model

    @pytest.mark.asyncio
    async def test_conduct_ethical_assessment(
        self,
        ethical_ai_service,
        sample_data,
        mock_model
    ):
        """Test comprehensive ethical assessment."""
        X, y = sample_data

        with patch.object(
            ethical_ai_service.bias_detector,
            'detect_bias',
            return_value=[]
        ) as mock_bias, \
             patch.object(
                 ethical_ai_service.model_explainer,
                 'explain_model',
                 return_value=ModelExplanation(
                     explanation_id="test",
                     model_id="test",
                     input_features=list(X.columns),
                     feature_importance={f"feature_{i}": 0.2 for i in range(3)}
                 )
             ) as mock_explain, \
             patch.object(
                 ethical_ai_service.compliance_monitor,
                 'assess_compliance',
                 return_value=ComplianceReport(
                     report_id="test",
                     model_id="test",
                     assessment_date=datetime.now(),
                     standards_assessed=[],
                     compliance_status={},
                     risk_level="low",
                     gaps_identified=[],
                     remediation_actions=[],
                     next_assessment_date=datetime.now()
                 )
             ) as mock_compliance:

            assessment = await ethical_ai_service.conduct_ethical_assessment(
                model=mock_model,
                model_id="test_model",
                model_type="credit_scoring",
                X=X,
                y=y,
                sensitive_attributes=['gender', 'age'],
                intended_use="Credit decisions",
                data_description="Test data"
            )

            assert isinstance(assessment, dict)
            assert "model_id" in assessment
            assert "ethical_score" in assessment
            assert "recommendations" in assessment

            mock_bias.assert_called_once()
            mock_explain.assert_called_once()
            mock_compliance.assert_called_once()

    def test_calculate_ethical_score(self, ethical_ai_service):
        """Test ethical score calculation."""
        assessment_results = {
            "components": {
                "bias_detection": [{"is_biased": False}, {"is_biased": True}],
                "compliance": {"compliance_status": {"standard1": True, "standard2": False}},
                "explainability": {"confidence_score": 0.8}
            }
        }

        score = ethical_ai_service._calculate_ethical_score(assessment_results)

        assert isinstance(score, dict)
        assert "overall" in score
        assert "bias" in score
        assert "compliance" in score
        assert "explainability" in score
        assert "grade" in score
        assert 0 <= score["overall"] <= 100

    def test_generate_ethical_recommendations(self, ethical_ai_service):
        """Test ethical recommendations generation."""
        assessment_results = {
            "components": {
                "bias_detection": [
                    {
                        "is_biased": True,
                        "severity": "high",
                        "bias_type": "demographic_parity",
                        "explanation": "Bias detected",
                        "recommendations": ["Mitigate bias"]
                    }
                ],
                "compliance": {
                    "gaps_identified": [
                        {
                            "requirement": "Risk management",
                            "description": "Missing risk management"
                        }
                    ]
                },
                "explainability": {
                    "confidence_score": 0.5  # Below threshold
                }
            }
        }

        recommendations = ethical_ai_service._generate_ethical_recommendations(assessment_results)

        assert isinstance(recommendations, list)
        assert len(recommendations) > 0
        for rec in recommendations:
            assert "category" in rec
            assert "priority" in rec
            assert "action" in rec

    @pytest.mark.asyncio
    async def test_start_stop_monitoring(self, ethical_ai_service):
        """Test monitoring start/stop functionality."""
        model_id = "test_model"

        # Start monitoring
        await ethical_ai_service.start_monitoring(model_id)
        assert ethical_ai_service.monitoring_active is True

        # Stop monitoring
        await ethical_ai_service.stop_monitoring(model_id)
        assert ethical_ai_service.monitoring_active is False

    @pytest.mark.asyncio
    async def test_generate_ethical_report(self, ethical_ai_service):
        """Test ethical report generation."""
        # Create assessment history
        assessment = {
            "model_id": "test_model",
            "ethical_score": {"overall": 85, "grade": "A"},
            "recommendations": [{"action": "Test recommendation"}]
        }
        ethical_ai_service.assessment_history.append(assessment)

        # Generate report
        report = await ethical_ai_service.generate_ethical_report("test_model")

        assert isinstance(report, dict)
        assert "report_metadata" in report
        assert "assessment_summary" in report
        assert "recommendations" in report
        assert "next_steps" in report


@pytest.mark.asyncio
async def test_ethical_ai_integration():
    """Integration test for the complete ethical AI workflow."""
    # Create test data
    np.random.seed(42)
    n_samples = 200

    X = pd.DataFrame({
        'income': np.random.normal(50000, 15000, n_samples),
        'age': np.random.randint(18, 80, n_samples),
        'credit_score': np.random.randint(300, 850, n_samples),
        'gender': np.random.choice(['male', 'female'], n_samples)
    })

    y = pd.Series(
        (X['income'] > 45000) & (X['credit_score'] > 600),
        dtype=int,
        name='loan_approved'
    )

    # Create mock model
    model = Mock()
    model.predict = Mock(return_value=y.values.astype(float) + np.random.normal(0, 0.1, n_samples))
    model.model_id = "integration_test_model"

    # Initialize service
    service = EthicalAIService()

    # Run assessment
    assessment = await service.conduct_ethical_assessment(
        model=model,
        model_id="integration_test_model",
        model_type="credit_scoring",
        X=X,
        y=y,
        sensitive_attributes=['gender', 'age'],
        intended_use="Loan approval decisions",
        data_description="Synthetic loan application data"
    )

    # Verify results
    assert isinstance(assessment, dict)
    assert "ethical_score" in assessment
    assert "components" in assessment

    ethical_score = assessment["ethical_score"]
    assert 0 <= ethical_score["overall"] <= 100
    assert ethical_score["grade"] in ["A+", "A", "B", "C", "D", "F"]

    # Generate report
    report = await service.generate_ethical_report("integration_test_model", format="json")
    assert "html" in report or "report_metadata" in report


if __name__ == "__main__":
    pytest.main([__file__, "-v"])