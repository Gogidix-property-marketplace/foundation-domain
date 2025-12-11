"""
API v1 Endpoint Modules

Individual endpoint modules for different API functionalities.
"""

# Import all endpoint modules to ensure they're registered
from . import inference
from . import models
from . import monitoring
from . import admin

__all__ = [
    "inference",
    "models",
    "monitoring",
    "admin",
]