# Native Python Setup Guide (No Docker)

This guide helps you run Gogidix AI Services using native Python only - no Docker required! This is much lighter and faster for development.

## 🚀 Quick Start (3 Commands)

```powershell
# 1. Open PowerShell as Administrator
cd C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\ai-services

# 2. Run setup (5 minutes)
.\scripts\setup_native_env.ps1

# 3. Start services
.\start_native.bat
```

## 📋 What's Different vs Docker Version

| Feature | Docker Version | Native Version |
|---------|----------------|----------------|
| **Speed** | 2-3 min startup | 10-15 sec startup |
| **RAM Usage** | 4-8GB | 500MB-1GB |
| **Setup** | Complex (Docker) | Simple (Python only) |
| **Database** | PostgreSQL | SQLite |
| **Cache** | Redis | In-memory |
| **Message Queue** | Kafka | None (not needed for basic) |
| **Search** | Elasticsearch | Simple text search |
| **Monitoring** | Grafana/Prometheus | Basic logging |

## ✅ What You Get (Native Version)

### Core Services (All Working):
- ✅ **AI Gateway** - Main API (port 8000)
- ✅ **Property Intelligence** - Property analysis (port 8001)
- ✅ **Conversational AI** - Chatbot (port 8002)
- ✅ **Analytics** - Basic analytics (port 8003)

### Features:
- ✅ Property valuation AI
- ✅ Multi-language chatbot (6 languages)
- ✅ Image analysis
- ✅ Recommendation engine
- ✅ SQLite database
- ✅ RESTful APIs
- ✅ Interactive documentation

### What's Not Included:
- ❌ Kafka (message queue)
- ❌ Elasticsearch (advanced search)
- ❌ Grafana (dashboard)
- ❌ Prometheus (metrics)
- ❌ MLflow (ML tracking)

## 🛠 Setup Steps

### Prerequisites
- Python 3.9+ installed
- PowerShell (comes with Windows)

### Step 1: Run Setup Script
```powershell
.\scripts\setup_native_env.ps1
```

This will:
- Create Python virtual environment
- Install all dependencies
- Set up SQLite database
- Create startup scripts

### Step 2: Generate Test Data
```powershell
.\generate_data.bat
```

### Step 3: Train ML Models (Optional)
```powershell
.\train_native.bat
```

### Step 4: Start Services
```powershell
.\start_native.bat
```

Each service will open in its own window.

## 📊 Accessing Services

Once running, access these URLs:

| Service | URL | What it does |
|---------|-----|-------------|
| **AI Gateway** | http://localhost:8000 | Main API endpoint |
| **API Docs** | http://localhost:8000/docs | Interactive API testing |
| **Property Intel** | http://localhost:8001 | Property analysis |
| **Chat API** | http://localhost:8002 | AI chatbot |
| **Analytics** | http://localhost:8003 | Usage analytics |

## 🧪 Testing the APIs

### Method 1: Interactive Docs
1. Go to http://localhost:8000/docs
2. Click any endpoint (e.g., POST /api/v1/property-valuation)
3. Click "Try it out"
4. Fill in values and "Execute"

### Method 2: PowerShell
```powershell
# Property valuation
Invoke-RestMethod -Uri "http://localhost:8000/api/v1/property-valuation" `
  -Method POST `
  -ContentType "application/json" `
  -Body @{
    property_type="apartment"
    bedrooms=2
    bathrooms=2
    square_feet=1200
    city="New York"
  } | ConvertTo-Json

# Chat with AI
Invoke-RestMethod -Uri "http://localhost:8000/api/v1/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body @{
    message="Find me a 2-bedroom apartment in NYC"
    conversation_id="test-123"
  } | ConvertTo-Json
```

## 🗂 File Structure After Setup

```
ai-services/
├── venv/                    # Python virtual environment
├── data/                    # SQLite database & generated data
│   ├── gogidix_ai.db       # SQLite database
│   └── properties_*.parquet # Test data
├── models/                  # Trained ML models
├── logs/                    # Application logs
├── uploads/                 # Uploaded files
├── .env.native             # Configuration file
├── start_native.bat        # Start all services
├── stop_native.bat         # Stop all services
├── test_native.bat         # Test APIs
├── generate_data.bat       # Generate test data
├── train_native.bat        # Train ML models
└── monitor_services.bat    # Service monitor
```

## 📝 Working with the Services

### View Logs
Each service runs in its own window showing logs.

### Monitor Services
```powershell
.\monitor_services.bat
```
This shows which services are running and memory usage.

### Restart Services
```powershell
# Stop all services
.\stop_native.bat

# Start again
.\start_native.bat
```

### Debug a Specific Service
Open its window and:
- Check error messages
- The service auto-reloads on code changes

## 🔧 Configuration

Edit `.env.native` to change settings:
- `DATABASE_URL`: Database connection
- `CACHE_TTL`: Cache duration (seconds)
- `RATE_LIMIT_REQUESTS`: API rate limit
- `DEVICE`: Use "cuda" if you have NVIDIA GPU

## 🚀 Development Workflow

### 1. Make Code Changes
Edit any Python files in `src/`

### 2. Services Auto-Reload
Services automatically restart when you save changes!

### 3. Test Your Changes
```powershell
.\test_native.bat
```

### 4. View Database
SQLite database file: `data/gogidix_ai.db`
Use SQLite browser to view data.

## 📈 Performance

### Expected Performance:
- **Startup Time**: 10-15 seconds
- **Memory Usage**: ~500MB all services
- **API Response**: <100ms
- **CPU Usage**: Minimal when idle

### Optimize Further:
- Set `DEVICE=cuda` if you have GPU
- Increase `MAX_WORKERS` in config
- Use smaller batch sizes

## 🐛 Troubleshooting

### Port Already in Use
```powershell
# Find what's using port 8000
netstat -ano | findstr :8000

# Kill the process
taskkill /PID <PID> /F
```

### Python Module Not Found
```powershell
# Activate virtual environment
.\venv\Scripts\Activate.ps1

# Install missing module
pip install <module-name>
```

### Database Error
```powershell
# Reinitialize database
python scripts\init_db_native.py
```

### Service Won't Start
1. Check the service window for errors
2. Ensure virtual environment is activated
3. Check if port is available

## 🔄 Switching Between Docker and Native

### From Native to Docker:
```powershell
# Stop native services
.\stop_native.bat

# Start Docker services
.\start_local.bat
```

### From Docker to Native:
```powershell
# Stop Docker services
.\stop_local.bat

# Start native services
.\start_native.bat
```

## 📚 Next Steps

1. **Explore APIs**: Visit http://localhost:8000/docs
2. **Add Features**: Edit services in `src/`
3. **Train Models**: Run `.\train_native.bat`
4. **View Data**: Check SQLite database
5. **Monitor Performance**: Use `.\monitor_services.bat`

## 💡 Tips

- **Development**: Use native mode for faster iteration
- **Testing**: Perfect for API testing without overhead
- **Learning**: Great for understanding the architecture
- **Production**: Use Docker for production deployment

## ❓ Questions?

Need help? Check the service windows for error messages or create an issue on GitHub.

Happy coding! 🚀