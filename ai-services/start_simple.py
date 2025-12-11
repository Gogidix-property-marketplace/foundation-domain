#!/usr/bin/env python3
"""
Simple AI Services Starter
Starts all services in a single Python process for easy testing
"""

import uvicorn
import sys
import os
import asyncio
import threading
import time
from pathlib import Path

# Add project root to Python path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

def start_service(port, module_name, service_name):
    """Start a single FastAPI service."""
    print(f"🚀 Starting {service_name} on port {port}...")

    # Set environment
    os.environ["PYTHONPATH"] = str(project_root)

    # Import the app
    app = __import__(module_name, fromlist=['app']).app

    # Run the service
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")

def main():
    """Start all services."""
    print("=" * 60)
    print("🚀 Starting Gogidix AI Services (Simple Mode)")
    print("=" * 60)

    services = [
        (8000, "src.gogidix_ai.gateway.main", "AI Gateway"),
        (8001, "src.gogidix_ai.property_intelligence.main", "Property Intelligence"),
        (8002, "src.gogidix_ai.conversational_ai.main", "Conversational AI"),
        (8003, "src.gogidix_ai.analytics.main", "Analytics"),
    ]

    # Create threads for each service
    threads = []

    for port, module, name in services:
        thread = threading.Thread(
            target=start_service,
            args=(port, module, name),
            daemon=True
        )
        threads.append(thread)
        thread.start()
        time.sleep(2)  # Stagger starts

    print("\n" + "=" * 60)
    print("✅ All services starting...")
    print("=" * 60)
    print("\n📊 Service URLs:")
    print("  • AI Gateway:         http://localhost:8000")
    print("  • Property Intel:     http://localhost:8001")
    print("  • Conversational AI:  http://localhost:8002")
    print("  • Analytics:          http://localhost:8003")
    print("\n📝 API Documentation:")
    print("  • Swagger UI:         http://localhost:8000/docs")
    print("  • ReDoc:              http://localhost:8000/redoc")
    print("\n🔍 Health Checks:")
    print("  • http://localhost:8000/health")
    print("  • http://localhost:8001/health")
    print("  • http://localhost:8002/health")
    print("  • http://localhost:8003/health")
    print("\n🛑 Press Ctrl+C to stop all services")
    print("=" * 60)

    try:
        # Keep main thread alive
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n🛑 Shutting down services...")
        sys.exit(0)

if __name__ == "__main__":
    main()