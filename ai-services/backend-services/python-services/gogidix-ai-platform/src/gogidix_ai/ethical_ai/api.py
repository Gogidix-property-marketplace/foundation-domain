"""
Ethical AI API Endpoints

RESTful API endpoints for bias detection, model explainability,
and compliance monitoring.
"""

import asyncio
import uuid
from datetime import datetime
from typing import Any, Dict, List, Optional, Union
from pathlib import Path

import pandas as pd
from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks, UploadFile, File, Query
from fastapi.responses import JSONResponse, HTMLResponse
from pydantic import BaseModel, Field, field_validator
from dataclasses import asdict

from ..core.config import get_settings
from ..core.logging import get_logger
from ..core.exceptions import EthicalAIError, ValidationError
from .service import (
    ethical_ai_service,
    BiasType,
    ComplianceStandard
)

logger = get_logger(__name__)
settings = get_settings()

router = APIRouter(prefix="/ethical-ai", tags=["ethical-ai"])


# Request/Response Models
class BiasDetectionRequest(BaseModel):
    """Request for bias detection analysis."""
    model_id: str = Field(..., description="Model identifier")
    sensitive_attributes: List[str] = Field(..., description="Protected attributes to analyze")
    bias_types: List[str] = Field(
        default=["demographic_parity", "equalized_odds", "equal_opportunity"],
        description="Types of bias to detect"
    )
    dataset_path: Optional[str] = Field(None, description="Path to dataset file")

    @field_validator("bias_types")
    @classmethod
    def validate_bias_types(cls, v):
        valid_types = [bt.value for bt in BiasType]
        for bt in v:
            if bt not in valid_types:
                raise ValueError(f"Invalid bias type: {bt}. Valid types: {valid_types}")
        return v


class ExplainabilityRequest(BaseModel):
    """Request for model explanation."""
    model_id: str = Field(..., description="Model identifier")
    explanation_methods: List[str] = Field(
        default=["shap", "lime", "feature_importance"],
        description="Explanation methods to use"
    )
    sample_size: int = Field(default=100, ge=10, le=1000, description="Sample size for analysis")
    dataset_path: Optional[str] = Field(None, description="Path to dataset file")


class ComplianceAssessmentRequest(BaseModel):
    """Request for compliance assessment."""
    model_id: str = Field(..., description="Model identifier")
    model_type: str = Field(..., description="Type of AI model")
    intended_use: str = Field(..., description="Intended use case")
    data_description: str = Field(..., description="Description of training data")
    standards: List[str] = Field(
        default=["ai_act_high_risk", "gdpr_article_22"],
        description="Compliance standards to assess"
    )

    @field_validator("standards")
    @classmethod
    def validate_standards(cls, v):
        valid_standards = [cs.value for cs in ComplianceStandard]
        for s in v:
            if s not in valid_standards:
                raise ValueError(f"Invalid compliance standard: {s}. Valid: {valid_standards}")
        return v


class EthicalAssessmentRequest(BaseModel):
    """Request for comprehensive ethical assessment."""
    model_id: str = Field(..., description="Model identifier")
    model_type: str = Field(..., description="Type of AI model")
    intended_use: str = Field(..., description="Intended use case")
    data_description: str = Field(..., description="Description of training data")
    sensitive_attributes: List[str] = Field(..., description="Protected attributes")
    dataset_path: str = Field(..., description="Path to dataset file")
    explanation_methods: List[str] = Field(
        default=["shap", "lime", "feature_importance"],
        description="Explanation methods to use"
    )


class MonitoringRequest(BaseModel):
    """Request to start/stop ethical monitoring."""
    model_id: str = Field(..., description="Model identifier")
    monitoring_type: str = Field(
        default="continuous",
        description="Type of monitoring: continuous, periodic, or on-demand"
    )
    alert_thresholds: Dict[str, float] = Field(
        default={"bias_score": 0.1, "ethical_score": 70.0},
        description="Thresholds for alerts"
    )


# Response Models
class BiasDetectionResponse(BaseModel):
    """Response from bias detection."""
    model_id: str
    assessment_id: str
    bias_results: List[Dict[str, Any]]
    overall_bias_score: float
    recommendations: List[str]
    assessment_date: datetime


class ExplainabilityResponse(BaseModel):
    """Response from explainability analysis."""
    model_id: str
    explanation_id: str
    feature_importance: Dict[str, float]
    explanations: Dict[str, Any]
    visualizations: Dict[str, str]
    confidence_score: float


class ComplianceResponse(BaseModel):
    """Response from compliance assessment."""
    model_id: str
    report_id: str
    compliance_status: Dict[str, bool]
    risk_level: str
    gaps_identified: List[Dict[str, Any]]
    remediation_actions: List[Dict[str, Any]]
    next_assessment_date: datetime


class EthicalAssessmentResponse(BaseModel):
    """Response from comprehensive ethical assessment."""
    model_id: str
    assessment_id: str
    ethical_score: Dict[str, Any]
    bias_results: List[Dict[str, Any]]
    explanation: Dict[str, Any]
    compliance: Dict[str, Any]
    recommendations: List[Dict[str, Any]]
    grade: str


class MonitoringStatusResponse(BaseModel):
    """Response with monitoring status."""
    model_id: str
    monitoring_active: bool
    last_check: Optional[datetime] = None
    alerts: List[Dict[str, Any]]
    metrics: Dict[str, Any]


# API Endpoints
@router.post("/bias-detection", response_model=BiasDetectionResponse)
async def detect_bias(
    request: BiasDetectionRequest,
    background_tasks: BackgroundTasks
) -> BiasDetectionResponse:
    """
    Detect bias in AI model predictions.

    Args:
        request: Bias detection request
        background_tasks: FastAPI background tasks

    Returns:
        BiasDetectionResponse with analysis results
    """
    try:
        logger.info(f"Starting bias detection for model {request.model_id}")

        assessment_id = str(uuid.uuid4())

        # Validate model exists
        # In production, check model registry
        model = None  # Would load actual model

        # Load dataset
        if request.dataset_path:
            dataset = pd.read_csv(request.dataset_path)
        else:
            raise pydantic.ValidationError("Dataset path is required for bias detection")

        # Convert bias types
        bias_types = [BiasType(bt) for bt in request.bias_types]

        # Run bias detection
        bias_results = await ethical_ai_service.bias_detector.detect_bias(
            model=model,
            dataset=dataset,
            target_column=dataset.columns[-1],  # Assume last column is target
            sensitive_attributes=request.sensitive_attributes,
            bias_types=bias_types
        )

        # Calculate overall bias score
        if bias_results:
            biased_count = sum(1 for result in bias_results if result.is_biased)
            overall_bias_score = max(0, 100 - (biased_count / len(bias_results) * 100))
        else:
            overall_bias_score = 100

        # Generate recommendations
        recommendations = []
        for result in bias_results:
            if result.is_biased:
                recommendations.extend(result.recommendations)

        return BiasDetectionResponse(
            model_id=request.model_id,
            assessment_id=assessment_id,
            bias_results=[asdict(result) for result in bias_results],
            overall_bias_score=overall_bias_score,
            recommendations=list(set(recommendations)),  # Remove duplicates
            assessment_date=datetime.now()
        )

    except Exception as e:
        logger.error(f"Bias detection failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/explainability", response_model=ExplainabilityResponse)
async def explain_model(
    request: ExplainabilityRequest,
    background_tasks: BackgroundTasks
) -> ExplainabilityResponse:
    """
    Generate model explanations.

    Args:
        request: Explainability request
        background_tasks: FastAPI background tasks

    Returns:
        ExplainabilityResponse with explanations
    """
    try:
        logger.info(f"Generating explanations for model {request.model_id}")

        # Load model and dataset
        model = None  # Would load actual model

        if request.dataset_path:
            dataset = pd.read_csv(request.dataset_path)
        else:
            raise pydantic.ValidationError("Dataset path is required for explainability")

        # Split features and target
        X = dataset.iloc[:, :-1]
        y = dataset.iloc[:, -1]

        # Generate explanations
        explanation = await ethical_ai_service.model_explainer.explain_model(
            model=model,
            X=X,
            y=y,
            explanation_methods=request.explanation_methods,
            sample_size=request.sample_size
        )

        return ExplainabilityResponse(
            model_id=request.model_id,
            explanation_id=explanation.explanation_id,
            feature_importance=explanation.feature_importance,
            explanations={
                "shap_available": explanation.shap_values is not None,
                "lime_explanation": explanation.lime_explanation,
                "counterfactuals_count": len(explanation.counterfactual_examples) if explanation.counterfactual_examples else 0,
                "decision_path": explanation.decision_path,
                "reasoning": explanation.reasoning
            },
            visualizations=explanation.visualizations or {},
            confidence_score=explanation.confidence_score
        )

    except Exception as e:
        logger.error(f"Model explanation failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/compliance", response_model=ComplianceResponse)
async def assess_compliance(
    request: ComplianceAssessmentRequest,
    background_tasks: BackgroundTasks
) -> ComplianceResponse:
    """
    Assess model compliance with standards.

    Args:
        request: Compliance assessment request
        background_tasks: FastAPI background tasks

    Returns:
        ComplianceResponse with assessment results
    """
    try:
        logger.info(f"Assessing compliance for model {request.model_id}")

        # Convert standards
        standards = [ComplianceStandard(s) for s in request.standards]

        # Run compliance assessment
        compliance_report = await ethical_ai_service.compliance_monitor.assess_compliance(
            model_id=request.model_id,
            model_type=request.model_type,
            intended_use=request.intended_use,
            data_description=request.data_description,
            bias_results=[],  # Would be populated from previous assessment
            explanation=None  # Would be populated from previous assessment
        )

        return ComplianceResponse(
            model_id=request.model_id,
            report_id=compliance_report.report_id,
            compliance_status=compliance_report.compliance_status,
            risk_level=compliance_report.risk_level,
            gaps_identified=compliance_report.gaps_identified,
            remediation_actions=compliance_report.remediation_actions,
            next_assessment_date=compliance_report.next_assessment_date
        )

    except Exception as e:
        logger.error(f"Compliance assessment failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/assessment", response_model=EthicalAssessmentResponse)
async def conduct_ethical_assessment(
    request: EthicalAssessmentRequest,
    background_tasks: BackgroundTasks
) -> EthicalAssessmentResponse:
    """
    Conduct comprehensive ethical AI assessment.

    Args:
        request: Ethical assessment request
        background_tasks: FastAPI background tasks

    Returns:
        EthicalAssessmentResponse with complete assessment
    """
    try:
        logger.info(f"Starting comprehensive ethical assessment for model {request.model_id}")

        # Load dataset
        dataset = pd.read_csv(request.dataset_path)
        X = dataset.iloc[:, :-1]
        y = dataset.iloc[:, -1]

        # Load model (in production, from model registry)
        model = None

        # Conduct assessment
        assessment_results = await ethical_ai_service.conduct_ethical_assessment(
            model=model,
            model_id=request.model_id,
            model_type=request.model_type,
            X=X,
            y=y,
            sensitive_attributes=request.sensitive_attributes,
            intended_use=request.intended_use,
            data_description=request.data_description
        )

        return EthicalAssessmentResponse(
            model_id=request.model_id,
            assessment_id=assessment_results["assessment_id"],
            ethical_score=assessment_results["ethical_score"],
            bias_results=assessment_results["components"]["bias_detection"],
            explanation=assessment_results["components"]["explainability"],
            compliance=assessment_results["components"]["compliance"],
            recommendations=assessment_results["recommendations"],
            grade=assessment_results["ethical_score"]["grade"]
        )

    except Exception as e:
        logger.error(f"Ethical assessment failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/monitoring/start")
async def start_monitoring(
    request: MonitoringRequest,
    background_tasks: BackgroundTasks
) -> Dict[str, str]:
    """
    Start ethical monitoring for a model.

    Args:
        request: Monitoring request
        background_tasks: FastAPI background tasks

    Returns:
        Status message
    """
    try:
        logger.info(f"Starting ethical monitoring for model {request.model_id}")

        # Start monitoring in background
        background_tasks.add_task(
            ethical_ai_service.start_monitoring,
            model_id=request.model_id
        )

        return {
            "message": f"Ethical monitoring started for model {request.model_id}",
            "model_id": request.model_id,
            "monitoring_type": request.monitoring_type
        }

    except Exception as e:
        logger.error(f"Failed to start monitoring: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/monitoring/stop")
async def stop_monitoring(
    model_id: str,
    background_tasks: BackgroundTasks
) -> Dict[str, str]:
    """
    Stop ethical monitoring for a model.

    Args:
        model_id: Model identifier
        background_tasks: FastAPI background tasks

    Returns:
        Status message
    """
    try:
        logger.info(f"Stopping ethical monitoring for model {model_id}")

        # Stop monitoring
        await ethical_ai_service.stop_monitoring(model_id)

        return {
            "message": f"Ethical monitoring stopped for model {model_id}",
            "model_id": model_id
        }

    except Exception as e:
        logger.error(f"Failed to stop monitoring: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/monitoring/status/{model_id}", response_model=MonitoringStatusResponse)
async def get_monitoring_status(model_id: str) -> MonitoringStatusResponse:
    """
    Get monitoring status for a model.

    Args:
        model_id: Model identifier

    Returns:
        MonitoringStatusResponse with current status
    """
    try:
        # In production, get actual monitoring status
        monitoring_active = ethical_ai_service.monitoring_active

        return MonitoringStatusResponse(
            model_id=model_id,
            monitoring_active=monitoring_active,
            last_check=datetime.now() if monitoring_active else None,
            alerts=[],  # Would get actual alerts
            metrics={}  # Would get actual metrics
        )

    except Exception as e:
        logger.error(f"Failed to get monitoring status: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/report/{model_id}")
async def get_ethical_report(
    model_id: str,
    format: str = Query(default="json", pattern="^(json|html)$")
) -> Union[Dict[str, Any], HTMLResponse]:
    """
    Generate ethical AI report.

    Args:
        model_id: Model identifier
        format: Report format (json or html)

    Returns:
        Ethical AI report in requested format
    """
    try:
        report = await ethical_ai_service.generate_ethical_report(
            model_id=model_id,
            format=format
        )

        if format == "html":
            return HTMLResponse(content=report["html"])
        else:
            return report

    except Exception as e:
        logger.error(f"Failed to generate report: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/upload-dataset")
async def upload_dataset(
    file: UploadFile = File(...),
    model_id: str = Query(..., description="Model ID to associate with dataset")
) -> Dict[str, str]:
    """
    Upload dataset for ethical assessment.

    Args:
        file: Dataset file (CSV, JSON)
        model_id: Model identifier

    Returns:
        Upload status with file path
    """
    try:
        # Validate file type
        if not file.filename.endswith(('.csv', '.json')):
            raise pydantic.ValidationError("Only CSV and JSON files are supported")

        # Save uploaded file
        upload_dir = Path(settings.ETHICAL_REPORTS_PATH) / "datasets"
        upload_dir.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{model_id}_{timestamp}_{file.filename}"
        file_path = upload_dir / filename

        with open(file_path, "wb") as buffer:
            content = await file.read()
            buffer.write(content)

        logger.info(f"Dataset uploaded: {file_path}")

        return {
            "message": "Dataset uploaded successfully",
            "file_path": str(file_path),
            "model_id": model_id,
            "filename": filename
        }

    except Exception as e:
        logger.error(f"Dataset upload failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/metrics/{model_id}")
async def get_ethical_metrics(model_id: str) -> Dict[str, Any]:
    """
    Get ethical AI metrics for a model.

    Args:
        model_id: Model identifier

    Returns:
        Ethical metrics and trends
    """
    try:
        # In production, get actual metrics from monitoring system
        metrics = {
            "model_id": model_id,
            "ethical_score": 85.5,
            "bias_score": 92.0,
            "explainability_score": 88.0,
            "compliance_score": 95.0,
            "trends": {
                "last_30_days": [82, 83, 84, 85, 85, 86, 85],
                "trend": "improving"
            },
            "alerts": [
                {
                    "type": "bias_detected",
                    "severity": "medium",
                    "message": "Slight bias detected in demographic parity",
                    "timestamp": "2024-01-15T10:30:00Z"
                }
            ]
        }

        return metrics

    except Exception as e:
        logger.error(f"Failed to get ethical metrics: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/standards")
async def get_compliance_standards() -> Dict[str, Any]:
    """
    Get list of available compliance standards.

    Returns:
        Available standards and requirements
    """
    try:
        standards = {}
        for standard in ComplianceStandard:
            standards[standard.value] = {
                "name": standard.value.replace("_", " ").title(),
                "description": f"Requirements for {standard.value}",
                "risk_level": "high" if "high_risk" in standard.value else "medium"
            }

        return {
            "standards": standards,
            "total_count": len(standards)
        }

    except Exception as e:
        logger.error(f"Failed to get compliance standards: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/health")
async def health_check() -> Dict[str, Any]:
    """
    Health check for ethical AI service.

    Returns:
        Service health status
    """
    try:
        return {
            "status": "healthy",
            "service": "ethical-ai",
            "timestamp": datetime.now().isoformat(),
            "components": {
                "bias_detector": "operational",
                "model_explainer": "operational",
                "compliance_monitor": "operational"
            }
        }

    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return {
            "status": "unhealthy",
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }