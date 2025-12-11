"""
Customer Pilot Program Module

Manages pilot customer onboarding and validation:
- Customer registration and setup
- API key management
- Usage monitoring
- Feedback collection
- Success metrics tracking
"""

import asyncio
import json
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from enum import Enum
import uuid
from pathlib import Path

import pandas as pd
from fastapi import HTTPException, status
from pydantic import BaseModel, Field, EmailStr
import jwt
from passlib.context import CryptContext
from jinja2 import Template

from ..core.config import get_settings
from ..core.logging import get_logger
from ..core.exceptions import ValidationError

logger = get_logger(__name__)
settings = get_settings()

# Password context for hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class CustomerTier(str, Enum):
    """Customer subscription tiers."""
    TRIAL = "trial"
    STARTUP = "startup"
    PROFESSIONAL = "professional"
    ENTERPRISE = "enterprise"


class OnboardingStatus(str, Enum):
    """Onboarding status."""
    REGISTERED = "registered"
    API_ACCESS = "api_access"
    INTEGRATION = "integration"
    TESTING = "testing"
    ACTIVE = "active"
    CHURNED = "churned"


class CustomerRegistration(BaseModel):
    """Customer registration data."""
    company_name: str = Field(..., description="Company name")
    contact_name: str = Field(..., description="Primary contact name")
    email: EmailStr = Field(..., description="Contact email")
    phone: str = Field(..., description="Contact phone")
    company_size: str = Field(..., description="Company size (e.g., '1-10', '11-50')")
    industry: str = Field(..., description="Industry sector")
    use_case: str = Field(..., description="Primary use case")
    expected_volume: str = Field(..., description="Expected monthly API calls")
    technical_contact: Optional[EmailStr] = Field(None, description="Technical contact email")
    pilot_start_date: Optional[datetime] = Field(None, description="Desired pilot start")


class CustomerProfile(BaseModel):
    """Customer profile information."""
    customer_id: str
    registration: CustomerRegistration
    tier: CustomerTier
    status: OnboardingStatus
    created_at: datetime
    api_keys: List[Dict[str, Any]] = Field(default_factory=list)
    usage_quota: Dict[str, int] = Field(default_factory=dict)
    current_usage: Dict[str, int] = Field(default_factory=dict)
    webhook_url: Optional[str] = None
    integration_status: Dict[str, bool] = Field(default_factory=dict)
    support_tickets: List[Dict] = Field(default_factory=list)
    feedback_scores: List[Dict] = Field(default_factory=list)
    metrics: Dict[str, Any] = Field(default_factory=dict)


class UsageMetric(BaseModel):
    """API usage metric."""
    customer_id: str
    endpoint: str
    timestamp: datetime
    response_time: float
    status_code: int
    tokens_used: Optional[int] = None
    model_used: Optional[str] = None


class CustomerFeedback(BaseModel):
    """Customer feedback entry."""
    customer_id: str
    rating: int = Field(..., ge=1, le=5, description="Rating from 1-5")
    category: str = Field(..., description="Feedback category")
    comments: str = Field(..., description="Feedback comments")
    feature_requests: List[str] = Field(default_factory=list)
    bugs_reported: List[str] = Field(default_factory=list)
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class PilotCustomerManager:
    """Manages pilot customer onboarding and lifecycle."""

    def __init__(self):
        self.customers = {}
        self.onboarding_templates = self._load_onboarding_templates()
        self.pilot_metrics = self._init_metrics()

    def _load_onboarding_templates(self) -> Dict:
        """Load onboarding email templates."""
        return {
            "welcome": """
            <h2>Welcome to Gogidix AI Services Pilot Program!</h2>
            <p>Hi {{ contact_name }},</p>
            <p>Thank you for registering for our pilot program. We're excited to help you
            integrate AI into your {{ industry }} business.</p>
            <h3>Next Steps:</h3>
            <ol>
                <li>Receive your API keys (below)</li>
                <li>Review our <a href="https://docs.gogidix.com">documentation</a></li>
                <li>Test our <a href="https://api.gogidix.com/docs">interactive API</a></li>
                <li>Schedule your onboarding call</li>
            </ol>
            <h3>Your API Credentials:</h3>
            <p><strong>API Key:</strong> {{ api_key }}</p>
            <p><strong>Environment:</strong> {{ environment }}</p>
            <p><strong>Dashboard:</strong> <a href="{{ dashboard_url }}">{{ dashboard_url }}</a></p>
            <p>For support, reply to this email or join our Slack community.</p>
            <p>Best regards,<br>The Gogidix Team</p>
            """,

            "integration_complete": """
            <h2>Integration Complete! 🎉</h2>
            <p>Congratulations {{ company_name }}!</p>
            <p>Your AI services integration is now live. Here are your pilot metrics:</p>
            <ul>
                <li>Days in pilot: {{ days_in_pilot }}</li>
                <li>Total API calls: {{ total_calls }}</li>
                <li>Average response time: {{ avg_response_time }}ms</li>
                <li>Success rate: {{ success_rate }}%</li>
            </ul>
            <p>Ready to scale? <a href="https://gogidix.com/pricing">View pricing plans</a></p>
            """,

            "pilot_ending": """
            <h2>Pilot Program Ending Soon</h2>
            <p>Hi {{ contact_name }},</p>
            <p>Your pilot program ends on {{ end_date }}. Let's discuss your results!</p>
            <p>Schedule a review call: <a href="{{ calendar_url }}">Book now</a></p>
            """
        }

    def _init_metrics(self) -> Dict:
        """Initialize pilot metrics tracking."""
        return {
            "total_customers": 0,
            "active_customers": 0,
            "total_api_calls": 0,
            "average_rating": 0,
            "conversion_rate": 0,
            "churn_rate": 0,
            "customer_tiers": {
                "trial": 0,
                "startup": 0,
                "professional": 0,
                "enterprise": 0
            }
        }

    async def register_customer(self, registration: CustomerRegistration) -> CustomerProfile:
        """Register a new pilot customer."""
        logger.info(f"Registering new customer: {registration.company_name}")

        # Check if customer already exists
        if any(c.email == registration.email for c in self.customers.values()):
            raise ValidationError("Email already registered")

        # Generate customer ID
        customer_id = f"cust_{uuid.uuid4().hex[:8]}"

        # Determine tier based on company size and expected volume
        tier = self._determine_tier(registration)

        # Create customer profile
        customer = CustomerProfile(
            customer_id=customer_id,
            registration=registration,
            tier=tier,
            status=OnboardingStatus.REGISTERED,
            created_at=datetime.utcnow(),
            usage_quota=self._get_tier_quota(tier),
            current_usage={},
            integration_status={
                "api_access": False,
                "webhook_configured": False,
                "first_call_made": False,
                "dashboard_accessed": False
            }
        )

        # Generate API keys
        api_key = self._generate_api_key(customer_id)
        customer.api_keys.append({
            "key_id": f"key_{uuid.uuid4().hex[:8]}",
            "api_key": api_key,
            "created_at": datetime.utcnow(),
            "last_used": None,
            "status": "active"
        })

        # Save customer
        self.customers[customer_id] = customer

        # Send welcome email
        await self._send_onboarding_email(customer, "welcome", {
            "api_key": api_key,
            "environment": "pilot",
            "dashboard_url": f"https://pilot.gogidix.com/dashboard/{customer_id}"
        })

        # Update metrics
        self.pilot_metrics["total_customers"] += 1

        logger.info(f"Customer {customer_id} registered successfully")
        return customer

    def _determine_tier(self, registration: CustomerRegistration) -> CustomerTier:
        """Determine customer tier based on registration info."""
        # Simple tier determination logic
        if registration.company_size in ["1-10", "11-50"]:
            return CustomerTier.STARTUP
        elif registration.company_size in ["51-200", "201-500"]:
            return CustomerTier.PROFESSIONAL
        else:
            return CustomerTier.ENTERPRISE

    def _get_tier_quota(self, tier: CustomerTier) -> Dict[str, int]:
        """Get API quotas for tier."""
        quotas = {
            CustomerTier.TRIAL: {
                "daily_calls": 100,
                "monthly_calls": 1000,
                "concurrent_requests": 5,
                "storage_mb": 100
            },
            CustomerTier.STARTUP: {
                "daily_calls": 1000,
                "monthly_calls": 20000,
                "concurrent_requests": 20,
                "storage_mb": 1000
            },
            CustomerTier.PROFESSIONAL: {
                "daily_calls": 5000,
                "monthly_calls": 100000,
                "concurrent_requests": 100,
                "storage_mb": 10000
            },
            CustomerTier.ENTERPRISE: {
                "daily_calls": 50000,
                "monthly_calls": 1000000,
                "concurrent_requests": 1000,
                "storage_mb": 100000
            }
        }
        return quotas.get(tier, quotas[CustomerTier.TRIAL])

    def _generate_api_key(self, customer_id: str) -> str:
        """Generate API key for customer."""
        # Generate key with customer ID prefix
        key_body = uuid.uuid4().hex
        timestamp = int(datetime.utcnow().timestamp())
        signature = jwt.encode(
            {
                "customer_id": customer_id,
                "timestamp": timestamp,
                "type": "api_key"
            },
            settings.JWT_SECRET,
            algorithm="HS256"
        )
        return f"sk_pilot_{signature[:32]}"

    async def _send_onboarding_email(self, customer: CustomerProfile,
                                    template_name: str,
                                    context: Dict[str, Any]):
        """Send onboarding email using template."""
        template = Template(self.onboarding_templates[template_name])

        # Merge context with customer data
        full_context = {
            "contact_name": customer.registration.contact_name,
            "company_name": customer.registration.company_name,
            "industry": customer.registration.industry,
            **context
        }

        html_content = template.render(**full_context)

        # In production, use email service (SendGrid, SES, etc.)
        logger.info(f"Sending {template_name} email to {customer.registration.email}")
        # await email_service.send_email(
        #     to=customer.registration.email,
        #     subject=f"Gogidix AI Pilot: {template_name.replace('_', ' ').title()}",
        #     html_content=html_content
        # )

    async def track_api_usage(self, metric: UsageMetric):
        """Track API usage for customer."""
        customer = self.customers.get(metric.customer_id)
        if not customer:
            logger.warning(f"Usage for unknown customer: {metric.customer_id}")
            return

        # Update usage counters
        today = metric.timestamp.date()
        today_str = today.isoformat()

        if "daily" not in customer.current_usage:
            customer.current_usage["daily"] = {}

        if today_str not in customer.current_usage["daily"]:
            customer.current_usage["daily"][today_str] = 0

        customer.current_usage["daily"][today_str] += 1

        # Update current month
        month_str = today.strftime("%Y-%m")
        if "monthly" not in customer.current_usage:
            customer.current_usage["monthly"] = {}

        if month_str not in customer.current_usage["monthly"]:
            customer.current_usage["monthly"][month_str] = 0

        customer.current_usage["monthly"][month_str] += 1

        # Check integration milestones
        if not customer.integration_status["first_call_made"]:
            customer.integration_status["first_call_made"] = True
            await self._handle_milestone(customer, "first_api_call")

        # Check if approaching quota
        await self._check_usage_quota(customer)

        # Update global metrics
        self.pilot_metrics["total_api_calls"] += 1

    async def _handle_milestone(self, customer: CustomerProfile, milestone: str):
        """Handle customer milestone."""
        logger.info(f"Milestone reached for {customer.customer_id}: {milestone}")

        if milestone == "first_api_call":
            customer.status = OnboardingStatus.TESTING
            await self._send_onboarding_email(customer, "integration_complete", {
                "days_in_pilot": (datetime.utcnow() - customer.created_at).days,
                "total_calls": sum(customer.current_usage.get("monthly", {}).values()),
                "avg_response_time": "150",
                "success_rate": "99.5"
            })

    async def _check_usage_quota(self, customer: CustomerProfile):
        """Check if customer is approaching usage quota."""
        # Get current month usage
        current_month = datetime.utcnow().strftime("%Y-%m")
        monthly_usage = customer.current_usage.get("monthly", {}).get(current_month, 0)
        monthly_quota = customer.usage_quota.get("monthly_calls", 0)

        # Check if at 80% of quota
        if monthly_usage >= monthly_quota * 0.8 and monthly_usage < monthly_quota:
            logger.info(f"Customer {customer.customer_id} at 80% of quota")
            # Send quota warning email
            pass
        elif monthly_usage >= monthly_quota:
            logger.warning(f"Customer {customer.customer_id} exceeded quota")
            # Could implement rate limiting here

    async def collect_feedback(self, feedback: CustomerFeedback):
        """Collect customer feedback."""
        customer = self.customers.get(feedback.customer_id)
        if not customer:
            raise ValidationError("Customer not found")

        # Store feedback
        customer.feedback_scores.append(feedback.dict())

        # Calculate average rating
        if customer.feedback_scores:
            ratings = [f["rating"] for f in customer.feedback_scores]
            customer.metrics["average_rating"] = sum(ratings) / len(ratings)

        # Update global metrics
        await self._update_global_metrics()

        # Handle low ratings
        if feedback.rating <= 2:
            await self._handle_low_feedback(customer, feedback)

    async def _handle_low_feedback(self, customer: CustomerProfile,
                                  feedback: CustomerFeedback):
        """Handle low customer feedback."""
        logger.warning(f"Low feedback from {customer.customer_id}: {feedback.rating}/5")

        # Create support ticket
        ticket_id = f"ticket_{uuid.uuid4().hex[:8]}"
        ticket = {
            "ticket_id": ticket_id,
            "created_at": datetime.utcnow(),
            "priority": "high",
            "category": feedback.category,
            "description": feedback.comments,
            "status": "open"
        }
        customer.support_tickets.append(ticket)

        # Notify customer success team
        # await slack_service.send_alert(
        #     channel="#customer-success",
        #     message=f"Low feedback alert from {customer.registration.company_name}: {feedback.comments}"
        # )

    async def get_customer_dashboard(self, customer_id: str) -> Dict[str, Any]:
        """Get customer dashboard data."""
        customer = self.customers.get(customer_id)
        if not customer:
            raise ValidationError("Customer not found")

        # Calculate metrics
        current_month = datetime.utcnow().strftime("%Y-%m")
        monthly_usage = customer.current_usage.get("monthly", {}).get(current_month, 0)
        daily_usage = customer.current_usage.get("daily", {})

        # Get recent activity
        recent_calls = sum(
            count for date, count in daily_usage.items()
            if datetime.fromisoformat(date) >= datetime.utcnow() - timedelta(days=7)
        )

        # Calculate integration progress
        completed_steps = sum(
            1 for completed in customer.integration_status.values() if completed
        )
        total_steps = len(customer.integration_status)
        integration_progress = (completed_steps / total_steps) * 100

        # Generate recommendations
        recommendations = await self._generate_recommendations(customer)

        return {
            "customer_info": {
                "company_name": customer.registration.company_name,
                "tier": customer.tier.value,
                "status": customer.status.value,
                "pilot_days": (datetime.utcnow() - customer.created_at).days
            },
            "usage_metrics": {
                "monthly_calls": monthly_usage,
                "monthly_quota": customer.usage_quota.get("monthly_calls", 0),
                "usage_percentage": (monthly_usage / customer.usage_quota.get("monthly_calls", 1)) * 100,
                "recent_calls": recent_calls
            },
            "integration_progress": {
                "percentage": integration_progress,
                "completed_steps": completed_steps,
                "total_steps": total_steps,
                "status": customer.integration_status
            },
            "feedback": {
                "average_rating": customer.metrics.get("average_rating", 0),
                "total_feedback": len(customer.feedback_scores),
                "last_feedback": customer.feedback_scores[-1] if customer.feedback_scores else None
            },
            "support": {
                "open_tickets": len([t for t in customer.support_tickets if t["status"] == "open"]),
                "total_tickets": len(customer.support_tickets)
            },
            "recommendations": recommendations,
            "quick_actions": [
                "View API documentation",
                "Schedule support call",
                "Submit feedback",
                "Upgrade plan"
            ]
        }

    async def _generate_recommendations(self, customer: CustomerProfile) -> List[str]:
        """Generate personalized recommendations."""
        recommendations = []

        # Check usage patterns
        current_month = datetime.utcnow().strftime("%Y-%m")
        usage = customer.current_usage.get("monthly", {}).get(current_month, 0)
        quota = customer.usage_quota.get("monthly_calls", 0)

        if usage == 0:
            recommendations.append("Make your first API call to get started")
        elif usage < quota * 0.1:
            recommendations.append("Try our image analysis features")
        elif usage > quota * 0.8:
            recommendations.append("Consider upgrading your plan for more API calls")

        # Check integration status
        if not customer.integration_status["webhook_configured"]:
            recommendations.append("Set up webhooks for real-time updates")

        # Check feedback
        if not customer.feedback_scores:
            recommendations.append("Share your feedback to help us improve")

        # Check pilot duration
        pilot_days = (datetime.utcnow() - customer.created_at).days
        if pilot_days > 25:
            recommendations.append("Your pilot is ending soon. Schedule a review call")

        return recommendations

    async def update_global_metrics(self):
        """Update global pilot metrics."""
        active_customers = sum(
            1 for c in self.customers.values()
            if c.status in [OnboardingStatus.API_ACCESS, OnboardingStatus.INTEGRATION,
                           OnboardingStatus.TESTING, OnboardingStatus.ACTIVE]
        )

        # Calculate average rating
        all_ratings = []
        for customer in self.customers.values():
            all_ratings.extend([f["rating"] for f in customer.feedback_scores])

        avg_rating = sum(all_ratings) / len(all_ratings) if all_ratings else 0

        # Update metrics
        self.pilot_metrics.update({
            "active_customers": active_customers,
            "average_rating": avg_rating,
            "customer_tiers": {
                tier.value: sum(1 for c in self.customers.values() if c.tier == tier)
                for tier in CustomerTier
            }
        })

    async def get_pilot_analytics(self) -> Dict[str, Any]:
        """Get comprehensive pilot program analytics."""
        await self.update_global_metrics()

        # Customer acquisition funnel
        customers_by_status = {}
        for status in OnboardingStatus:
            customers_by_status[status.value] = sum(
                1 for c in self.customers.values() if c.status == status
            )

        # Usage trends
        daily_usage = {}
        for customer in self.customers.values():
            for date, count in customer.current_usage.get("daily", {}).items():
                daily_usage[date] = daily_usage.get(date, 0) + count

        # Sort dates
        sorted_usage = sorted(daily_usage.items(), key=lambda x: x[0])
        usage_trend = [{"date": date, "calls": count} for date, count in sorted_usage[-30:]]

        # Feature usage
        feature_usage = {}
        for customer in self.customers.values():
            for ticket in customer.support_tickets:
                category = ticket["category"]
                feature_usage[category] = feature_usage.get(category, 0) + 1

        # Feedback analysis
        feedback_by_category = {}
        for customer in self.customers.values():
            for feedback in customer.feedback_scores:
                category = feedback["category"]
                if category not in feedback_by_category:
                    feedback_by_category[category] = []
                feedback_by_category[category].append(feedback["rating"])

        avg_ratings_by_category = {
            cat: sum(ratings) / len(ratings)
            for cat, ratings in feedback_by_category.items()
        }

        return {
            "overview": self.pilot_metrics,
            "customer_funnel": customers_by_status,
            "usage_trends": usage_trend,
            "feature_usage": feature_usage,
            "feedback_analysis": {
                "average_by_category": avg_ratings_by_category,
                "total_feedback": sum(len(c.feedback_scores) for c in self.customers.values())
            },
            "top_performers": await self._get_top_performers(),
            "at_risk_customers": await self._get_at_risk_customers()
        }

    async def _get_top_performers(self) -> List[Dict]:
        """Get top performing pilot customers."""
        performers = []

        for customer in self.customers.values():
            # Calculate score based on usage, integration, and feedback
            usage_score = min(customer.current_usage.get("monthly", {}).get(
                datetime.utcnow().strftime("%Y-%m"), 0
            ) / customer.usage_quota.get("monthly_calls", 1), 1.0)

            integration_score = sum(
                1 for completed in customer.integration_status.values() if completed
            ) / len(customer.integration_status)

            feedback_score = customer.metrics.get("average_rating", 0) / 5

            overall_score = (usage_score * 0.4 + integration_score * 0.3 + feedback_score * 0.3)

            performers.append({
                "customer_id": customer.customer_id,
                "company_name": customer.registration.company_name,
                "overall_score": overall_score,
                "usage_score": usage_score,
                "integration_score": integration_score,
                "feedback_score": feedback_score
            })

        # Return top 10
        return sorted(performers, key=lambda x: x["overall_score"], reverse=True)[:10]

    async def _get_at_risk_customers(self) -> List[Dict]:
        """Get customers at risk of churn."""
        at_risk = []

        for customer in self.customers.values():
            risk_factors = []

            # Check usage
            current_month = datetime.utcnow().strftime("%Y-%m")
            usage = customer.current_usage.get("monthly", {}).get(current_month, 0)
            if usage == 0:
                risk_factors.append("No usage this month")
            elif usage < customer.usage_quota.get("monthly_calls", 1) * 0.1:
                risk_factors.append("Low usage")

            # Check feedback
            recent_feedback = [
                f for f in customer.feedback_scores
                if (datetime.utcnow() - f["timestamp"]).days <= 30
            ]
            if recent_feedback:
                avg_rating = sum(f["rating"] for f in recent_feedback) / len(recent_feedback)
                if avg_rating <= 2:
                    risk_factors.append("Low satisfaction rating")

            # Check support tickets
            open_tickets = len([t for t in customer.support_tickets if t["status"] == "open"])
            if open_tickets > 3:
                risk_factors.append("Multiple open support tickets")

            # Check pilot duration
            pilot_days = (datetime.utcnow() - customer.created_at).days
            if pilot_days > 30 and customer.status != OnboardingStatus.ACTIVE:
                risk_factors.append("Extended pilot without activation")

            if risk_factors:
                at_risk.append({
                    "customer_id": customer.customer_id,
                    "company_name": customer.registration.company_name,
                    "risk_factors": risk_factors,
                    "risk_score": len(risk_factors) / 4  # Normalize to 0-1
                })

        return sorted(at_risk, key=lambda x: x["risk_score"], reverse=True)


# Global instance
pilot_manager = PilotCustomerManager()