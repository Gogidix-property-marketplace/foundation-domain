# BLUEPRINT GUIDELINES - API GATEWAY REFERENCE

## Gold Standard Blueprint Location

**Primary Reference**:
```
C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\java-services\api-gateway
```

## Integration Architecture Patterns

### 1. Service Communication
The API Gateway demonstrates:
- **RESTful API Design**: Consistent endpoint patterns
- **Authentication Flow**: JWT token validation
- **Rate Limiting**: Request throttling mechanisms
- **Circuit Breaking**: Failover and resilience patterns

### 2. Frontend-Backend Communication
Follow gateway's patterns:
- **CORS Configuration**: Proper cross-origin setup
- **Request/Response Format**: Standardized JSON structure
- **Error Handling**: Consistent error response format
- **Status Codes**: HTTP status code conventions

### 3. Real-time Communication
Learn from gateway implementations:
- **WebSocket Support**: Real-time bidirectional communication
- **Event Streaming**: Server-sent events implementation
- **Message Formats**: Consistent data serialization
- **Connection Management**: Connection lifecycle handling

## Frontend Architecture Guidelines

### Component Structure
```typescript
// Follow API Gateway's modular approach
src/
├── components/           # Reusable UI components
│   ├── common/          # Shared components
│   ├── charts/          # Data visualization
│   └── forms/           # Form components
├── pages/               # Page-level components
├── hooks/               # Custom React hooks
├── services/            # API service layers
├── store/               # State management
├── utils/               # Utility functions
└── types/               # TypeScript definitions
```

### API Service Integration
```typescript
// Mirror API Gateway's service patterns
class DashboardService {
  private apiClient: ApiClient;

  constructor() {
    this.apiClient = new ApiClient({
      baseURL: process.env.REACT_APP_API_URL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  async getSystemHealth(): Promise<SystemHealth> {
    return this.apiClient.get('/api/v1/system/health');
  }
}
```

### Authentication Integration
```typescript
// Follow API Gateway's JWT validation
const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      validateToken(token).then(setUser).catch(() => {
        localStorage.removeItem('jwt_token');
      });
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
```

## Backend Integration Guidelines

### RESTful API Design
```java
// Follow API Gateway's controller patterns
@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "${app.frontend.url}")
public class DashboardController {

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemOverview> getSystemOverview() {
        // Implementation following gateway patterns
    }

    @GetMapping("/metrics")
    public ResponseEntity<SystemMetrics> getSystemMetrics(
        @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime end) {
        // Implementation
    }
}
```

### Error Handling Standards
```java
// Mirror API Gateway's error handling
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message(ex.getMessage())
            .path(getCurrentPath())
            .build();
        return ResponseEntity.badRequest().body(error);
    }
}
```

## Data Visualization Standards

### Chart Implementation
```typescript
// Follow consistent chart patterns
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';

const PerformanceChart: React.FC<PerformanceChartProps> = ({ data }) => {
  return (
    <div className="chart-container">
      <h3>System Performance</h3>
      <LineChart width={800} height={400} data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="timestamp" />
        <YAxis />
        <Tooltip />
        <Line
          type="monotone"
          dataKey="responseTime"
          stroke="#8884d8"
          strokeWidth={2}
        />
      </LineChart>
    </div>
  );
};
```

### Real-time Data Updates
```typescript
// Follow API Gateway's real-time patterns
const useRealTimeData = (endpoint: string) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const socket = io(process.env.REACT_APP_WS_URL);

    socket.on(endpoint, (newData) => {
      setData(newData);
      setLoading(false);
    });

    return () => socket.disconnect();
  }, [endpoint]);

  return { data, loading };
};
```

## Performance Optimization Guidelines

### Frontend Performance
- **Code Splitting**: Lazy loading for optimal performance
```typescript
const LazyDashboard = lazy(() => import('./Dashboard'));

const App: React.FC = () => (
  <Suspense fallback={<LoadingSpinner />}>
    <LazyDashboard />
  </Suspense>
);
```

- **Caching Strategy**: Service worker implementation
```typescript
// Follow API Gateway's caching patterns
self.addEventListener('fetch', event => {
  if (event.request.url.includes('/api/v1/metrics')) {
    event.respondWith(
      caches.match(event.request).then(response => {
        return response || fetch(event.request).then(fetchResponse => {
          caches.open('dashboard-cache-v1').then(cache => {
            cache.put(event.request, fetchResponse.clone());
          });
          return fetchResponse;
        });
      })
    );
  }
});
```

### Backend Performance
- **Connection Pooling**: Database connection management
```yaml
# Follow API Gateway's database configuration
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

- **Caching Layer**: Redis configuration
```java
// Mirror API Gateway's caching patterns
@Service
public class DashboardService {

    @Cacheable(value = "systemMetrics", key = "#period")
    public SystemMetrics getSystemMetrics(TimePeriod period) {
        // Implementation
    }
}
```

## Security Integration Guidelines

### Authentication Flow
```typescript
// Follow API Gateway's authentication
const login = async (credentials: LoginCredentials): Promise<void> => {
  try {
    const response = await apiClient.post('/auth/login', credentials);
    const { token, refreshToken } = response.data;

    localStorage.setItem('jwt_token', token);
    localStorage.setItem('refresh_token', refreshToken);

    setUser(response.data.user);
  } catch (error) {
    throw new AuthenticationError('Invalid credentials');
  }
};
```

### Route Protection
```typescript
// Implement role-based access control
const ProtectedRoute: React.FC<{ children: React.ReactNode; roles: string[] }> = ({
  children,
  roles
}) => {
  const { user } = useAuth();

  if (!user || !roles.some(role => user.roles.includes(role))) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};
```

## Testing Guidelines

### Frontend Testing
```typescript
// Component testing following gateway patterns
describe('Dashboard Overview', () => {
  test('displays system metrics correctly', async () => {
    const mockMetrics = createMockSystemMetrics();

    render(<DashboardOverview metrics={mockMetrics} />);

    expect(screen.getByText('CPU Usage')).toBeInTheDocument();
    expect(screen.getByText('75%')).toBeInTheDocument();
  });
});
```

### Integration Testing
```java
// API integration testing
@SpringBootTest
@AutoConfigureTestDatabase
class DashboardControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnSystemOverview() {
        ResponseEntity<SystemOverview> response = restTemplate.getForEntity(
            "/api/v1/dashboard/overview",
            SystemOverview.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
```

## Deployment Standards

### Docker Configuration
```dockerfile
# Follow API Gateway's containerization approach
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:18-alpine
WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

### Kubernetes Deployment
```yaml
# Mirror API Gateway's deployment patterns
apiVersion: apps/v1
kind: Deployment
metadata:
  name: centralized-dashboard
spec:
  replicas: 3
  selector:
    matchLabels:
      app: centralized-dashboard
  template:
    metadata:
      labels:
        app: centralized-dashboard
    spec:
      containers:
      - name: dashboard
        image: gogidix/centralized-dashboard:latest
        ports:
        - containerPort: 3000
        env:
        - name: API_GATEWAY_URL
          value: "http://api-gateway:8080"
```

## Monitoring and Observability

### Frontend Monitoring
```typescript
// Follow API Gateway's monitoring patterns
import * as Sentry from '@sentry/react';

Sentry.init({
  dsn: process.env.REACT_APP_SENTRY_DSN,
  environment: process.env.REACT_APP_ENVIRONMENT,
  tracesSampleRate: 1.0,
});

const trackUserAction = (action: string, data?: any) => {
  window.gtag('event', action, data);
  Sentry.addBreadcrumb({
    message: action,
    data: data,
    level: 'info'
  });
};
```

### Backend Monitoring
```java
// Mirror API Gateway's monitoring setup
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final MeterRegistry meterRegistry;

    @GetMapping("/metrics")
    public ResponseEntity<SystemMetrics> getSystemMetrics() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SystemMetrics metrics = dashboardService.getSystemMetrics();
            meterRegistry.counter("dashboard.metrics.requests").increment();
            return ResponseEntity.ok(metrics);
        } finally {
            sample.stop(Timer.builder("dashboard.metrics.response.time").register(meterRegistry));
        }
    }
}
```

---

**Reference Document**: API Gateway Implementation
**Last Updated**: 2025-11-28
**Review Frequency**: Monthly
**Approval Required**: CTO Architecture Review