"""
Conversational AI Chatbot Service

Intelligent property chatbot with multi-language support,
intent recognition, and context management.
"""

__version__ = "1.0.0"
__author__ = "AI Services Team"
__email__ = "ai-team@gogidix.com"

from .chatbot import PropertyChatbot
from .nlp_processor import NLPProcessor
from .intent_classifier import IntentClassifier
from .entity_extractor import EntityExtractor
from .response_generator import ResponseGenerator

__all__ = [
    "PropertyChatbot",
    "NLPProcessor",
    "IntentClassifier",
    "EntityExtractor",
    "ResponseGenerator",
]