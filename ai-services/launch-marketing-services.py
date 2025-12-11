#!/usr/bin/env python3
"""
Launch Marketing AI Services
Comprehensive script to start all marketing and sales AI services

Services to Launch:
- Customer Segmentation Service (Port 9058)
- A/B Testing Service (Port 9059)
- Revenue Analytics Service (Port 9001)
- Lead Generation Service (Port 9055)
- Marketing Automation Service (Port 9056)
"""

import subprocess
import sys
import time
import os
import signal
from datetime import datetime

class ServiceLauncher:
    def __init__(self):
        self.services = {
            "customer-segmentation": {
                "port": 9058,
                "path": "backend-services/python-services/customer-segmentation-service",
                "main": "main.py",
                "description": "AI Customer Segmentation with Clustering Algorithms"
            },
            "ab-testing": {
                "port": 9059,
                "path": "backend-services/python-services/ab-testing-service",
                "main": "main.py",
                "description": "Statistical A/B Testing with Significance Analysis"
            },
            "revenue-analytics": {
                "port": 9001,
                "path": "backend-services/python-services/revenue-analytics-service",
                "main": "main.py",
                "description": "Revenue Forecasting and Analytics"
            },
            "lead-generation": {
                "port": 9055,
                "path": "backend-services/python-services/lead-generation-service",
                "main": "main.py",
                "description": "AI-Powered Lead Generation and Scoring"
            },
            "marketing-automation": {
                "port": 9056,
                "path": "backend-services/python-services/marketing-automation-service",
                "main": "main.py",
                "description": "Advanced Marketing Automation Platform"
            }
        }
        self.processes = []
        self.base_dir = os.path.dirname(os.path.abspath(__file__))

    def check_dependencies(self):
        """Check if required dependencies are installed"""
        print("🔍 Checking dependencies...")

        try:
            import fastapi
            print("✅ FastAPI installed")
        except ImportError:
            print("❌ FastAPI not installed. Run: pip install fastapi")
            return False

        try:
            import uvicorn
            print("✅ Uvicorn installed")
        except ImportError:
            print("❌ Uvicorn not installed. Run: pip install uvicorn")
            return False

        try:
            import pandas
            print("✅ Pandas installed")
        except ImportError:
            print("❌ Pandas not installed. Run: pip install pandas")
            return False

        try:
            import numpy
            print("✅ NumPy installed")
        except ImportError:
            print("❌ NumPy not installed. Run: pip install numpy")
            return False

        try:
            import scipy
            print("✅ SciPy installed")
        except ImportError:
            print("❌ SciPy not installed. Run: pip install scipy")
            return False

        return True

    def check_service_files(self):
        """Check if service files exist"""
        print("📁 Checking service files...")

        all_exist = True
        for service_name, config in self.services.items():
            service_path = os.path.join(self.base_dir, config["path"])
            main_file = os.path.join(service_path, config["main"])

            if os.path.exists(main_file):
                print(f"✅ {service_name} - {main_file}")
            else:
                print(f"❌ {service_name} - Missing: {main_file}")
                all_exist = False

        return all_exist

    def check_port_available(self, port):
        """Check if port is available"""
        import socket

        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            try:
                s.bind(('localhost', port))
                return True
            except:
                return False

    def check_ports(self):
        """Check if all ports are available"""
        print("🔌 Checking port availability...")

        all_available = True
        for service_name, config in self.services.items():
            if self.check_port_available(config["port"]):
                print(f"✅ Port {config['port']} - {service_name}")
            else:
                print(f"❌ Port {config['port']} - {service_name} (Already in use)")
                all_available = False

        return all_available

    def launch_service(self, service_name, config):
        """Launch individual service"""
        service_path = os.path.join(self.base_dir, config["path"])
        main_file = os.path.join(service_path, config["main"])

        print(f"\n🚀 Launching {service_name} on port {config['port']}")
        print(f"   📁 Path: {service_path}")
        print(f"   📄 Main: {main_file}")
        print(f"   📝 Description: {config['description']}")

        # Change to service directory
        os.chdir(service_path)

        # Launch service
        try:
            process = subprocess.Popen(
                [sys.executable, config["main"]],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                bufsize=1,
                universal_newlines=True
            )

            return process
        except Exception as e:
            print(f"❌ Failed to launch {service_name}: {str(e)}")
            return None

    def launch_all_services(self):
        """Launch all marketing services"""
        print("\n" + "="*80)
        print("🎯 LAUNCHING MARKETING AI SERVICES")
        print("="*80)
        print(f"⏰ Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"🌟 Total Services: {len(self.services)}")
        print("-"*80)

        # Check dependencies
        if not self.check_dependencies():
            print("\n❌ Missing dependencies. Please install them first.")
            return False

        # Check service files
        if not self.check_service_files():
            print("\n❌ Missing service files. Please create them first.")
            return False

        # Check ports
        if not self.check_ports():
            print("\n⚠️  Some ports are in use. Services may fail to start.")

        print("\n🚀 Starting services...")

        # Launch each service
        for service_name, config in self.services.items():
            process = self.launch_service(service_name, config)
            if process:
                self.processes.append({
                    "name": service_name,
                    "process": process,
                    "port": config["port"],
                    "config": config
                })
                time.sleep(2)  # Give service time to start

        # Wait a moment and check status
        time.sleep(5)
        self.check_services_status()

        return True

    def check_services_status(self):
        """Check status of all running services"""
        print("\n" + "="*80)
        print("📊 SERVICES STATUS")
        print("="*80)

        running_count = 0

        for service_info in self.processes:
            service_name = service_info["name"]
            process = service_info["process"]
            port = service_info["port"]

            if process.poll() is None:  # Process is running
                running_count += 1
                status = "🟢 RUNNING"

                # Check if port is actually listening
                if self.check_port_available(port):
                    status = "🟡 STARTING"
            else:
                status = "🔴 STOPPED"

            print(f"{status:<12} {service_name:<25} Port: {port}")

        print("-"*80)
        print(f"📈 Summary: {running_count}/{len(self.processes)} services running")

        if running_count == len(self.processes):
            print("✅ All marketing services launched successfully!")
        else:
            print("⚠️  Some services failed to start. Check logs for details.")

    def show_service_info(self):
        """Show information about all services"""
        print("\n" + "="*80)
        print("📚 MARKETING AI SERVICES INFORMATION")
        print("="*80)

        for service_name, config in self.services.items():
            print(f"\n🔸 {service_name.upper()}")
            print(f"   📍 Port: {config['port']}")
            print(f"   📄 Description: {config['description']}")
            print(f"   🔗 Endpoint: http://localhost:{config['port']}")
            print(f"   📖 Docs: http://localhost:{config['port']}/docs")
            print(f"   ❤️  Health: http://localhost:{config['port']}/health")

        print("\n" + "="*80)
        print("🌐 WEB DASHBOARD")
        print("="*80)
        print("🖥️  AI Dashboard: http://localhost:3000")
        print("🔗 API Gateway: http://localhost:3002")
        print("🏢 Management Bridge: http://localhost:3003")
        print("="*80)

    def show_api_examples(self):
        """Show API usage examples"""
        print("\n" + "="*80)
        print("💡 API USAGE EXAMPLES")
        print("="*80)

        examples = [
            {
                "service": "Customer Segmentation",
                "port": 9058,
                "command": "curl -X POST http://localhost:9058/api/v1/segmentation",
                "description": "Create customer segments with AI clustering"
            },
            {
                "service": "A/B Testing",
                "port": 9059,
                "command": "curl -X POST http://localhost:9059/api/v1/tests",
                "description": "Create statistical A/B tests"
            },
            {
                "service": "Revenue Analytics",
                "port": 9001,
                "command": "curl -X POST http://localhost:9001/api/v1/forecast",
                "description": "Generate revenue forecasts"
            },
            {
                "service": "Lead Generation",
                "port": 9055,
                "command": "curl -X POST http://localhost:9055/api/v1/leads/generate",
                "description": "Generate AI-powered leads"
            },
            {
                "service": "Marketing Automation",
                "port": 9056,
                "command": "curl -X POST http://localhost:9056/api/v1/campaigns/create",
                "description": "Create automated marketing campaigns"
            }
        ]

        for example in examples:
            print(f"\n🔸 {example['service']}")
            print(f"   📝 {example['description']}")
            print(f"   💻 {example['command']}")

    def stop_services(self):
        """Stop all running services"""
        print("\n⏹️  Stopping all services...")

        for service_info in self.processes:
            process = service_info["process"]
            service_name = service_info["name"]

            if process.poll() is None:  # Process is running
                print(f"   🛑 Stopping {service_name}...")
                process.terminate()

                # Wait for graceful shutdown
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    print(f"   💥 Force killing {service_name}...")
                    process.kill()

        print("✅ All services stopped")
        self.processes = []

    def run_interactive(self):
        """Run in interactive mode"""
        while True:
            print("\n" + "="*50)
            print("🎯 MARKETING AI SERVICES CONTROL")
            print("="*50)
            print("1. 🚀 Launch all services")
            print("2. 📊 Check service status")
            print("3. 📚 Show service information")
            print("4. 💡 Show API examples")
            print("5. 🛑 Stop all services")
            print("6. 🚪 Exit")
            print("="*50)

            choice = input("\nSelect option (1-6): ").strip()

            if choice == "1":
                self.launch_all_services()
            elif choice == "2":
                self.check_services_status()
            elif choice == "3":
                self.show_service_info()
            elif choice == "4":
                self.show_api_examples()
            elif choice == "5":
                self.stop_services()
            elif choice == "6":
                self.stop_services()
                print("👋 Goodbye!")
                break
            else:
                print("❌ Invalid option. Please try again.")

    def signal_handler(self, signum, frame):
        """Handle Ctrl+C gracefully"""
        print("\n\n⚠️  Interrupt received. Stopping services...")
        self.stop_services()
        sys.exit(0)

def main():
    launcher = ServiceLauncher()

    # Set up signal handler for graceful shutdown
    signal.signal(signal.SIGINT, launcher.signal_handler)
    signal.signal(signal.SIGTERM, launcher.signal_handler)

    if len(sys.argv) > 1:
        if sys.argv[1] == "launch":
            launcher.launch_all_services()
        elif sys.argv[1] == "stop":
            launcher.stop_services()
        elif sys.argv[1] == "status":
            launcher.check_services_status()
        elif sys.argv[1] == "info":
            launcher.show_service_info()
        else:
            print("Usage: python launch-marketing-services.py [launch|stop|status|info]")
    else:
        launcher.run_interactive()

if __name__ == "__main__":
    main()