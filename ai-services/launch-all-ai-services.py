#!/usr/bin/env python3
"""
Launch All AI Services Script
Comprehensive launcher for all 17 implemented AI services

Services (17 of 34 complete):
- Core AI Services (7): Predictive (9000), Recommendation (9010), Image (9020), Text (9030), Fraud (9040), Chatbot (9035), Content Gen (9037)
- Marketing Suite (5): Revenue Analytics (9001), Lead Gen (9055), Marketing Auto (9056), Customer Seg (9058), A/B Testing (9059)
- Business Services (5): BI Dashboard (9070), Workflow Engine (9080), Financial Analysis (9090), plus 2 more
- Infrastructure (3): AI Dashboard (3000), API Gateway (3002), Management Bridge (3003)
"""

import subprocess
import sys
import time
import os
import signal
from datetime import datetime
import platform

class AIServiceLauncher:
    def __init__(self):
        # All implemented AI services
        self.services = {
            # Infrastructure Services
            "ai-dashboard": {
                "port": 3000,
                "type": "nodejs",
                "path": "backend-services/nodejs-services/ai-dashboard-web",
                "command": "npm start",
                "description": "AI Services Dashboard - Web Interface"
            },
            "api-gateway": {
                "port": 3002,
                "type": "nodejs",
                "path": "backend-services/nodejs-services/unified-ai-gateway",
                "command": "npm start",
                "description": "Unified API Gateway - Service Orchestration"
            },
            "management-bridge": {
                "port": 3003,
                "type": "nodejs",
                "path": "backend-services/nodejs-services/management-domain-integration",
                "command": "npm start",
                "description": "Management Domain Bridge - Java Spring Boot Integration"
            },

            # Core AI Services
            "predictive-analytics": {
                "port": 9000,
                "type": "python",
                "path": "backend-services/python-services/predictive-analytics-service",
                "command": "python main.py",
                "description": "Predictive Analytics - Time Series Forecasting"
            },
            "revenue-analytics": {
                "port": 9001,
                "type": "python",
                "path": "backend-services/python-services/revenue-analytics-service",
                "command": "python main.py",
                "description": "Revenue Analytics - Financial Forecasting"
            },
            "recommendation": {
                "port": 9010,
                "type": "python",
                "path": "backend-services/python-services/recommendation-service",
                "command": "python main.py",
                "description": "Recommendation Engine - Collaborative Filtering"
            },
            "image-analysis": {
                "port": 9020,
                "type": "python",
                "path": "backend-services/python-services/image-analysis-service",
                "command": "python main.py",
                "description": "Image Analysis - Computer Vision"
            },
            "text-analysis": {
                "port": 9030,
                "type": "python",
                "path": "backend-services/python-services/text-analysis-service",
                "command": "python main.py",
                "description": "Text Analysis - NLP & Sentiment"
            },
            "chatbot": {
                "port": 9035,
                "type": "python",
                "path": "backend-services/python-services/chatbot-service",
                "command": "python main.py",
                "description": "AI Chatbot - Conversational Assistant"
            },
            "fraud-detection": {
                "port": 9040,
                "type": "python",
                "path": "backend-services/python-services/fraud-detection-service",
                "command": "python main.py",
                "description": "Fraud Detection - Real-time Scoring"
            },
            "lead-generation": {
                "port": 9055,
                "type": "python",
                "path": "backend-services/python-services/lead-generation-service",
                "command": "python main.py",
                "description": "Lead Generation - AI-powered Lead Gen"
            },
            "marketing-automation": {
                "port": 9056,
                "type": "python",
                "path": "backend-services/python-services/marketing-automation-service",
                "command": "python main.py",
                "description": "Marketing Automation - Campaign Management"
            },
            "customer-segmentation": {
                "port": 9058,
                "type": "python",
                "path": "backend-services/python-services/customer-segmentation-service",
                "command": "python main.py",
                "description": "Customer Segmentation - AI Clustering"
            },
            "ab-testing": {
                "port": 9059,
                "type": "python",
                "path": "backend-services/python-services/ab-testing-service",
                "command": "python main.py",
                "description": "A/B Testing - Statistical Analysis"
            },
            "bi-dashboard": {
                "port": 9070,
                "type": "python",
                "path": "backend-services/python-services/bi-dashboard-service",
                "command": "python main.py",
                "description": "BI Dashboard - Business Intelligence"
            },
            "workflow-engine": {
                "port": 9080,
                "type": "python",
                "path": "backend-services/python-services/workflow-engine-service",
                "command": "python main.py",
                "description": "Workflow Engine - Process Automation"
            },
            "financial-analysis": {
                "port": 9090,
                "type": "python",
                "path": "backend-services/python-services/financial-analysis-service",
                "command": "python main.py",
                "description": "Financial Analysis - Investment Analytics"
            }
        }

        self.processes = {}
        self.base_dir = os.path.dirname(os.path.abspath(__file__))

    def check_port_available(self, port):
        """Check if port is available"""
        import socket
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            try:
                s.bind(('localhost', port))
                return True
            except:
                return False

    def check_dependencies(self):
        """Check dependencies"""
        print("[CHECK] Checking system dependencies...")

        # Check Node.js for Node services
        node_services = [s for s in self.services.values() if s["type"] == "nodejs"]
        if node_services:
            try:
                result = subprocess.run(['node', '--version'], capture_output=True, text=True)
                if result.returncode == 0:
                    print(f"[OK] Node.js {result.stdout.strip()}")
                else:
                    print("[ERROR] Node.js not found")
                    return False
            except FileNotFoundError:
                print("[ERROR] Node.js not found")
                return False

        # Check Python for Python services
        python_services = [s for s in self.services.values() if s["type"] == "python"]
        if python_services:
            try:
                result = subprocess.run(['python', '--version'], capture_output=True, text=True)
                if result.returncode == 0:
                    print(f"[OK] Python {result.stdout.strip()}")
                else:
                    print("[ERROR] Python not found")
                    return False
            except FileNotFoundError:
                print("[ERROR] Python not found")
                return False

        # Check Python packages
        try:
            import fastapi
            print("[OK] FastAPI installed")
        except ImportError:
            print("[WARNING] FastAPI not installed - Python services may fail")

        return True

    def launch_service(self, service_name, config):
        """Launch individual service"""
        service_path = os.path.join(self.base_dir, config["path"])

        if not os.path.exists(service_path):
            print(f"[ERROR] Service path not found: {service_path}")
            return None

        print(f"\n[LAUNCH] Starting {service_name}")
        print(f"       Port: {config['port']}")
        print(f"       Type: {config['type']}")
        print(f"       Description: {config['description']}")

        # Change to service directory
        original_dir = os.getcwd()
        os.chdir(service_path)

        try:
            if config["type"] == "nodejs":
                # Install dependencies if needed
                if not os.path.exists("node_modules"):
                    print("       Installing Node.js dependencies...")
                    subprocess.run(['npm', 'install'], capture_output=True)

                # Launch Node.js service
                if "start" in config["command"]:
                    process = subprocess.Popen(
                        config["command"].split(),
                        stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE,
                        text=True
                    )
                else:
                    process = subprocess.Popen(
                        ['node', config["command"]],
                        stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE,
                        text=True
                    )

            else:  # Python service
                # Launch Python service
                process = subprocess.Popen(
                    config["command"].split(),
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True
                )

            # Return to original directory
            os.chdir(original_dir)

            return {
                "name": service_name,
                "process": process,
                "port": config["port"],
                "config": config,
                "started_at": datetime.now()
            }

        except Exception as e:
            print(f"[ERROR] Failed to launch {service_name}: {str(e)}")
            os.chdir(original_dir)
            return None

    def launch_all_services(self, service_type="all"):
        """Launch all services or specific type"""
        print("\n" + "="*100)
        print("🚀 LAUNCHING AI SERVICES PLATFORM")
        print("="*100)
        print(f"⏰ Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"🌟 Total Services: {len(self.services)}")
        print(f"📊 Progress: 17 of 34 services implemented (50% complete)")
        print("-"*100)

        # Check dependencies
        if not self.check_dependencies():
            print("\n[ERROR] Missing dependencies. Please install them first.")
            return False

        # Filter services by type if specified
        services_to_launch = self.services
        if service_type != "all":
            services_to_launch = {
                k: v for k, v in self.services.items()
                if v["type"] == service_type
            }

        # Check ports
        print("\n[CHECK] Checking port availability...")
        ports_ok = True
        for service_name, config in services_to_launch.items():
            if self.check_port_available(config["port"]):
                print(f"[OK] Port {config['port']} - {service_name}")
            else:
                print(f"[WARNING] Port {config['port']} - {service_name} (Already in use)")
                ports_ok = False

        if not ports_ok:
            print("\n[WARNING] Some ports are in use. Services may fail to start.")

        # Launch services
        print(f"\n[LAUNCH] Starting {len(services_to_launch)} services...")
        launched = 0

        for service_name, config in services_to_launch.items():
            service_info = self.launch_service(service_name, config)
            if service_info:
                self.processes[service_name] = service_info
                launched += 1
                time.sleep(2)  # Give service time to start

        # Wait and check status
        time.sleep(5)
        self.check_services_status()

        print(f"\n[SUCCESS] Launched {launched}/{len(services_to_launch)} services")
        return launched > 0

    def check_services_status(self):
        """Check status of all running services"""
        print("\n" + "="*100)
        print("📊 SERVICES STATUS")
        print("="*100)

        running = 0
        failed = 0
        starting = 0

        for service_name, service_info in self.processes.items():
            process = service_info["process"]
            port = service_info["port"]

            if process.poll() is None:  # Process is running
                if self.check_port_available(port):
                    status = "🟡 STARTING"
                    starting += 1
                else:
                    status = "🟢 RUNNING"
                    running += 1
            else:
                status = "🔴 FAILED"
                failed += 1

            runtime = datetime.now() - service_info["started_at"]
            print(f"{status:<12} {service_name:<25} Port: {port:<6} Runtime: {runtime}")

        print("-"*100)
        print(f"📈 Summary: {running} running, {starting} starting, {failed} failed")

        if running > 0:
            print("\n✅ AI Services Platform is running!")
            print("\n🌐 Access Points:")
            print(f"   AI Dashboard: http://localhost:3000")
            print(f"   API Gateway: http://localhost:3002")
            print(f"   Management Bridge: http://localhost:3003")
            print(f"\n📚 API Documentation:")
            print("   Add '/docs' to any service URL for Swagger documentation")
            print("   Example: http://localhost:9000/docs")

    def stop_all_services(self):
        """Stop all running services"""
        print("\n⏹️  Stopping all services...")

        for service_name, service_info in self.processes.items():
            process = service_info["process"]

            if process.poll() is None:  # Process is running
                print(f"   🛑 Stopping {service_name}...")
                process.terminate()

                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    print(f"   💥 Force killing {service_name}...")
                    process.kill()

        print("✅ All services stopped")
        self.processes = {}

    def show_service_info(self):
        """Show information about all services"""
        print("\n" + "="*100)
        print("📚 AI SERVICES CATALOG")
        print("="*100)

        categories = {
            "Infrastructure": [],
            "Core AI": [],
            "Marketing": [],
            "Business": []
        }

        # Categorize services
        for name, config in self.services.items():
            if name in ["ai-dashboard", "api-gateway", "management-bridge"]:
                categories["Infrastructure"].append((name, config))
            elif name in ["predictive-analytics", "revenue-analytics", "recommendation",
                        "image-analysis", "text-analysis", "chatbot", "fraud-detection"]:
                categories["Core AI"].append((name, config))
            elif name in ["lead-generation", "marketing-automation", "customer-segmentation", "ab-testing"]:
                categories["Marketing"].append((name, config))
            else:
                categories["Business"].append((name, config))

        for category, services in categories.items():
            print(f"\n🔸 {category} Services:")
            for name, config in services:
                print(f"   • {name:<25} Port: {config['port']:<6} - {config['description']}")

        print(f"\n📊 Implementation Progress: 17/34 services (50% complete)")
        print(f"🚀 Remaining: 17 services to implement")

    def generate_api_list(self):
        """Generate list of all API endpoints"""
        print("\n" + "="*100)
        print("🔗 API ENDPOINTS")
        print("="*100)

        for service_name, config in self.services.items():
            print(f"\n🔸 {service_name.title()}:")
            print(f"   Base URL: http://localhost:{config['port']}")
            print(f"   Health: http://localhost:{config['port']}/health")
            print(f"   Metrics: http://localhost:{config['port']}/metrics")
            print(f"   API Docs: http://localhost:{config['port']}/docs")

    def run_interactive(self):
        """Run in interactive mode"""
        while True:
            print("\n" + "="*60)
            print("🎯 AI SERVICES CONTROL PANEL")
            print("="*60)
            print("1. 🚀 Launch all services")
            print("2. 📊 Check service status")
            print("3. 📚 Show service catalog")
            print("4. 🔗 List API endpoints")
            print("5. ⚡ Launch Python services only")
            print("6. ⚡ Launch Node.js services only")
            print("7. 🛑 Stop all services")
            print("8. 🚪 Exit")
            print("="*60)

            choice = input("\nSelect option (1-8): ").strip()

            if choice == "1":
                self.launch_all_services()
            elif choice == "2":
                self.check_services_status()
            elif choice == "3":
                self.show_service_info()
            elif choice == "4":
                self.generate_api_list()
            elif choice == "5":
                self.launch_all_services("python")
            elif choice == "6":
                self.launch_all_services("nodejs")
            elif choice == "7":
                self.stop_all_services()
            elif choice == "8":
                self.stop_all_services()
                print("\n👋 Thank you for using AI Services Platform!")
                break
            else:
                print("[ERROR] Invalid option. Please try again.")

    def signal_handler(self, signum, frame):
        """Handle Ctrl+C gracefully"""
        print("\n\n⚠️  Interrupt received. Stopping services...")
        self.stop_all_services()
        sys.exit(0)

def main():
    launcher = AIServiceLauncher()

    # Set up signal handler for graceful shutdown
    signal.signal(signal.SIGINT, launcher.signal_handler)
    signal.signal(signal.SIGTERM, launcher.signal_handler)

    if len(sys.argv) > 1:
        if sys.argv[1] == "launch":
            launcher.launch_all_services()
        elif sys.argv[1] == "stop":
            launcher.stop_all_services()
        elif sys.argv[1] == "status":
            launcher.check_services_status()
        elif sys.argv[1] == "info":
            launcher.show_service_info()
        elif sys.argv[1] == "apis":
            launcher.generate_api_list()
        elif sys.argv[1] == "python":
            launcher.launch_all_services("python")
        elif sys.argv[1] == "nodejs":
            launcher.launch_all_services("nodejs")
        else:
            print("Usage: python launch-all-ai-services.py [launch|stop|status|info|apis|python|nodejs]")
    else:
        launcher.run_interactive()

if __name__ == "__main__":
    main()