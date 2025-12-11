import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import React from 'react'
import AIServicesAPI, {
  HealthResponse,
  MetricsResponse,
  ServiceRegistryResponse
} from '@/lib/api'
import {
  getWebSocketManager,
  subscribeToHealthUpdates,
  subscribeToMetricsUpdates,
  subscribeToServiceStatus,
  subscribeToSystemEvents
} from '@/lib/websocket'

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

interface StoreState {
  services: AIService[]
  globalMetrics: {
    totalServices: number
    healthyServices: number
    averageUptime: number
    totalRequests: number
    totalErrors: number
  }
  loading: boolean
  error: string | null
  wsConnected: boolean
  lastUpdated: string | null
}

interface StoreActions {
  setServices: (services: AIService[]) => void
  updateServiceHealth: (serviceId: string, health: AIService['health']) => void
  updateServiceMetrics: (serviceId: string, metrics: AIService['metrics']) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
  setWsConnected: (connected: boolean) => void
  setLastUpdated: (timestamp: string) => void

  // API Actions
  fetchServices: () => Promise<void>
  fetchHealth: () => Promise<void>
  fetchStats: () => Promise<void>
  getServiceDetails: (serviceId: string) => Promise<AIService>
  fetchServiceMetrics: (serviceId: string) => Promise<void>
  refreshAllData: () => Promise<void>

  // Service Management Actions
  restartService: (serviceId: string) => Promise<void>
  scaleService: (serviceId: string, replicas: number) => Promise<void>
  getServiceLogs: (serviceId: string, lines?: number) => Promise<any>

  // Initialize real-time updates
  initializeWebSocket: () => void
  cleanupWebSocket: () => void
}

export const useStore = create<StoreState & StoreActions>()(
  devtools(
    (set, get) => {
      // Initialize WebSocket subscriptions
      let wsUnsubscribers: (() => void)[] = []

      const initializeWebSocket = () => {
        const wsManager = getWebSocketManager()

        // Subscribe to connection status
        const unsubscribeConnection = wsManager.onConnectionChange((connected) => {
          set({ wsConnected: connected })
        })

        // Subscribe to health updates
        const unsubscribeHealth = subscribeToHealthUpdates((serviceId, health) => {
          get().updateServiceHealth(serviceId, {
            status: health.status,
            lastCheck: health.timestamp,
          })
        })

        // Subscribe to metrics updates
        const unsubscribeMetrics = subscribeToMetricsUpdates((serviceId, metrics) => {
          get().updateServiceMetrics(serviceId, {
            totalRequests: metrics.metrics.totalRequests,
            successfulRequests: metrics.metrics.successfulRequests,
            averageResponseTime: metrics.metrics.averageResponseTime,
            uptime: metrics.metrics.uptime,
          })
        })

        // Subscribe to service status changes
        const unsubscribeStatus = subscribeToServiceStatus((serviceId, status) => {
          get().updateServiceHealth(serviceId, {
            status: status as 'healthy' | 'degraded' | 'down',
            lastCheck: new Date().toISOString(),
          })
        })

        wsUnsubscribers = [
          unsubscribeConnection,
          unsubscribeHealth,
          unsubscribeMetrics,
          unsubscribeStatus,
        ]
      }

      const cleanupWebSocket = () => {
        wsUnsubscribers.forEach(unsubscribe => unsubscribe())
        wsUnsubscribers = []
        getWebSocketManager().disconnect()
      }

      // Convert registry response to internal service format
      const convertRegistryToServices = async (registry: ServiceRegistryResponse): Promise<AIService[]> => {
        const healthMap = await AIServicesAPI.getAllServicesHealth()
        const metricsMap = await AIServicesAPI.getAllServicesMetrics()

        return registry.services.map(service => {
          const health = healthMap.get(service.id)
          const metrics = metricsMap.get(service.id)

          return {
            id: service.id,
            name: service.name,
            description: service.description,
            port: service.port,
            category: service.category,
            technology: service.technology,
            endpoint: service.endpoint,
            documentation: service.documentation,
            health: {
              status: health?.status || 'down',
              lastCheck: health?.timestamp || service.lastHealthCheck,
            },
            metrics: {
              totalRequests: metrics?.metrics.totalRequests || 0,
              successfulRequests: metrics?.metrics.successfulRequests || 0,
              averageResponseTime: metrics?.metrics.averageResponseTime || 0,
              uptime: metrics?.metrics.uptime || 0,
            },
          }
        })
      }

      return {
        services: [],
        globalMetrics: {
          totalServices: 0,
          healthyServices: 0,
          averageUptime: 0,
          totalRequests: 0,
          totalErrors: 0,
        },
        loading: false,
        error: null,
        wsConnected: false,
        lastUpdated: null,

        setServices: (services) => {
          const healthyServices = services.filter((s) => s.health.status === 'healthy').length
          const totalRequests = services.reduce((acc, s) => acc + s.metrics.totalRequests, 0)
          const totalErrors = services.reduce((acc, s) => acc + (s.metrics.totalRequests - s.metrics.successfulRequests), 0)
          const avgUptime = services.length > 0 ? services.reduce((acc, s) => acc + s.metrics.uptime, 0) / services.length : 0

          set({
            services,
            globalMetrics: {
              totalServices: services.length,
              healthyServices,
              averageUptime: avgUptime,
              totalRequests,
              totalErrors,
            },
            lastUpdated: new Date().toISOString(),
          })
        },

        updateServiceHealth: (serviceId, health) =>
          set((state) => {
            const updatedServices = state.services.map((service) =>
              service.id === serviceId ? { ...service, health } : service
            )

            const healthyServices = updatedServices.filter((s) => s.health.status === 'healthy').length
            const avgUptime = updatedServices.length > 0
              ? updatedServices.reduce((acc, s) => acc + s.metrics.uptime, 0) / updatedServices.length
              : 0

            return {
              services: updatedServices,
              globalMetrics: {
                ...state.globalMetrics,
                healthyServices,
                averageUptime: avgUptime,
              },
              lastUpdated: new Date().toISOString(),
            }
          }),

        updateServiceMetrics: (serviceId, metrics) =>
          set((state) => {
            const updatedServices = state.services.map((service) =>
              service.id === serviceId ? { ...service, metrics } : service
            )

            const totalRequests = updatedServices.reduce((acc, s) => acc + s.metrics.totalRequests, 0)
            const totalErrors = updatedServices.reduce((acc, s) => acc + (s.metrics.totalRequests - s.metrics.successfulRequests), 0)

            return {
              services: updatedServices,
              globalMetrics: {
                ...state.globalMetrics,
                totalRequests,
                totalErrors,
              },
              lastUpdated: new Date().toISOString(),
            }
          }),

        setLoading: (loading) => set({ loading }),
        setError: (error) => set({ error }),
        setWsConnected: (connected) => set({ wsConnected: connected }),
        setLastUpdated: (timestamp) => set({ lastUpdated: timestamp }),

        // API Actions using real backend
        fetchServices: async () => {
          try {
            set({ loading: true, error: null })
            const registry = await AIServicesAPI.getServiceRegistry()
            const services = await convertRegistryToServices(registry)
            get().setServices(services)
          } catch (error) {
            console.error('Failed to fetch services:', error)
            set({ error: error instanceof Error ? error.message : 'Failed to fetch services' })
          } finally {
            set({ loading: false })
          }
        },

        fetchHealth: async () => {
          try {
            set({ loading: true, error: null })
            const healthMap = await AIServicesAPI.getAllServicesHealth()

            healthMap.forEach((health, serviceId) => {
              get().updateServiceHealth(serviceId, {
                status: health.status,
                lastCheck: health.timestamp,
              })
            })
          } catch (error) {
            console.error('Failed to fetch health status:', error)
            set({ error: error instanceof Error ? error.message : 'Failed to fetch health status' })
          } finally {
            set({ loading: false })
          }
        },

        fetchStats: async () => {
          try {
            set({ loading: true, error: null })
            const metricsMap = await AIServicesAPI.getAllServicesMetrics()

            metricsMap.forEach((metrics, serviceId) => {
              get().updateServiceMetrics(serviceId, {
                totalRequests: metrics.metrics.totalRequests,
                successfulRequests: metrics.metrics.successfulRequests,
                averageResponseTime: metrics.metrics.averageResponseTime,
                uptime: metrics.metrics.uptime,
              })
            })
          } catch (error) {
            console.error('Failed to fetch stats:', error)
            set({ error: error instanceof Error ? error.message : 'Failed to fetch stats' })
          } finally {
            set({ loading: false })
          }
        },

        refreshAllData: async () => {
          await Promise.all([
            get().fetchServices(),
            get().fetchHealth(),
            get().fetchStats(),
          ])
        },

        getServiceDetails: async (serviceId: string) => {
          const service = get().services.find((s) => s.id === serviceId)
          if (!service) {
            throw new Error('Service not found')
          }
          return service
        },

        fetchServiceMetrics: async (serviceId: string) => {
          try {
            const metrics = await AIServicesAPI.getServiceMetrics(serviceId)
            get().updateServiceMetrics(serviceId, {
              totalRequests: metrics.metrics.totalRequests,
              successfulRequests: metrics.metrics.successfulRequests,
              averageResponseTime: metrics.metrics.averageResponseTime,
              uptime: metrics.metrics.uptime,
            })
            return metrics
          } catch (error) {
            console.error(`Failed to fetch metrics for ${serviceId}:`, error)
            throw error
          }
        },

        restartService: async (serviceId: string) => {
          try {
            await AIServicesAPI.restartService(serviceId)
            // Optimistically update service status
            get().updateServiceHealth(serviceId, {
              status: 'degraded',
              lastCheck: new Date().toISOString(),
            })
          } catch (error) {
            console.error(`Failed to restart service ${serviceId}:`, error)
            throw error
          }
        },

        scaleService: async (serviceId: string, replicas: number) => {
          try {
            await AIServicesAPI.scaleService(serviceId, replicas)
          } catch (error) {
            console.error(`Failed to scale service ${serviceId}:`, error)
            throw error
          }
        },

        getServiceLogs: async (serviceId: string, lines = 100) => {
          try {
            return await AIServicesAPI.getServiceLogs(serviceId, lines)
          } catch (error) {
            console.error(`Failed to fetch logs for ${serviceId}:`, error)
            throw error
          }
        },

        initializeWebSocket,
        cleanupWebSocket,
      }
    },
    {
      name: 'ai-services-store',
    }
  )
)

// Hook to initialize store with data
export const useInitializeStore = () => {
  const { fetchServices, initializeWebSocket, cleanupWebSocket } = useStore()

  // Fetch initial data
  React.useEffect(() => {
    fetchServices()
  }, [fetchServices])

  // Initialize WebSocket
  React.useEffect(() => {
    initializeWebSocket()
    return cleanupWebSocket
  }, [initializeWebSocket, cleanupWebSocket])
}

// Hook for real-time updates
export const useRealtimeUpdates = () => {
  const { wsConnected, lastUpdated } = useStore()

  return {
    isConnected: wsConnected,
    lastUpdated,
  }
}