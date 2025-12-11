import axios, { AxiosInstance, AxiosResponse, AxiosError } from 'axios'

// API Configuration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_GATEWAY_URL || 'http://localhost:3002'
const API_TIMEOUT = 10000 // 10 seconds

// Create axios instance with default configuration
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor for adding auth token (if needed)
apiClient.interceptors.request.use(
  (config) => {
    // Add authentication token if available
    const token = localStorage.getItem('auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response
  },
  (error: AxiosError) => {
    console.error('API Error:', error)

    // Handle different types of errors
    if (error.response) {
      // Server responded with error status
      switch (error.response.status) {
        case 401:
          // Unauthorized - redirect to login
          window.location.href = '/login'
          break
        case 403:
          // Forbidden - insufficient permissions
          console.error('Insufficient permissions to access this resource')
          break
        case 404:
          // Not found
          console.error('Resource not found')
          break
        case 500:
          // Server error
          console.error('Internal server error')
          break
        default:
          console.error('Unknown error occurred')
      }
    } else if (error.request) {
      // Network error
      console.error('Network error - unable to reach server')
    } else {
      // Other error
      console.error('Error:', error.message)
    }

    return Promise.reject(error)
  }
)

// Retry logic for failed requests
export const retryRequest = async (
  fn: () => Promise<any>,
  retries: number = 3,
  delay: number = 1000
): Promise<any> => {
  try {
    return await fn()
  } catch (error) {
    if (retries === 0) {
      throw error
    }

    await new Promise(resolve => setTimeout(resolve, delay))
    return retryRequest(fn, retries - 1, delay * 2)
  }
}

// API response wrapper
export interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
  timestamp: string
}

// Health check interfaces
export interface HealthResponse {
  status: 'healthy' | 'degraded' | 'down'
  timestamp: string
  uptime: number
  version: string
  services?: {
    database: 'up' | 'down'
    cache: 'up' | 'down'
    [key: string]: 'up' | 'down'
  }
}

// Service metrics interfaces
export interface MetricsResponse {
  serviceId: string
  timestamp: string
  metrics: {
    totalRequests: number
    successfulRequests: number
    failedRequests: number
    averageResponseTime: number
    p95ResponseTime: number
    p99ResponseTime: number
    requestsPerMinute: number
    errorRate: number
    uptime: number
    cpuUsage: number
    memoryUsage: number
  }
}

// Service registry interfaces
export interface ServiceRegistryResponse {
  services: Array<{
    id: string
    name: string
    port: number
    technology: string
    category: string
    description: string
    endpoint: string
    documentation: string
    healthEndpoint: string
    metricsEndpoint: string
    status: 'healthy' | 'degraded' | 'down'
    lastHealthCheck: string
  }>
  totalServices: number
  healthyServices: number
  timestamp: string
}

// API Functions
export class AIServicesAPI {
  // Gateway and System endpoints
  static async getSystemHealth(): Promise<HealthResponse> {
    const response = await apiClient.get<ApiResponse<HealthResponse>>('/health')
    return response.data.data
  }

  static async getServiceRegistry(): Promise<ServiceRegistryResponse> {
    const response = await apiClient.get<ApiResponse<ServiceRegistryResponse>>('/services')
    return response.data.data
  }

  static async getSystemMetrics(): Promise<any> {
    const response = await apiClient.get<ApiResponse<any>>('/metrics')
    return response.data.data
  }

  static async getSystemStats(): Promise<any> {
    const response = await apiClient.get<ApiResponse<any>>('/stats')
    return response.data.data
  }

  // Individual service endpoints
  static async getServiceHealth(servicePort: number): Promise<HealthResponse> {
    // For individual services, we need to call them directly
    const response = await axios.get(`http://localhost:${servicePort}/health`, {
      timeout: 5000,
    })
    return response.data
  }

  static async getServiceMetrics(serviceId: string): Promise<MetricsResponse> {
    const response = await apiClient.get<ApiResponse<MetricsResponse>>(`/services/${serviceId}/metrics`)
    return response.data.data
  }

  static async getServiceStats(serviceId: string): Promise<any> {
    const response = await apiClient.get<ApiResponse<any>>(`/services/${serviceId}/stats`)
    return response.data.data
  }

  // Service management endpoints
  static async restartService(serviceId: string): Promise<ApiResponse<any>> {
    const response = await apiClient.post<ApiResponse<any>>(`/services/${serviceId}/restart`)
    return response.data
  }

  static async scaleService(serviceId: string, replicas: number): Promise<ApiResponse<any>> {
    const response = await apiClient.post<ApiResponse<any>>(`/services/${serviceId}/scale`, { replicas })
    return response.data
  }

  static async getServiceLogs(serviceId: string, lines: number = 100): Promise<any> {
    const response = await apiClient.get<ApiResponse<any>>(`/services/${serviceId}/logs?lines=${lines}`)
    return response.data.data
  }

  // Batch operations
  static async getAllServicesHealth(): Promise<Map<string, HealthResponse>> {
    const registry = await this.getServiceRegistry()
    const healthPromises = registry.services.map(async (service) => {
      try {
        const health = await this.getServiceHealth(service.port)
        return [service.id, health]
      } catch (error) {
        return [service.id, {
          status: 'down' as const,
          timestamp: new Date().toISOString(),
          uptime: 0,
          version: 'unknown'
        }]
      }
    })

    const healthResults = await Promise.all(healthPromises)
    return new Map(healthResults)
  }

  static async getAllServicesMetrics(): Promise<Map<string, MetricsResponse>> {
    const registry = await this.getServiceRegistry()
    const metricsPromises = registry.services.map(async (service) => {
      try {
        const metrics = await this.getServiceMetrics(service.id)
        return [service.id, metrics]
      } catch (error) {
        return [service.id, {
          serviceId: service.id,
          timestamp: new Date().toISOString(),
          metrics: {
            totalRequests: 0,
            successfulRequests: 0,
            failedRequests: 0,
            averageResponseTime: 0,
            p95ResponseTime: 0,
            p99ResponseTime: 0,
            requestsPerMinute: 0,
            errorRate: 0,
            uptime: 0,
            cpuUsage: 0,
            memoryUsage: 0,
          }
        }]
      }
    })

    const metricsResults = await Promise.all(metricsPromises)
    return new Map(metricsResults)
  }
}

export default AIServicesAPI