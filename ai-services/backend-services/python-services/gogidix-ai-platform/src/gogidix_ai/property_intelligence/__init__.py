"""
Property Intelligence Service

AI-powered property analysis including valuation,
image recognition, NLP, and market insights.
"""

__version__ = "1.0.0"
__author__ = "AI Services Team"
__email__ = "ai-team@gogidix.com"

from .valuation import PropertyValuationModel
from .image_analysis import PropertyImageAnalyzer
from .nlp_service import PropertyNLPService
from .market_analyzer import MarketAnalyzer
from .recommendation import PropertyRecommendationEngine

__all__ = [
    "PropertyValuationModel",
    "PropertyImageAnalyzer",
    "PropertyNLPService",
    "MarketAnalyzer",
    "PropertyRecommendationEngine",
]