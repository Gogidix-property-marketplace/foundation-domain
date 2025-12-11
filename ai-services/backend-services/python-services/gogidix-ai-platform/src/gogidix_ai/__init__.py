"""
Gogidix AI Services - Enterprise AI Platform

Production-grade AI services for property marketplace with cutting-edge
machine learning, real-time inference, and enterprise-scale reliability.

Modules:
- AI Gateway: Model routing and orchestration
- Property Intelligence: Valuation, image analysis, market insights
- Conversational AI: Multi-language intelligent chatbot
- Analytics: Real-time insights and reporting
- ML Platform: Training, deployment, and monitoring
- Ethical AI: Bias detection, fairness, and compliance
"""

__version__ = "1.0.0"
__author__ = "AI Services Team"
__email__ = "ai-team@gogidix.com"

from .core.config import Settings, get_settings
from .core.logging import get_logger
from .core.exceptions import (
    AIServiceError,
    ModelNotFoundError,
    ValidationError,
    RateLimitError,
    AuthenticationError,
)

# Import all service modules
from .ai_gateway.service import AIGatewayService
from .property_intelligence import (
    PropertyValuationModel,
    PropertyImageAnalyzer,
    PropertyNLPService,
    MarketAnalyzer,
    PropertyRecommendationEngine
)
from .conversational_ai import PropertyChatbot
from .analytics import AnalyticsService
from .ml_platform import MLPlatform
from .ethical_ai import (
    EthicalAIService,
    BiasDetector,
    ModelExplainer,
    ComplianceMonitor
)

# Core components
__all__ = [
    # Version
    "__version__",

    # Configuration
    "Settings",
    "get_settings",

    # Logging
    "get_logger",

    # Exceptions
    "AIServiceError",
    "ModelNotFoundError",
    "ValidationError",
    "RateLimitError",
    "AuthenticationError",

    # AI Gateway
    "AIGatewayService",

    # Property Intelligence
    "PropertyValuationModel",
    "PropertyImageAnalyzer",
    "PropertyNLPService",
    "MarketAnalyzer",
    "PropertyRecommendationEngine",

    # Conversational AI
    "PropertyChatbot",

    # Analytics
    "AnalyticsService",

    # ML Platform
    "MLPlatform",

    # Ethical AI
    "EthicalAIService",
    "BiasDetector",
    "ModelExplainer",
    "ComplianceMonitor",
]