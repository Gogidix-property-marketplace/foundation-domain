"""
Ethical AI Service

Responsible AI components including bias detection, fairness metrics,
explainability, and compliance monitoring for AI Act and GDPR.
"""

__version__ = "1.0.0"
__author__ = "AI Services Team"
__email__ = "ai-team@gogidix.com"

from .service import (
    EthicalAIService,
    BiasDetector,
    ModelExplainer,
    ComplianceMonitor,
    BiasType,
    ComplianceStandard,
    BiasDetectionResult,
    FairnessMetrics,
    ModelExplanation,
    ComplianceReport
)

__all__ = [
    "EthicalAIService",
    "BiasDetector",
    "ModelExplainer",
    "ComplianceMonitor",
    "BiasType",
    "ComplianceStandard",
    "BiasDetectionResult",
    "FairnessMetrics",
    "ModelExplanation",
    "ComplianceReport",
]