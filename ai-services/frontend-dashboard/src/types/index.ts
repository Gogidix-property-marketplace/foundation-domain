export interface AIService {
  id: string
  name: string
  description: string
  port: number
  category: string
  technology: string
  endpoint: string
  documentation: string
  health: {
    status: 'healthy' | 'degraded' | 'down'
    lastCheck: string
  }
  metrics: {
    totalRequests: number
    successfulRequests: number
    averageResponseTime: number
    uptime: number
  }
}

export interface GlobalMetrics {
  totalServices: number
  healthyServices: number
  averageUptime: number
  totalRequests: number
  totalErrors: number
}

export interface NavigationItem {
  name: string
  href: string
  icon: React.ComponentType<{ className?: string }>
  current?: boolean
  children?: NavigationItem[]
}