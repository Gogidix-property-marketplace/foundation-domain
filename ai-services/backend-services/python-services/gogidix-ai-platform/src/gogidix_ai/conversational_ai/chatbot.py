"""
Property Marketplace Conversational AI Chatbot

Intelligent chatbot for property inquiries with multi-language support,
context management, and personalized responses.
"""

import asyncio
import json
import uuid
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass
from pathlib import Path
import logging

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger
from gogidix_ai.conversational_ai.nlp_processor import NLPProcessor
from gogidix_ai.conversational_ai.intent_classifier import IntentClassifier
from gogidix_ai.conversational_ai.entity_extractor import EntityExtractor
from gogidix_ai.conversational_ai.response_generator import ResponseGenerator

logger = get_logger(__name__)


@dataclass
class ConversationState:
    """Represents the state of a conversation."""
    conversation_id: str
    user_id: str
    messages: List[Dict[str, Any]]
    entities: Dict[str, Any]
    context: Dict[str, Any]
    preferences: Dict[str, Any]
    last_intent: Optional[str] = None
    last_action: Optional[str] = None
    created_at: datetime = None
    updated_at: datetime = None

    def __post_init__(self):
        if self.created_at is None:
            self.created_at = datetime.utcnow()
        if self.updated_at is None:
            self.updated_at = datetime.utcnow()


class PropertyChatbot:
    """
    Intelligent property marketplace chatbot with advanced NLP capabilities.
    """

    def __init__(self, model_path: Optional[str] = None):
        """Initialize the chatbot."""
        self.settings = get_settings()
        self.model_path = model_path or self.settings.CHATBOT_MODEL_PATH

        # Initialize NLP components
        self.nlp_processor = NLPProcessor()
        self.intent_classifier = IntentClassifier()
        self.entity_extractor = EntityExtractor()
        self.response_generator = ResponseGenerator()

        # Conversation management
        self.conversations: Dict[str, ConversationState] = {}
        self.conversation_history: Dict[str, List[Dict]] = {}
        self.user_profiles: Dict[str, Dict[str, Any]] = {}

        # Language support
        self.supported_languages = {
            "en": "English",
            "es": "Spanish",
            "fr": "French",
            "de": "German",
            "it": "Italian",
            "pt": "Portuguese",
            "zh": "Chinese",
            "ja": "Japanese",
            "ko": "Korean",
            "ar": "Arabic",
            "hi": "Hindi"
        }

        # Intent to action mapping
        self.intent_handlers = {
            "property_search": self._handle_property_search,
            "property_details": self._handle_property_details,
            "price_inquiry": self._handle_price_inquiry,
            "schedule_visit": self._handle_schedule_visit,
            "contact_agent": self._handle_contact_agent,
            "neighborhood_info": self._handle_neighborhood_info,
            "mortgage_calc": self._handle_mortgage_calc,
            "property_alerts": self._handle_property_alerts,
            "comparison": self._handle_comparison,
            "greeting": self._handle_greeting,
            "goodbye": self._handle_goodbye,
            "small_talk": self._handle_small_talk,
            "help": self._handle_help,
            "complaint": self._handle_complaint,
            "compliment": self._handle_compliment,
            "unknown": self._handle_unknown
        }

    async def initialize(self):
        """Initialize the chatbot and load models."""
        try:
            # Load models
            await self.intent_classifier.load_model(self.model_path)
            await self.entity_extractor.load_model(self.model_path)
            await self.response_generator.load_model(self.model_path)

            # Load conversation history if exists
            await self._load_conversation_history()

            # Load user profiles
            await self._load_user_profiles()

            logger.info(
                "Property Chatbot initialized successfully",
                supported_languages=len(self.supported_languages),
                intents=len(self.intent_handlers)
            )

        except Exception as e:
            logger.error(
                "Failed to initialize chatbot",
                error=str(e)
            )
            raise

    async def chat(
        self,
        message: str,
        user_id: str,
        conversation_id: Optional[str] = None,
        language: Optional[str] = None,
        context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Process a chat message and generate response.

        Args:
            message: User message
            user_id: User identifier
            conversation_id: Existing conversation ID
            language: Language code
            context: Additional context

        Returns:
            Response dictionary with message and metadata
        """
        try:
            # Detect language if not provided
            if language is None:
                language = await self.nlp_processor.detect_language(message)

            # Get or create conversation
            if conversation_id is None:
                conversation_id = str(uuid.uuid4())
                self.conversations[conversation_id] = ConversationState(
                    conversation_id=conversation_id,
                    user_id=user_id,
                    messages=[],
                    entities={},
                    context=context or {},
                    preferences={}
                )
            else:
                # Update existing conversation
                if conversation_id in self.conversations:
                    self.conversations[conversation_id].updated_at = datetime.utcnow()
                    if context:
                        self.conversations[conversation_id].context.update(context)

            # Get conversation state
            state = self.conversations[conversation_id]

            # Process message
            processed_message = await self.nlp_processor.preprocess(
                message, language
            )

            # Classify intent
            intent, confidence = await self.intent_classifier.classify(
                processed_message, language
            )

            # Extract entities
            entities = await self.entity_extractor.extract(
                processed_message, intent, language
            )

            # Update conversation state
            self._update_conversation_state(
                state, message, intent, entities
            )

            # Generate response
            response = await self._generate_response(
                state, processed_message, intent, entities, language
            )

            # Save message and response
            await self._save_message(state, message, response, language)

            # Log conversation
            await self._log_interaction(
                user_id, conversation_id, message, response,
                intent, entities, language
            )

            return {
                "conversation_id": conversation_id,
                "response": response["text"],
                "intent": intent,
                "confidence": confidence,
                "entities": entities,
                "language": language,
                "suggestions": response.get("suggestions", []),
                "actions": response.get("actions", []),
                "context": response.get("context", {}),
                "timestamp": datetime.utcnow().isoformat()
            }

        except Exception as e:
            logger.error(
                "Chat processing failed",
                user_id=user_id,
                conversation_id=conversation_id,
                error=str(e)
            )
            return {
                "conversation_id": conversation_id,
                "response": "I apologize, but I'm having trouble processing your request. Please try again.",
                "intent": "error",
                "confidence": 0.0,
                "error": str(e)
            }

    async def get_conversation_history(
        self,
        conversation_id: str,
        limit: Optional[int] = None
    ) -> List[Dict[str, Any]]:
        """
        Get conversation history.

        Args:
            conversation_id: Conversation identifier
            limit: Number of recent messages to return

        Returns:
            List of messages
        """
        if conversation_id not in self.conversations:
            return []

        messages = self.conversations[conversation_id].messages
        if limit:
            messages = messages[-limit:]

        return messages

    async def get_user_profile(self, user_id: str) -> Dict[str, Any]:
        """Get user profile and preferences."""
        return self.user_profiles.get(user_id, {})

    async def update_user_preferences(
        self,
        user_id: str,
        preferences: Dict[str, Any]
    ):
        """Update user preferences."""
        if user_id not in self.user_profiles:
            self.user_profiles[user_id] = {}

        self.user_profiles[user_id]["preferences"] = preferences
        await self._save_user_profiles()

    async def get_conversation_summary(
        self,
        conversation_id: str
    ) -> Dict[str, Any]:
        """Get summary of conversation."""
        if conversation_id not in self.conversations:
            return {}

        state = self.conversations[conversation_id]

        # Extract key information
        properties_mentioned = []
        search_criteria = {}
        questions_asked = []

        for msg in state.messages:
            if "entities" in msg:
                for entity in msg["entities"]:
                    if entity["type"] == "property_id":
                        properties_mentioned.append(entity["value"])
                    elif entity["type"] == "search_criteria":
                        search_criteria.update(entity["value"])

            if msg["sender"] == "user" and "?" in msg.get("text", ""):
                questions_asked.append(msg["text"])

        return {
            "conversation_id": conversation_id,
            "user_id": state.user_id,
            "duration_minutes": (
                datetime.utcnow() - state.created_at
            ).total_seconds() / 60,
            "message_count": len(state.messages),
            "properties_mentioned": properties_mentioned,
            "search_criteria": search_criteria,
            "questions_asked": questions_asked[:5],
            "last_intent": state.last_intent,
            "language": state.context.get("language", "en")
        }

    def _update_conversation_state(
        self,
        state: ConversationState,
        message: str,
        intent: str,
        entities: List[Dict[str, Any]]
    ):
        """Update conversation state with new information."""
        state.last_intent = intent
        state.entities.update({
            e["type"]: e["value"] for e in entities
        })

        # Update context based on intent
        if intent == "property_search":
            if "search_criteria" not in state.context:
                state.context["search_criteria"] = {}
            state.context["search_criteria"].update(
                {e["type"]: e["value"] for e in entities}
            )

    async def _generate_response(
        self,
        state: ConversationState,
        message: str,
        intent: str,
        entities: List[Dict[str, Any]],
        language: str
    ) -> Dict[str, Any]:
        """Generate appropriate response based on intent."""
        handler = self.intent_handlers.get(
            intent, self._handle_unknown
        )

        # Get user preferences
        user_profile = self.user_profiles.get(
            state.user_id, {}
        ).get("preferences", {})

        # Generate response
        response = await handler(
            state, message, entities, language, user_profile
        )

        # Personalize response
        response["text"] = self._personalize_response(
            response["text"], state, user_profile, language
        )

        return response

    async def _handle_property_search(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle property search intent."""
        search_criteria = {
            e["type"]: e["value"] for e in entities
            if e["type"] in [
                "location", "property_type", "price_range",
                "bedrooms", "bathrooms", "square_feet"
            ]
        }

        # Check if we have enough criteria
        if not search_criteria:
            return {
                "text": "I'd be happy to help you search for properties! Could you let me know what you're looking for? For example, you can tell me about the location, price range, or number of bedrooms.",
                "suggestions": [
                    "3 bedroom house under $500,000",
                    "Apartment in downtown area",
                    "House with a pool"
                ],
                "actions": ["show_filters"]
            }

        # Simulate property search
        search_results = await self._search_properties(search_criteria)

        if search_results:
            return {
                "text": f"I found {len(search_results)} properties matching your criteria. Here are the top results:",
                "context": {
                    "search_results": search_results[:3],
                    "total_count": len(search_results)
                },
                "suggestions": [
                    "See more properties",
                    "Refine search",
                    "Schedule a visit"
                ],
                "actions": ["show_search_results", "refine_search"]
            }
        else:
            return {
                "text": "I couldn't find any properties matching your criteria. Would you like me to adjust the search parameters?",
                "suggestions": [
                    "Increase price range",
                    "Change location",
                    "Modify property type"
                ],
                "actions": ["modify_search"]
            }

    async def _handle_property_details(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle property details inquiry."""
        property_id = next(
            (e["value"] for e in entities if e["type"] == "property_id"),
            None
        )

        if property_id:
            # Get property details
            details = await self._get_property_details(property_id)

            if details:
                return {
                    "text": f"Here are the details for the property at {details['address']}:",
                    "context": {
                        "property_details": details
                    },
                    "suggestions": [
                        "Schedule a visit",
                        "Get more photos",
                        "Calculate mortgage"
                    ],
                    "actions": ["schedule_visit", "more_photos", "mortgage_calc"]
                }
            else:
                return {
                    "text": "I couldn't find that property. Could you please check the property ID or provide more details?",
                    "actions": ["search_properties"]
                }
        else:
            return {
                "text": "Which property would you like to know more about? You can provide the property ID or describe the property.",
                "suggestions": [
                    "Search by location",
                    "Browse featured properties"
                ],
                "actions": ["search", "browse"]
            }

    async def _handle_price_inquiry(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle price inquiry."""
        if "price_range" in [e["type"] for e in entities]:
            price_range = next(
                e["value"] for e in entities if e["type"] == "price_range"
            )
            return {
                "text": f"The price range {price_range} is quite popular in this area. Properties in this range typically offer good value with modern amenities. Would you like me to show you some specific properties in this price range?",
                "suggestions": [
                    "Show properties in this range",
                    "Check market trends",
                    "Get financing info"
                ],
                "actions": ["show_matching", "market_trends", "financing"]
            }
        else:
            return {
                "text": "I can help you with pricing information! Are you looking for properties in a specific price range, or would you like me to analyze market prices in a particular area?",
                "suggestions": [
                    "Under $300,000",
                    "$300,000 - $500,000",
                    "$500,000 - $750,000",
                    "$750,000+"
                ],
                "actions": ["show_price_ranges", "market_analysis"]
            }

    async def _handle_schedule_visit(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle visit scheduling."""
        return {
            "text": "I'd be happy to help you schedule a property visit! Please let me know:\n1. Which property you're interested in\n2. Your preferred date and time\n3. Your contact information\n\nI can then connect you with the property agent to confirm the visit.",
            "context": {
                "visit_info": {
                    "status": "collecting_info"
                }
            },
            "suggestions": [
                "Schedule for this weekend",
                "Available this evening",
                "Next week availability"
            ],
            "actions": ["show_availability", "contact_agent"]
        }

    async def _handle_contact_agent(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle agent contact request."""
        return {
            "text": "I'll connect you with one of our experienced property agents right away. Our agents have extensive knowledge of the local market and can provide personalized assistance with your property search. They'll contact you shortly to discuss your requirements.",
            "context": {
                "agent_contact": {
                    "status": "initiated",
                    "timestamp": datetime.utcnow().isoformat()
                }
            },
            "suggestions": [
                "Common questions to ask",
                "Prepare documents needed",
                "What to bring to visit"
            ],
            "actions": ["agent_connected"]
        }

    async def _handle_neighborhood_info(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle neighborhood information request."""
        location = next(
            (e["value"] for e in entities if e["type"] == "location"),
            None
        )

        if location:
            # Get neighborhood information
            neighborhood_info = await self._get_neighborhood_info(location)

            return {
                "text": f"Here's information about {location}:",
                "context": {
                    "neighborhood_info": neighborhood_info
                },
                "suggestions": [
                    "School ratings",
                    "Crime statistics",
                    "Local amenities",
                    "Transportation options"
                ],
                "actions": ["show_schools", "show_crime_stats", "show_amenities"]
            }
        else:
            return {
                "text": "I'd be happy to provide neighborhood information! Which area or neighborhood would you like to know more about?",
                "suggestions": [
                    "Popular neighborhoods",
                    "School districts",
                    "Upcoming areas"
                ],
                "actions": ["show_neighborhoods"]
            }

    async def _handle_mortgage_calc(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle mortgage calculation."""
        # Extract financial entities
        price = next(
            (e["value"] for e in entities if e["type"] == "price"),
            None
        )
        down_payment = next(
            (e["value"] for e in entities if e["type"] == "down_payment"),
            None
        )
        interest_rate = next(
            (e["value"] for e in entities if e["type"] == "interest_rate"),
            None
        )
        loan_term = next(
            (e["value"] for e in entities if e["type"] == "loan_term"),
            None
        )

        if price:
            # Calculate mortgage
            mortgage_calc = await self._calculate_mortgage(
                price, down_payment, interest_rate, loan_term
            )

            return {
                "text": f"Based on a property price of ${price:,}, here's your mortgage estimate:",
                "context": {
                    "mortgage_calculation": mortgage_calc
                },
                "suggestions": [
                    "Adjust down payment",
                    "Change loan term",
                    "Compare rates"
                ],
                "actions": ["recalculate", "get_preapproved"]
            }
        else:
            return {
                "text": "I can help you calculate mortgage payments! Please provide:\n• Property price\n• Down payment amount\n• Interest rate\n• Loan term (years)",
                "suggestions": [
                    "Calculate with 20% down",
                    "30-year mortgage",
                    "15-year mortgage"
                ],
                "actions": ["use_defaults", "quick_estimate"]
            }

    async def _handle_property_alerts(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle property alert setup."""
        return {
            "text": "I can set up property alerts for you! You'll receive notifications when new properties match your criteria. What would you like to be alerted about?",
            "context": {
                "alert_setup": {
                    "status": "collecting_preferences"
                }
            },
            "suggestions": [
                "Price drop alerts",
                "New listings in area",
                "Open house notifications",
                "Recently reduced properties"
            ],
            "actions": ["setup_alerts"]
        }

    async def _handle_comparison(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle property comparison."""
        # Get recently mentioned properties
        properties = [
            e["value"] for e in entities if e["type"] == "property_id"
        ]

        if len(properties) >= 2:
            comparison = await self._compare_properties(properties)

            return {
                "text": f"Here's a comparison of the {len(properties)} properties you're interested in:",
                "context": {
                    "property_comparison": comparison
                },
                "suggestions": [
                    "View detailed comparison",
                    "Schedule visits",
                    "Get more information"
                ],
                "actions": ["detailed_comparison", "schedule_visits"]
            }
        else:
            return {
                "text": "I can help you compare properties! Please mention at least 2 properties you'd like to compare.",
                "suggestions": [
                    "Compare top 3 from search",
                    "Compare recently viewed",
                    "Create comparison list"
                ],
                "actions": ["select_properties"]
            }

    async def _handle_greeting(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle greeting messages."""
        greetings = {
            "en": [
                "Welcome to Gogidix Property Marketplace! I'm here to help you find your perfect property. What kind of property are you looking for today?",
                "Hello! I'm your AI property assistant. How can I help you with your real estate search today?"
            ],
            "es": [
                "¡Bienvenido a Gogidix! Estoy aquí para ayudarte a encontrar tu propiedad perfecta. ¿Qué tipo de propiedad buscas hoy?"
            ],
            "fr": [
                "Bienvenue sur Gogidix! Je suis là pour vous aider à trouver la propriété parfaite. Quel type de propriété cherchez-vous aujourd'hui?"
            ]
        }

        greeting_list = greetings.get(language, greetings["en"])

        # Personalize based on user profile
        if user_profile.get("last_search"):
            greeting_list.append(
                f"Welcome back! I remember you were interested in {user_profile['last_search']}. Would you like to continue with that search?"
            )

        return {
            "text": greeting_list[0],
            "suggestions": [
                "Search for properties",
                "Browse neighborhoods",
                "Get market insights"
            ],
            "actions": ["start_search", "browse", "insights"]
        }

    async def _handle_goodbye(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle goodbye messages."""
        goodbyes = {
            "en": [
                "Thank you for chatting with us today! Feel free to come back anytime if you need help with your property search.",
                "It was great helping you today! Don't hesitate to reach out if you have any more questions about properties."
            ],
            "es": [
                "¡Gracias por chatear con nosotros hoy! No dudes en volver si necesitas ayuda con tu búsqueda de propiedades."
            ]
        }

        goodbye_list = goodbyes.get(language, goodbyes["en"])

        # Summarize conversation
        summary = await self.get_conversation_summary(state.conversation_id)

        return {
            "text": goodbye_list[0],
            "context": {
                "conversation_summary": summary
            },
            "suggestions": [
                "Save conversation",
                "Email transcript",
                "Rate our service"
            ],
            "actions": ["save_conversation", "email_transcript", "rate_service"]
        }

    async def _handle_small_talk(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle small talk."""
        responses = {
            "en": [
                "That's interesting! Now, how about we get back to finding your perfect property?",
                "I appreciate the conversation! Let me know when you're ready to continue with your property search."
            ]
        }

        response_list = responses.get(language, responses["en"])

        return {
            "text": response_list[0],
            "suggestions": [
                "Continue property search",
                "Browse listings",
                "Ask me anything"
            ],
            "actions": ["continue", "browse", "ask"]
        }

    async def _handle_help(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle help requests."""
        help_text = {
            "en": """I can help you with:
• Searching for properties
• Getting property details
• Scheduling visits
• Market analysis
• Mortgage calculations
• Neighborhood information
• Setting up alerts
• Comparing properties

Just ask me anything about real estate!""",
            "es": """Puedo ayudarte con:
• Búsqueda de propiedades
• Detalles de propiedades
• Programar visitas
• Análisis del mercado
• Cálculos hipotecarios
• Información de barrios
• Configurar alertas
• Comparar propiedades"""
        }

        return {
            "text": help_text.get(language, help_text["en"]),
            "suggestions": [
                "Search properties",
                "Market trends",
                "Mortgage calculator"
            ],
            "actions": ["search", "trends", "calculator"]
        }

    async def _handle_complaint(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle complaints."""
        return {
            "text": "I'm sorry to hear you're having issues. Your feedback is important to us. Could you please tell me more about what's wrong so I can help resolve it or connect you with the right person?",
            "suggestions": [
                "Technical issue",
                "Customer service",
                "Report a problem"
            ],
            "actions": ["technical_support", "customer_service", "report_issue"]
        }

    async def _handle_compliment(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle compliments."""
        return {
            "text": "Thank you so much for your kind words! I'm delighted I could help you. Is there anything else I can assist you with today?",
            "suggestions": [
                "Continue browsing",
                "Save favorites",
                "Share with friends"
            ],
            "actions": ["continue", "save", "share"]
        }

    async def _handle_unknown(
        self,
        state: ConversationState,
        message: str,
        entities: List[Dict[str, Any]],
        language: str,
        user_profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Handle unknown/unrecognized intents."""
        return {
            "text": "I'm not sure I understood that correctly. Could you please rephrase your question or try one of these common tasks?",
            "suggestions": [
                "Search for properties",
                "Get property details",
                "Calculate mortgage"
            ],
            "actions": ["search", "details", "calculator"]
        }

    # Helper methods
    def _personalize_response(
        self,
        response: str,
        state: ConversationState,
        user_profile: Dict[str, Any],
        language: str
    ) -> str:
        """Personalize response based on user profile and conversation state."""
        # Add personalization here
        # For now, return as-is
        return response

    async def _search_properties(
        self,
        criteria: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """Search for properties based on criteria."""
        # Simulate property search
        results = []
        for i in range(5):
            result = {
                "id": f"PROP_{i+1:03d}",
                "title": f"Beautiful {criteria.get('bedrooms', 3)} Bedroom {criteria.get('property_type', 'House')}",
                "address": f"{criteria.get('location', 'Downtown')} - Street {i+1}",
                "price": criteria.get('price_range', {}).get('max', 500000),
                "bedrooms": criteria.get('bedrooms', 3),
                "bathrooms": criteria.get('bathrooms', 2),
                "square_feet": criteria.get('square_feet', 2000),
                "image_url": f"https://example.com/property_{i+1}.jpg"
            }
            results.append(result)
        return results

    async def _get_property_details(
        self,
        property_id: str
    ) -> Optional[Dict[str, Any]]:
        """Get detailed property information."""
        # Simulate property details
        return {
            "id": property_id,
            "title": "Modern 3 Bedroom House",
            "address": "123 Main Street, Downtown",
            "price": 450000,
            "bedrooms": 3,
            "bathrooms": 2,
            "square_feet": 2100,
            "year_built": 2015,
            "description": "Beautiful modern home with open concept living area, updated kitchen, and master suite with walk-in closet.",
            "features": ["Hardwood floors", "Granite countertops", "Stainless steel appliances", "Fireplace", "2-car garage"],
            "image_urls": [
                "https://example.com/property1_1.jpg",
                "https://example.com/property1_2.jpg"
            ]
        }

    async def _get_neighborhood_info(
        self,
        location: str
    ) -> Dict[str, Any]:
        """Get neighborhood information."""
        return {
            "name": location,
            "description": f"{location} is a vibrant neighborhood with excellent schools, parks, and shopping centers.",
            "schools": [
                {"name": "Lincoln Elementary", "rating": 9.2},
                {"name": "Washington High School", "rating": 8.8}
            ],
            "amenities": ["Parks", "Shopping Centers", "Restaurants", "Public Transit"],
            "crime_rate": "Low",
            "median_price": 425000,
            "walk_score": 85
        }

    async def _calculate_mortgage(
        self,
        price: float,
        down_payment: Optional[float] = None,
        interest_rate: Optional[float] = None,
        loan_term: Optional[int] = None
    ) -> Dict[str, Any]:
        """Calculate mortgage payments."""
        # Default values
        down_payment = down_payment or (price * 0.2)
        interest_rate = interest_rate or 4.5
        loan_term = loan_term or 30

        # Calculate loan amount
        loan_amount = price - down_payment

        # Calculate monthly payment
        monthly_rate = interest_rate / 100 / 12
        num_payments = loan_term * 12

        if monthly_rate == 0:
            monthly_payment = loan_amount / num_payments
        else:
            monthly_payment = loan_amount * (
                monthly_rate * (1 + monthly_rate) ** num_payments /
                ((1 + monthly_rate) ** num_payments - 1)
            )

        total_payment = monthly_payment * num_payments
        total_interest = total_payment - loan_amount

        return {
            "property_price": price,
            "down_payment": down_payment,
            "loan_amount": loan_amount,
            "interest_rate": interest_rate,
            "loan_term_years": loan_term,
            "monthly_payment": round(monthly_payment, 2),
            "total_payment": round(total_payment, 2),
            "total_interest": round(total_interest, 2)
        }

    async def _compare_properties(
        self,
        property_ids: List[str]
    ) -> Dict[str, Any]:
        """Compare multiple properties."""
        properties = []
        for pid in property_ids:
            props = await self._get_property_details(pid)
            if props:
                properties.append(props)

        if len(properties) < 2:
            return {"error": "Need at least 2 properties to compare"}

        # Create comparison
        comparison = {
            "properties": properties,
            "comparison_matrix": {
                "price": [p["price"] for p in properties],
                "bedrooms": [p["bedrooms"] for p in properties],
                "bathrooms": [p["bathrooms"] for p in properties],
                "square_feet": [p["square_feet"] for p in properties],
                "price_per_sqft": [
                    round(p["price"] / p["square_feet"], 2)
                    for p in properties
                ]
            }
        }

        return comparison

    async def _save_message(
        self,
        state: ConversationState,
        message: str,
        response: Dict[str, Any],
        language: str
    ):
        """Save message and response to conversation."""
        timestamp = datetime.utcnow().isoformat()

        # Save user message
        state.messages.append({
            "timestamp": timestamp,
            "sender": "user",
            "text": message,
            "language": language
        })

        # Save bot response
        state.messages.append({
            "timestamp": timestamp,
            "sender": "bot",
            "text": response["text"],
            "intent": response.get("intent"),
            "language": language,
            "suggestions": response.get("suggestions", []),
            "actions": response.get("actions", []),
            "context": response.get("context", {})
        })

        # Limit message history to last 100 messages
        if len(state.messages) > 100:
            state.messages = state.messages[-100:]

    async def _log_interaction(
        self,
        user_id: str,
        conversation_id: str,
        message: str,
        response: Dict[str, Any],
        intent: str,
        entities: List[Dict[str, Any]],
        language: str
    ):
        """Log interaction for analytics."""
        # Log to analytics system
        pass

    async def _load_conversation_history(self):
        """Load conversation history from storage."""
        # Implement loading from database or file
        pass

    async def _save_conversation_history(self):
        """Save conversation history to storage."""
        # Implement saving to database or file
        pass

    async def _load_user_profiles(self):
        """Load user profiles from storage."""
        # Implement loading from database or file
        pass

    async def _save_user_profiles(self):
        """Save user profiles to storage."""
        # Implement saving to database or file
        pass

    async def close_conversation(self, conversation_id: str) -> bool:
        """Close a conversation and save history."""
        try:
            if conversation_id in self.conversations:
                # Save conversation before closing
                await self._save_conversation_history()

                # Remove from active conversations
                del self.conversations[conversation_id]

                logger.info(
                    "Conversation closed",
                    conversation_id=conversation_id
                )
                return True
            return False
        except Exception as e:
            logger.error(
                "Failed to close conversation",
                conversation_id=conversation_id,
                error=str(e)
            )
            return False

    def get_active_conversations(self, user_id: Optional[str] = None) -> List[str]:
        """Get list of active conversation IDs."""
        if user_id:
            return [
                cid for cid, state in self.conversations.items()
                if state.user_id == user_id
            ]
        return list(self.conversations.keys())