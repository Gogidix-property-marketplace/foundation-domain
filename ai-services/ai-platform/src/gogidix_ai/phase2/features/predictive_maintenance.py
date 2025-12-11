"""
Predictive Maintenance Module

AI-powered predictive maintenance for property management:
- Equipment failure prediction
- Maintenance scheduling optimization
- Cost estimation
- Vendor recommendation
- IoT sensor integration
"""

import asyncio
import json
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple, Any
from enum import Enum
import uuid

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier, GradientBoostingRegressor
from sklearn.preprocessing import StandardScaler
import joblib
from pydantic import BaseModel, Field
from fastapi import HTTPException, status

from ..core.config import get_settings
from ..core.logging import get_logger
from ..core.exceptions import PredictionError

logger = get_logger(__name__)
settings = get_settings()


class EquipmentType(str, Enum):
    """Types of equipment in properties."""
    HVAC = "hvac"
    PLUMBING = "plumbing"
    ELECTRICAL = "electrical"
    APPLIANCE = "appliance"
    ROOF = "roof"
    FOUNDATION = "foundation"
    WINDOWS = "windows"
    INSULATION = "insulation"


class MaintenancePriority(str, Enum):
    """Maintenance priority levels."""
    CRITICAL = "critical"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"


class MaintenanceTask(BaseModel):
    """Maintenance task model."""
    task_id: str = Field(..., description="Task identifier")
    equipment_id: str = Field(..., description="Equipment identifier")
    equipment_type: EquipmentType = Field(..., description="Type of equipment")
    property_id: str = Field(..., description="Property identifier")
    priority: MaintenancePriority = Field(..., description="Task priority")
    description: str = Field(..., description="Task description")
    estimated_cost: float = Field(..., description="Estimated cost")
    estimated_duration: int = Field(..., description="Duration in hours")
    failure_probability: float = Field(..., description="Probability of failure")
    recommended_date: datetime = Field(..., description="Recommended maintenance date")
    vendor_recommendation: Optional[Dict[str, Any]] = Field(None, description="Vendor info")
    required_skills: List[str] = Field(default_factory=list)
    required_parts: List[str] = Field(default_factory=list)


class IoTReading(BaseModel):
    """IoT sensor reading."""
    sensor_id: str
    equipment_id: str
    timestamp: datetime
    readings: Dict[str, float]
    alerts: List[str] = Field(default_factory=list)


class Equipment(BaseModel):
    """Equipment information."""
    equipment_id: str
    property_id: str
    equipment_type: EquipmentType
    brand: str
    model: str
    install_date: datetime
    last_maintenance: Optional[datetime]
    warranty_expiry: Optional[datetime]
    expected_lifespan: int  # in years
    sensor_ids: List[str] = Field(default_factory=list)
    maintenance_history: List[Dict] = Field(default_factory=list)


class PredictiveMaintenanceService:
    """Main service for predictive maintenance."""

    def __init__(self):
        self.models = {}
        self.scalers = {}
        self.load_models()
        self.vendor_database = self._load_vendor_database()

    def load_models(self):
        """Load trained ML models."""
        try:
            # Load failure prediction models
            for equipment_type in EquipmentType:
                model_path = f"models/maintenance/failure_prediction_{equipment_type.value}.joblib"
                if Path(model_path).exists():
                    self.models[equipment_type.value] = joblib.load(model_path)
                    scaler_path = f"models/maintenance/scaler_{equipment_type.value}.joblib"
                    self.scalers[equipment_type.value] = joblib.load(scaler_path)
                    logger.info(f"Loaded model for {equipment_type.value}")
        except Exception as e:
            logger.warning(f"Failed to load models: {e}")

    def _load_vendor_database(self) -> Dict:
        """Load vendor database."""
        return {
            "hvac": [
                {
                    "name": "CoolAir Pro Services",
                    "rating": 4.8,
                    "response_time": 24,
                    "hourly_rate": 85,
                    "specialties": ["all_brands", "emergency"],
                    "coverage": ["downtown", "suburbs"]
                },
                {
                    "name": "Climate Control Experts",
                    "rating": 4.6,
                    "response_time": 48,
                    "hourly_rate": 75,
                    "specialties": ["carrier", "trane", "lennox"],
                    "coverage": ["metro_area"]
                }
            ],
            "plumbing": [
                {
                    "name": "AquaFix Plumbing",
                    "rating": 4.7,
                    "response_time": 12,
                    "hourly_rate": 70,
                    "specialties": ["emergency", "renovation"],
                    "coverage": ["all_areas"]
                }
            ],
            "electrical": [
                {
                    "name": "PowerPro Electric",
                    "rating": 4.9,
                    "response_time": 6,
                    "hourly_rate": 95,
                    "specialties": ["commercial", "residential", "emergency"],
                    "coverage": ["all_areas"]
                }
            ]
        }

    async def predict_failures(self, equipment: Equipment,
                             sensor_readings: List[IoTReading]) -> List[MaintenanceTask]:
        """Predict equipment failures and create maintenance tasks."""
        logger.info(f"Predicting failures for equipment {equipment.equipment_id}")

        tasks = []

        # Prepare features for prediction
        features = self._prepare_features(equipment, sensor_readings)

        # Make predictions
        if equipment.equipment_type.value in self.models:
            model = self.models[equipment.equipment_type.value]
            scaler = self.scalers[equipment.equipment_type.value]

            # Scale features
            features_scaled = scaler.transform([features])

            # Predict failure probability
            failure_prob = model.predict_proba(features_scaled)[0][1]

            # If high risk, create maintenance task
            if failure_prob > 0.3:  # 30% threshold
                task = await self._create_maintenance_task(
                    equipment, failure_prob, sensor_readings
                )
                tasks.append(task)

        return tasks

    def _prepare_features(self, equipment: Equipment,
                         sensor_readings: List[IoTReading]) -> List[float]:
        """Prepare features for ML model."""
        # Time-based features
        age_years = (datetime.utcnow() - equipment.install_date).days / 365
        days_since_maintenance = 0
        if equipment.last_maintenance:
            days_since_maintenance = (datetime.utcnow() - equipment.last_maintenance).days

        # Sensor-based features
        recent_readings = sensor_readings[-10:] if sensor_readings else []
        avg_temperature = 0
        avg_vibration = 0
        avg_power_consumption = 0
        anomaly_count = 0

        if recent_readings:
            temps = []
            vibrations = []
            powers = []

            for reading in recent_readings:
                if "temperature" in reading.readings:
                    temps.append(reading.readings["temperature"])
                if "vibration" in reading.readings:
                    vibrations.append(reading.readings["vibration"])
                if "power_consumption" in reading.readings:
                    powers.append(reading.readings["power_consumption"])
                anomaly_count += len(reading.alerts)

            avg_temperature = np.mean(temps) if temps else 0
            avg_vibration = np.mean(vibrations) if vibrations else 0
            avg_power_consumption = np.mean(powers) if powers else 0

        # Maintenance history features
        maintenance_count = len(equipment.maintenance_history)
        avg_repair_cost = 0
        if equipment.maintenance_history:
            costs = [m.get("cost", 0) for m in equipment.maintenance_history]
            avg_repair_cost = np.mean(costs)

        # Compile feature vector
        features = [
            age_years,
            days_since_maintenance,
            avg_temperature,
            avg_vibration,
            avg_power_consumption,
            anomaly_count,
            maintenance_count,
            avg_repair_cost,
            len(sensor_readings)  # Sensor count
        ]

        return features

    async def _create_maintenance_task(self, equipment: Equipment,
                                     failure_prob: float,
                                     sensor_readings: List[IoTReading]) -> MaintenanceTask:
        """Create a maintenance task based on prediction."""
        # Determine priority
        if failure_prob > 0.7:
            priority = MaintenancePriority.CRITICAL
            days_ahead = 3
        elif failure_prob > 0.5:
            priority = MaintenancePriority.HIGH
            days_ahead = 7
        elif failure_prob > 0.3:
            priority = MaintenancePriority.MEDIUM
            days_ahead = 14
        else:
            priority = MaintenancePriority.LOW
            days_ahead = 30

        # Estimate cost
        base_cost = self._get_base_repair_cost(equipment.equipment_type)
        cost_multiplier = 1 + (failure_prob * 2)  # Higher prob = higher urgency cost
        estimated_cost = base_cost * cost_multiplier

        # Estimate duration
        base_duration = self._get_base_repair_duration(equipment.equipment_type)
        estimated_duration = base_duration * (1 + failure_prob)

        # Recommend vendor
        vendor = self._recommend_vendor(equipment.equipment_type)

        # Determine required parts and skills
        required_parts = self._get_required_parts(equipment, sensor_readings)
        required_skills = self._get_required_skills(equipment.equipment_type)

        # Create task description
        description = self._generate_task_description(
            equipment, failure_prob, sensor_readings
        )

        task = MaintenanceTask(
            task_id=str(uuid.uuid4()),
            equipment_id=equipment.equipment_id,
            equipment_type=equipment.equipment_type,
            property_id=equipment.property_id,
            priority=priority,
            description=description,
            estimated_cost=estimated_cost,
            estimated_duration=int(estimated_duration),
            failure_probability=failure_prob,
            recommended_date=datetime.utcnow() + timedelta(days=days_ahead),
            vendor_recommendation=vendor,
            required_skills=required_skills,
            required_parts=required_parts
        )

        return task

    def _get_base_repair_cost(self, equipment_type: EquipmentType) -> float:
        """Get base repair cost for equipment type."""
        costs = {
            EquipmentType.HVAC: 500,
            EquipmentType.PLUMBING: 300,
            EquipmentType.ELECTRICAL: 400,
            EquipmentType.APPLIANCE: 200,
            EquipmentType.ROOF: 2000,
            EquipmentType.FOUNDATION: 5000,
            EquipmentType.WINDOWS: 600,
            EquipmentType.INSULATION: 800
        }
        return costs.get(equipment_type, 500)

    def _get_base_repair_duration(self, equipment_type: EquipmentType) -> int:
        """Get base repair duration in hours."""
        durations = {
            EquipmentType.HVAC: 4,
            EquipmentType.PLUMBING: 2,
            EquipmentType.ELECTRICAL: 3,
            EquipmentType.APPLIANCE: 2,
            EquipmentType.ROOF: 8,
            EquipmentType.FOUNDATION: 24,
            EquipmentType.WINDOWS: 3,
            EquipmentType.INSULATION: 6
        }
        return durations.get(equipment_type, 4)

    def _recommend_vendor(self, equipment_type: EquipmentType) -> Dict[str, Any]:
        """Recommend best vendor for equipment type."""
        vendors = self.vendor_database.get(equipment_type.value, [])
        if vendors:
            # Select vendor with highest rating
            best_vendor = max(vendors, key=lambda v: v["rating"])
            return best_vendor
        return {}

    def _get_required_parts(self, equipment: Equipment,
                          sensor_readings: List[IoTReading]) -> List[str]:
        """Determine required parts based on equipment and sensor data."""
        parts = []

        # Analyze sensor readings for specific issues
        for reading in sensor_readings[-5:]:  # Check recent readings
            if "temperature" in reading.readings:
                if reading.readings["temperature"] > 90:
                    parts.append("Thermal overload protector")
                elif reading.readings["temperature"] < 0:
                    parts.append("Heating element")

            if "vibration" in reading.readings:
                if reading.readings["vibration"] > 5.0:
                    parts.append("Motor bearings")
                    parts.append("Mounting hardware")

            if "pressure" in reading.readings:
                if reading.readings["pressure"] > 150:
                    parts.append("Pressure relief valve")
                elif reading.readings["pressure"] < 10:
                    parts.append("Pump seal")

        # Add common parts based on equipment type
        common_parts = {
            EquipmentType.HVAC: ["Air filter", "Refrigerant", "Capacitor"],
            EquipmentType.PLUMBING: ["Pipe seal", "Valve assembly", "Gasket kit"],
            EquipmentType.ELECTRICAL: ["Circuit breaker", "Wiring harness", "Fuse"],
            EquipmentType.APPLIANCE: ["Control board", "Heating element", "Seal gasket"]
        }

        parts.extend(common_parts.get(equipment.equipment_type, []))

        # Remove duplicates
        return list(set(parts))

    def _get_required_skills(self, equipment_type: EquipmentType) -> List[str]:
        """Get required skills for equipment type."""
        skills = {
            EquipmentType.HVAC: ["HVAC certification", "Refrigerant handling"],
            EquipmentType.PLUMBING: ["Plumbing license", "Pipe fitting"],
            EquipmentType.ELECTRICAL: ["Electrician license", "Knowledge of NEC"],
            EquipmentType.APPLIANCE: ["Appliance repair", "Electronics"],
            EquipmentType.ROOF: ["Roofing certification", "Safety training"],
            EquipmentType.FOUNDATION: ["Structural engineering", "Concrete work"],
            EquipmentType.WINDOWS: ["Window installation", "Glazing"],
            EquipmentType.INSULATION: ["Insulation installation", "Safety training"]
        }
        return skills.get(equipment_type, ["General maintenance"])

    def _generate_task_description(self, equipment: Equipment,
                                 failure_prob: float,
                                 sensor_readings: List[IoTReading]) -> str:
        """Generate task description based on analysis."""
        age_years = (datetime.utcnow() - equipment.install_date).days / 365

        description = f"Predictive maintenance for {equipment.brand} {equipment.model} "
        description += f"(age: {age_years:.1f} years). Risk level: {failure_prob:.1%}. "

        # Add specific issues based on sensor data
        if sensor_readings:
            latest = sensor_readings[-1]
            if latest.alerts:
                description += f"Active alerts: {', '.join(latest.alerts)}. "

            if "temperature" in latest.readings:
                temp = latest.readings["temperature"]
                if temp > 80:
                    description += "Operating at high temperature. "
                elif temp < 40:
                    description += "Operating at low temperature. "

        if failure_prob > 0.7:
            description += "Immediate attention required to prevent failure."
        elif failure_prob > 0.5:
            description += "Schedule maintenance within the week."
        else:
            description += "Routine maintenance recommended."

        return description

    async def optimize_maintenance_schedule(self,
                                          tasks: List[MaintenanceTask],
                                          constraints: Dict[str, Any]) -> Dict[str, Any]:
        """Optimize maintenance schedule based on constraints."""
        logger.info(f"Optimizing schedule for {len(tasks)} tasks")

        # Sort tasks by priority and failure probability
        sorted_tasks = sorted(
            tasks,
            key=lambda t: (t.priority.value, t.failure_probability),
            reverse=True
        )

        # Schedule tasks considering constraints
        schedule = []
        budget_remaining = constraints.get("budget", float('inf'))
        weekly_capacity = constraints.get("weekly_hours", 40)

        current_week = datetime.utcnow()
        week_hours_used = 0

        for task in sorted_tasks:
            # Check budget
            if task.estimated_cost > budget_remaining:
                continue

            # Check weekly capacity
            if week_hours_used + task.estimated_duration > weekly_capacity:
                current_week += timedelta(weeks=1)
                week_hours_used = 0

            # Schedule task
            scheduled_date = max(
                task.recommended_date,
                current_week
            )

            schedule.append({
                "task_id": task.task_id,
                "scheduled_date": scheduled_date,
                "duration": task.estimated_duration
            })

            # Update counters
            budget_remaining -= task.estimated_cost
            week_hours_used += task.estimated_duration

        # Calculate schedule metrics
        total_cost = sum(t.estimated_cost for t in tasks if any(
            s["task_id"] == t.task_id for s in schedule
        ))
        total_duration = sum(s["duration"] for s in schedule)
        completion_date = max(s["scheduled_date"] for s in schedule) if schedule else None

        return {
            "schedule": schedule,
            "metrics": {
                "tasks_scheduled": len(schedule),
                "tasks_deferred": len(tasks) - len(schedule),
                "total_cost": total_cost,
                "total_duration": total_duration,
                "completion_date": completion_date.isoformat() if completion_date else None
            }
        }


class MaintenanceScheduler:
    """Advanced maintenance scheduling with optimization."""

    def __init__(self):
        self.service = PredictiveMaintenanceService()

    async def create_maintenance_plan(self,
                                    property_id: str,
                                    equipment_list: List[Equipment],
                                    budget: Optional[float] = None,
                                    time_horizon: int = 90) -> Dict[str, Any]:
        """Create comprehensive maintenance plan."""
        logger.info(f"Creating maintenance plan for property {property_id}")

        # Get all sensor readings for equipment
        sensor_readings = await self._get_sensor_readings(equipment_list)

        # Predict failures for all equipment
        all_tasks = []
        for equipment in equipment_list:
            readings = sensor_readings.get(equipment.equipment_id, [])
            tasks = await self.service.predict_failures(equipment, readings)
            all_tasks.extend(tasks)

        # Add routine maintenance tasks
        routine_tasks = await self._generate_routine_tasks(equipment_list)
        all_tasks.extend(routine_tasks)

        # Optimize schedule
        constraints = {
            "budget": budget or float('inf'),
            "weekly_hours": 40,
            "time_horizon": time_horizon
        }

        optimized_schedule = await self.service.optimize_maintenance_schedule(
            all_tasks, constraints
        )

        # Group tasks by week
        weekly_tasks = self._group_tasks_by_week(optimized_schedule["schedule"])

        # Calculate total metrics
        total_predicted_cost = sum(t.estimated_cost for t in all_tasks)
        critical_tasks = [t for t in all_tasks if t.priority == MaintenancePriority.CRITICAL]

        return {
            "property_id": property_id,
            "plan_period": f"{time_horizon} days",
            "total_tasks": len(all_tasks),
            "critical_tasks": len(critical_tasks),
            "estimated_total_cost": total_predicted_cost,
            "optimized_schedule": optimized_schedule,
            "weekly_breakdown": weekly_tasks,
            "recommendations": await self._generate_recommendations(
                all_tasks, equipment_list
            )
        }

    async def _get_sensor_readings(self, equipment_list: List[Equipment]) -> Dict[str, List[IoTReading]]:
        """Get recent sensor readings for all equipment."""
        # In production, this would query the IoT database
        readings = {}

        for equipment in equipment_list:
            # Simulate recent readings
            readings[equipment.equipment_id] = [
                IoTReading(
                    sensor_id=f"sensor_{i}",
                    equipment_id=equipment.equipment_id,
                    timestamp=datetime.utcnow() - timedelta(hours=i),
                    readings={
                        "temperature": np.random.normal(70, 10),
                        "vibration": np.random.exponential(1),
                        "power_consumption": np.random.normal(500, 100)
                    },
                    alerts=[]
                )
                for i in range(24, 0, -3)  # Last 24 hours
            ]

        return readings

    async def _generate_routine_tasks(self, equipment_list: List[Equipment]) -> List[MaintenanceTask]:
        """Generate routine maintenance tasks."""
        tasks = []

        for equipment in equipment_list:
            # Check if routine maintenance is due
            days_since_maintenance = 999999  # Large number if no maintenance
            if equipment.last_maintenance:
                days_since_maintenance = (datetime.utcnow() - equipment.last_maintenance).days

            # Define maintenance intervals in days
            intervals = {
                EquipmentType.HVAC: 90,
                EquipmentType.PLUMBING: 180,
                EquipmentType.ELECTRICAL: 365,
                EquipmentType.APPLIANCE: 180,
                EquipmentType.ROOF: 180,
                EquipmentType.FOUNDATION: 365,
                EquipmentType.WINDOWS: 365,
                EquipmentType.INSULATION: 365
            }

            interval = intervals.get(equipment.equipment_type, 180)

            if days_since_maintenance >= interval:
                task = MaintenanceTask(
                    task_id=f"routine_{equipment.equipment_id}_{uuid.uuid4().hex[:8]}",
                    equipment_id=equipment.equipment_id,
                    equipment_type=equipment.equipment_type,
                    property_id=equipment.property_id,
                    priority=MaintenancePriority.MEDIUM,
                    description=f"Routine maintenance for {equipment.brand} {equipment.model}",
                    estimated_cost=self.service._get_base_repair_cost(equipment.equipment_type) * 0.5,
                    estimated_duration=2,
                    failure_probability=0.1,
                    recommended_date=datetime.utcnow() + timedelta(days=7),
                    vendor_recommendation=self.service._recommend_vendor(equipment.equipment_type),
                    required_skills=self.service._get_required_skills(equipment.equipment_type)
                )
                tasks.append(task)

        return tasks

    def _group_tasks_by_week(self, schedule: List[Dict]) -> Dict[int, List[Dict]]:
        """Group tasks by week number."""
        weekly_tasks = {}

        for task in schedule:
            week_num = (task["scheduled_date"] - datetime.utcnow()).days // 7
            if week_num not in weekly_tasks:
                weekly_tasks[week_num] = []
            weekly_tasks[week_num].append(task)

        return weekly_tasks

    async def _generate_recommendations(self,
                                      tasks: List[MaintenanceTask],
                                      equipment_list: List[Equipment]) -> List[str]:
        """Generate maintenance recommendations."""
        recommendations = []

        # Analyze failure patterns
        failure_counts = {}
        for task in tasks:
            if task.failure_probability > 0.5:
                failure_counts[task.equipment_type] = failure_counts.get(
                    task.equipment_type, 0
                ) + 1

        # Generate recommendations
        for equipment_type, count in failure_counts.items():
            if count >= 3:
                recommendations.append(
                    f"Consider upgrading {count} {equipment_type.value} units "
                    "with frequent failure predictions"
                )

        # Budget recommendations
        total_cost = sum(t.estimated_cost for t in tasks)
        if total_cost > 10000:
            recommendations.append(
                "High maintenance costs detected. Consider preventive maintenance "
                "contract to reduce expenses"
            )

        # Age-based recommendations
        old_equipment = [
            e for e in equipment_list
            if (datetime.utcnow() - e.install_date).days / 365 > e.expected_lifespan * 0.8
        ]

        if old_equipment:
            recommendations.append(
                f"{len(old_equipment)} equipment units are nearing end of life. "
                "Plan for replacement in next 1-2 years"
            )

        return recommendations


# Global service instances
predictive_maintenance_service = PredictiveMaintenanceService()
maintenance_scheduler = MaintenanceScheduler()