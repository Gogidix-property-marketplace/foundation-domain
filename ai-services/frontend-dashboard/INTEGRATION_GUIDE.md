# 🔌 Frontend Dashboard Integration Guide

**Complete guide for integrating the frontend dashboard with the AI services backend**

---

## 📋 **OVERVIEW**

This guide covers the complete integration of the Next.js frontend dashboard with the Gogidix AI Services backend. The integration includes real-time data fetching, WebSocket connections, and comprehensive API communication.

### **Integration Architecture**

```
Frontend Dashboard (Next.js) → API Gateway (Port 3002) → Individual AI Services
                                      ↓
                                WebSocket Server → Real-time Updates
```

---

## 🚀 **INTEGRATION PREREQUISITES**

### **Backend Services Status**

Before integrating, ensure the following backend services are running:

- **API Gateway**: Port 3002 (primary integration point)
- **Individual AI Services**: Ports 9000-9110
- **WebSocket Server**: ws://localhost:3002/ws
- **Service Registry**: Available at `/services` endpoint

### **Environment Configuration**

Create `.env.local` with the following configuration:

```bash
# API Gateway URL (Primary backend integration point)
NEXT_PUBLIC_API_GATEWAY_URL=http://localhost:3002

# WebSocket URL for real-time updates
NEXT_PUBLIC_WS_URL=ws://localhost:3002/ws

# Environment
NEXT_PUBLIC_ENVIRONMENT=development

# Feature flags for backend integration
NEXT_PUBLIC_ENABLE_WEBSOCKETS=true
NEXT_PUBLIC_ENABLE_SERVICE_MANAGEMENT=true
NEXT_PUBLIC_ENABLE_REAL_TIME_METRICS=true

# API timeouts and retries
NEXT_PUBLIC_API_TIMEOUT=10000
NEXT_PUBLIC_WS_RECONNECT_INTERVAL=5000
NEXT_PUBLIC_MAX_RETRIES=3
```

---

## 🔌 **API INTEGRATION**

### **1. API Client Configuration**

The frontend uses a centralized API client (`src/lib/api.ts`) configured with:

- **Base URL**: API Gateway endpoint
- **Authentication**: JWT token support
- **Error Handling**: Automatic retry logic with exponential backoff
- **Timeouts**: Configurable request timeouts

### **2. Core API Endpoints**

#### **System Endpoints**
```typescript
// System health check
GET /health

// Service registry
GET /services

// System metrics
GET /metrics

// System statistics
GET /stats
```

#### **Service Management Endpoints**
```typescript
// Individual service health
GET /services/{serviceId}/health

// Individual service metrics
GET /services/{serviceId}/metrics

// Individual service stats
GET /services/{serviceId}/stats

// Restart service
POST /services/{serviceId}/restart

// Scale service
POST /services/{serviceId}/scale

// Get service logs
GET /services/{serviceId}/logs
```

### **3. API Integration Examples**

#### **Fetching Service Registry**
```typescript
// Automatically called on dashboard load
const fetchServices = async () => {
  try {
    const registry = await AIServicesAPI.getServiceRegistry()
    const services = await convertRegistryToServices(registry)
    setServices(services)
  } catch (error) {
    setError('Failed to fetch services')
  }
}
```

#### **Real-time Health Monitoring**
```typescript
// Real-time health updates via WebSocket
const unsubscribeHealth = subscribeToHealthUpdates((serviceId, health) => {
  updateServiceHealth(serviceId, {
    status: health.status,
    lastCheck: health.timestamp,
  })
})
```

---

## 📡 **WEBSOCKET INTEGRATION**

### **1. WebSocket Connection**

The dashboard establishes a WebSocket connection to:

```
ws://localhost:3002/ws
```

### **2. Real-time Message Types**

#### **Health Updates**
```typescript
{
  type: 'health_update',
  serviceId: 'predictive-analytics',
  timestamp: '2024-01-01T12:00:00Z',
  data: {
    status: 'healthy',
    uptime: 99.9,
    timestamp: '2024-01-01T12:00:00Z'
  }
}
```

#### **Metrics Updates**
```typescript
{
  type: 'metrics_update',
  serviceId: 'recommendation-service',
  timestamp: '2024-01-01T12:00:00Z',
  data: {
    totalRequests: 1500,
    successfulRequests: 1485,
    averageResponseTime: 95.2,
    uptime: 99.7
  }
}
```

#### **Service Status Changes**
```typescript
{
  type: 'service_status',
  serviceId: 'nlp-processing',
  timestamp: '2024-01-01T12:00:00Z',
  data: {
    status: 'degraded',
    previousStatus: 'healthy'
  }
}
```

### **3. WebSocket Reconnection Strategy**

- **Automatic Reconnection**: Exponential backoff
- **Max Attempts**: 10 reconnection attempts
- **Connection Status**: Displayed in UI header and status indicator
- **Heartbeat**: Connection health monitoring

---

## 🏪 **STATE MANAGEMENT INTEGRATION**

### **1. Zustand Store Structure**

The store is fully integrated with backend APIs:

```typescript
interface StoreState {
  // Data from backend
  services: AIService[]
  globalMetrics: GlobalMetrics

  // UI State
  loading: boolean
  error: string | null
  wsConnected: boolean
  lastUpdated: string | null

  // API Actions
  fetchServices: () => Promise<void>
  fetchHealth: () => Promise<void>
  fetchStats: () => Promise<void>
  refreshAllData: () => Promise<void>

  // Real-time actions
  initializeWebSocket: () => void
  cleanupWebSocket: () => void
}
```

### **2. Data Flow Architecture**

```
Backend Service → API Gateway → WebSocket → Frontend Store → UI Components
```

### **3. Automatic Data Synchronization**

- **Initial Load**: Fetch all services on app initialization
- **Real-time Updates**: WebSocket pushes changes instantly
- **Periodic Refresh**: Fallback polling every 30 seconds
- **Error Recovery**: Automatic retry on connection failures

---

## 🎯 **COMPONENT INTEGRATION**

### **1. Dashboard Components**

All dashboard components are integrated with real backend data:

- **DashboardStats**: Uses `globalMetrics` from store
- **ServiceHealth**: Real-time service status from backend
- **ActivityFeed**: Recent system events from WebSocket
- **QuickActions**: Service management API calls
- **MetricsOverview**: Live performance metrics

### **2. Service Detail Pages**

Dynamic service pages (`/services/[serviceId]`) with:

- **Overview Tab**: Service information and current status
- **Metrics Tab**: Real-time charts from backend metrics
- **Documentation Tab**: Links to service Swagger docs
- **Logs Tab**: Live log streaming from backend
- **Settings Tab**: Service configuration and management

### **3. Error Handling Integration**

- **API Errors**: Displayed in toast notifications and error banners
- **Connection Errors**: Automatic reconnection with status indicators
- **Service Errors**: Individual service error states and recovery
- **Network Errors**: Graceful degradation with offline support

---

## 🔄 **REAL-TIME FEATURES**

### **1. Live Health Monitoring**

- **Service Status**: Instant updates when services go down/up
- **Health Checks**: Real-time health check results
- **Status Indicators**: Visual status changes in UI
- **Alerts**: Automatic notifications for service failures

### **2. Live Metrics**

- **Request Counts**: Real-time request volume tracking
- **Response Times**: Live latency monitoring
- **Error Rates**: Instant error rate updates
- **Resource Usage**: CPU, memory, and network metrics

### **3. System Events**

- **Service Restarts**: Notifications when services restart
- **Scale Events**: Updates when services are scaled
- **Deployments**: Information about service deployments
- **Alerts**: System alerts and warnings

---

## 🛠️ **MANAGEMENT FEATURES**

### **1. Service Management**

#### **Restart Service**
```typescript
const handleRestart = async (serviceId: string) => {
  try {
    await AIServicesAPI.restartService(serviceId)
    // Optimistically update UI
    updateServiceHealth(serviceId, { status: 'degraded' })
  } catch (error) {
    setError('Failed to restart service')
  }
}
```

#### **Scale Service**
```typescript
const handleScale = async (serviceId: string, replicas: number) => {
  try {
    await AIServicesAPI.scaleService(serviceId, replicas)
  } catch (error) {
    setError('Failed to scale service')
  }
}
```

### **2. Log Management**

```typescript
const fetchLogs = async (serviceId: string, lines = 100) => {
  try {
    const logs = await AIServicesAPI.getServiceLogs(serviceId, lines)
    setLogs(logs)
  } catch (error) {
    setError('Failed to fetch logs')
  }
}
```

---

## 🧪 **TESTING INTEGRATION**

### **1. API Integration Tests**

```typescript
// Test service registry fetching
describe('Service Registry Integration', () => {
  it('should fetch services from API Gateway', async () => {
    const registry = await AIServicesAPI.getServiceRegistry()
    expect(registry.services).toBeDefined()
    expect(registry.totalServices).toBeGreaterThan(0)
  })
})
```

### **2. WebSocket Tests**

```typescript
// Test real-time updates
describe('WebSocket Integration', () => {
  it('should receive health updates', (done) => {
    const unsubscribe = subscribeToHealthUpdates((serviceId, health) => {
      expect(health.status).toBeDefined()
      done()
      unsubscribe()
    })
  })
})
```

### **3. End-to-End Tests**

```typescript
// Test complete workflow
describe('Dashboard Integration', () => {
  it('should display real-time service metrics', async () => {
    // Start dashboard
    render(<Dashboard />)

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Total Services')).toBeInTheDocument()
    })

    // Verify metrics
    expect(screen.getByText('48')).toBeInTheDocument()
  })
})
```

---

## 🚀 **DEPLOYMENT INTEGRATION**

### **1. Environment Configuration**

#### **Development**
```bash
NEXT_PUBLIC_API_GATEWAY_URL=http://localhost:3002
NEXT_PUBLIC_WS_URL=ws://localhost:3002/ws
NEXT_PUBLIC_ENVIRONMENT=development
```

#### **Production**
```bash
NEXT_PUBLIC_API_GATEWAY_URL=https://api.gogidix.com
NEXT_PUBLIC_WS_URL=wss://ws.gogidix.com/ws
NEXT_PUBLIC_ENVIRONMENT=production
```

### **2. Docker Integration**

```dockerfile
# Build with production environment variables
FROM node:18-alpine AS builder
WORKDIR /app
COPY . .
ENV NEXT_PUBLIC_API_GATEWAY_URL=https://api.gogidix.com
RUN npm run build

FROM node:18-alpine AS runner
WORKDIR /app
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
EXPOSE 3000
CMD ["npm", "start"]
```

### **3. Kubernetes Integration**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-dashboard
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-dashboard
  template:
    spec:
      containers:
      - name: ai-dashboard
        image: gogidix/ai-dashboard:latest
        ports:
        - containerPort: 3000
        env:
        - name: NEXT_PUBLIC_API_GATEWAY_URL
          value: "http://api-gateway:3002"
        - name: NEXT_PUBLIC_WS_URL
          value: "ws://api-gateway:3002/ws"
```

---

## 🔍 **TROUBLESHOOTING**

### **1. Connection Issues**

#### **Symptom**: Dashboard shows "Disconnected"
- **Check**: API Gateway status (port 3002)
- **Solution**: Restart API Gateway service
- **Command**: `curl http://localhost:3002/health`

#### **Symptom**: Services not loading
- **Check**: Service registry endpoint
- **Solution**: Verify individual services are running
- **Command**: Check Docker containers

### **2. WebSocket Issues**

#### **Symptom**: Real-time updates not working
- **Check**: WebSocket server status
- **Solution**: Verify WebSocket endpoint accessibility
- **Command**: `wscat -c ws://localhost:3002/ws`

#### **Symptom**: Frequent disconnections
- **Check**: Network stability and proxy configuration
- **Solution**: Adjust reconnection interval and timeout settings

### **3. Performance Issues**

#### **Symptom**: Slow dashboard loading
- **Check**: API response times
- **Solution**: Optimize backend service performance
- **Command**: Monitor API Gateway logs

---

## 🎯 **SUCCESS METRICS**

### **Integration Success Indicators**

- ✅ **All Services Loading**: 48 services displayed in dashboard
- ✅ **Real-time Updates**: Live health and metrics updates
- ✅ **WebSocket Connected**: Persistent real-time connection
- ✅ **Error Handling**: Graceful error recovery
- ✅ **Performance**: < 2 second initial load time
- ✅ **Reliability**: 99.9% uptime connection

### **Monitoring Integration**

- **Dashboard Health**: Connection status indicator
- **API Response Times**: Performance metrics tracking
- **Error Rates**: Integration error monitoring
- **User Experience**: Loading states and error messages

---

## 📞 **SUPPORT**

### **Integration Support Channels**

- **Documentation**: Complete API and integration guides
- **Error Logs**: Comprehensive error logging
- **Status Indicators**: Real-time connection and service status
- **Debug Mode**: Development debugging features

---

**The frontend dashboard is now fully integrated with the AI services backend, providing real-time monitoring, management, and visualization capabilities.** 🚀