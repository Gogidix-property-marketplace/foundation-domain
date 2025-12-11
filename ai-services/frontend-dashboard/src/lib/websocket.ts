import { AIService, HealthResponse, MetricsResponse } from '@/types'

// WebSocket Configuration
const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:3002/ws'
const RECONNECT_INTERVAL = 5000 // 5 seconds
const MAX_RECONNECT_ATTEMPTS = 10

export interface WebSocketMessage {
  type: 'health_update' | 'metrics_update' | 'service_status' | 'system_event'
  serviceId?: string
  timestamp: string
  data: any
}

export class WebSocketManager {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private isConnecting = false
  private messageHandlers: Map<string, (message: WebSocketMessage) => void> = new Map()
  private connectionHandlers: ((connected: boolean) => void)[] = []

  constructor() {
    this.connect()
  }

  // Connect to WebSocket server
  private connect(): void {
    if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
      return
    }

    this.isConnecting = true
    console.log('Connecting to WebSocket server...')

    try {
      this.ws = new WebSocket(WS_URL)

      this.ws.onopen = () => {
        console.log('WebSocket connected')
        this.isConnecting = false
        this.reconnectAttempts = 0
        this.notifyConnectionHandlers(true)
      }

      this.ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)
          this.handleMessage(message)
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error)
        }
      }

      this.ws.onclose = () => {
        console.log('WebSocket disconnected')
        this.isConnecting = false
        this.notifyConnectionHandlers(false)
        this.scheduleReconnect()
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        this.isConnecting = false
      }

    } catch (error) {
      console.error('Failed to create WebSocket connection:', error)
      this.isConnecting = false
      this.scheduleReconnect()
    }
  }

  // Schedule reconnection attempt
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.log('Max reconnection attempts reached')
      return
    }

    this.reconnectAttempts++
    console.log(`Scheduling reconnect attempt ${this.reconnectAttempts} in ${RECONNECT_INTERVAL / 1000} seconds`)

    setTimeout(() => {
      this.connect()
    }, RECONNECT_INTERVAL * this.reconnectAttempts)
  }

  // Handle incoming WebSocket messages
  private handleMessage(message: WebSocketMessage): void {
    const handler = this.messageHandlers.get(message.type)
    if (handler) {
      handler(message)
    }

    // Also call global handler
    const globalHandler = this.messageHandlers.get('*')
    if (globalHandler) {
      globalHandler(message)
    }
  }

  // Subscribe to specific message types
  public subscribe(messageType: string, handler: (message: WebSocketMessage) => void): () => void {
    this.messageHandlers.set(messageType, handler)

    // Return unsubscribe function
    return () => {
      this.messageHandlers.delete(messageType)
    }
  }

  // Subscribe to connection status changes
  public onConnectionChange(handler: (connected: boolean) => void): () => void {
    this.connectionHandlers.push(handler)

    // Return unsubscribe function
    return () => {
      const index = this.connectionHandlers.indexOf(handler)
      if (index > -1) {
        this.connectionHandlers.splice(index, 1)
      }
    }
  }

  // Notify all connection handlers
  private notifyConnectionHandlers(connected: boolean): void {
    this.connectionHandlers.forEach(handler => {
      try {
        handler(connected)
      } catch (error) {
        console.error('Error in connection handler:', error)
      }
    })
  }

  // Send message to server
  public send(message: Partial<WebSocketMessage>): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const fullMessage: WebSocketMessage = {
        type: message.type || 'system_event',
        timestamp: new Date().toISOString(),
        data: message.data || {},
        ...message
      }
      this.ws.send(JSON.stringify(fullMessage))
    } else {
      console.warn('WebSocket not connected, cannot send message')
    }
  }

  // Get connection status
  public get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  // Disconnect WebSocket
  public disconnect(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.messageHandlers.clear()
    this.connectionHandlers = []
  }
}

// Singleton instance
let wsManager: WebSocketManager | null = null

export const getWebSocketManager = (): WebSocketManager => {
  if (!wsManager) {
    wsManager = new WebSocketManager()
  }
  return wsManager
}

// Utility functions for specific use cases
export const subscribeToHealthUpdates = (
  callback: (serviceId: string, health: HealthResponse) => void
): (() => void) => {
  const wsManager = getWebSocketManager()

  return wsManager.subscribe('health_update', (message) => {
    if (message.serviceId) {
      callback(message.serviceId, message.data as HealthResponse)
    }
  })
}

export const subscribeToMetricsUpdates = (
  callback: (serviceId: string, metrics: MetricsResponse) => void
): (() => void) => {
  const wsManager = getWebSocketManager()

  return wsManager.subscribe('metrics_update', (message) => {
    if (message.serviceId) {
      callback(message.serviceId, message.data as MetricsResponse)
    }
  })
}

export const subscribeToServiceStatus = (
  callback: (serviceId: string, status: string) => void
): (() => void) => {
  const wsManager = getWebSocketManager()

  return wsManager.subscribe('service_status', (message) => {
    if (message.serviceId) {
      callback(message.serviceId, message.data.status)
    }
  })
}

export const subscribeToSystemEvents = (
  callback: (event: any) => void
): (() => void) => {
  const wsManager = getWebSocketManager()

  return wsManager.subscribe('system_event', (message) => {
    callback(message.data)
  })
}

export default getWebSocketManager