'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/router'
import { ArrowLeftIcon } from '@heroicons/react/24/outline'
import Link from 'next/link'
import { toast } from 'react-hot-toast'
import { useStore } from '@/store'

interface ServiceDetails {
  id: string
  name: string
  description: string
  port: number
  category: string
  technology: string
  endpoint: string
  documentation: string
  metrics: {
    totalRequests: number
    successfulRequests: number
    averageResponseTime: number
    uptime: number
  }
  status: 'healthy' | 'degraded' | 'down'
  lastUpdate: string
}

export default function ServiceDetailPage({ params }: { params: { serviceId: string } }) {
  const router = useRouter()
  const { services, getServiceDetails, fetchServiceMetrics } = useStore()
  const [service, setService] = useState<ServiceDetails | null>(null)
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('overview')

  const serviceId = params.serviceId

  useEffect(() => {
    const loadService = async () => {
      try {
        setLoading(true)
        const serviceDetails = await getServiceDetails(serviceId)
        setService(serviceDetails)

        // Start real-time metrics updates
        const interval = setInterval(async () => {
          await fetchServiceMetrics(serviceId)
        }, 5000)

        return () => clearInterval(interval)
      } catch (error) {
        console.error('Failed to load service:', error)
        toast.error('Service not found')
        router.push('/')
      } finally {
        setLoading(false)
      }
    }

    loadService()
  }, [serviceId])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gogidix-600"></div>
      </div>
    )
  }

  if (!service) {
    return null
  }

  const getStatusIcon = () => {
    switch (service.status) {
      case 'healthy':
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />
      case 'degraded':
        return <InformationCircleIcon className="h-5 w-5 text-yellow-500" />
      case 'down':
        return <XCircleIcon className="h-5 w-5 text-red-500" />
    }
  }

  const getStatusColor = () => {
    switch (service.status) {
      case 'healthy':
        return 'bg-green-100 text-green-800'
      case 'degraded':
        return 'bg-yellow-100 text-yellow-800'
      case 'down':
        return 'bg-red-100 text-red-800'
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link
              href="/"
              className="flex items-center text-gray-500 hover:text-gray-700"
            >
              <ArrowLeftIcon className="h-5 w-5 mr-2" />
              Back to Dashboard
            </Link>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{service.name}</h1>
              <p className="text-gray-500">{service.description}</p>
            </div>
          </div>
          <div className={`flex items-center px-4 py-2 rounded-full ${getStatusColor()}`}>
            {getStatusIcon()}
            <span className="ml-2 font-medium capitalize">{service.status}</span>
          </div>
        </div>

        {/* Service Info */}
        <div className="mt-6 grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm text-gray-500">Port</p>
            <p className="text-lg font-semibold">{service.port}</p>
          </div>
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm text-gray-500">Technology</p>
            <p className="text-lg font-semibold">{service.technology}</p>
          </div>
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm text-gray-500">Category</p>
            <p className="text-lg font-semibold">{service.category}</p>
          </div>
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm text-gray-500">Endpoint</p>
            <p className="text-lg font-semibold truncate">{service.endpoint}</p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-lg shadow-sm">
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-6" aria-label="Tabs">
            {['overview', 'metrics', 'documentation', 'logs', 'settings'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === tab
                    ? 'border-gogidix-500 text-gogidix-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        <div className="p-6">
          {activeTab === 'overview' && (
            <div className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">Overview</h3>
                <p className="text-gray-600">
                  {service.description}
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-white rounded-lg p-6 border border-gray-200">
                  <h4 className="text-sm font-medium text-gray-900 mb-4">Performance Metrics</h4>
                  <dl className="space-y-3">
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Total Requests</dt>
                      <dd className="text-sm font-medium text-gray-900">
                        {service.metrics.totalRequests.toLocaleString()}
                      </dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Success Rate</dt>
                      <dd className="text-sm font-medium text-gray-900">
                        {((service.metrics.successfulRequests / service.metrics.totalRequests) * 100).toFixed(1)}%
                      </dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Avg Response Time</dt>
                      <dd className="text-sm font-medium text-gray-900">
                        {service.metrics.averageResponseTime.toFixed(2)}ms
                      </dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Uptime</dt>
                      <dd className="text-sm font-medium text-gray-900">
                        {service.metrics.uptime.toFixed(1)}%
                      </dd>
                    </div>
                  </dl>
                </div>

                <div className="bg-white rounded-lg p-6 border border-gray-200">
                  <h4 className="text-sm font-medium text-gray-900 mb-4">Quick Actions</h4>
                  <div className="space-y-3">
                    <Link
                      href={service.documentation}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block w-full text-center bg-gogidix-600 text-white py-2 px-4 rounded-lg hover:bg-gogidix-700 transition-colors"
                    >
                      View API Documentation
                    </Link>
                    <button
                      onClick={() => router.push(`/api-tester/${serviceId}`)}
                      className="w-full bg-white text-gogidix-600 py-2 px-4 rounded-lg border border-gogidix-300 hover:bg-gray-50 transition-colors"
                    >
                      Test API
                    </button>
                    <button
                      onClick={() => window.open(`http://localhost:${service.port}/health`, '_blank')}
                      className="w-full bg-white text-gogidix-600 py-2 px-4 rounded-lg border border-gogidix-300 hover:bg-gray-50 transition-colors"
                    >
                      Health Check
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'metrics' && (
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900">Service Metrics</h3>
              <p className="text-gray-600">
                Real-time performance and usage statistics
              </p>
              {/* Metrics component will be implemented here */}
              <div className="bg-gray-50 rounded-lg p-8 text-center text-gray-500">
                <ChartBarIcon className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                <p>Metrics visualization component will be implemented here</p>
              </div>
            </div>
          )}

          {activeTab === 'documentation' && (
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900">Documentation</h3>
              <p className="text-gray-600">
                API documentation and usage examples
              </p>
              <div className="bg-gray-50 rounded-lg p-8 text-center text-gray-500">
                <InformationCircleIcon className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                <p>Documentation component will be implemented here</p>
              </div>
            </div>
          )}

          {activeTab === 'logs' && (
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900">Service Logs</h3>
              <p className="text-gray-600">
                Recent log entries and error tracking
              </p>
              <div className="bg-gray-50 rounded-lg p-8 text-center text-gray-500">
                <ChartBarIcon className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                <p>Logs component will be implemented here</p>
              </div>
            </div>
          )}

          {activeTab === 'settings' && (
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900">Service Settings</h3>
              <p className="text-gray-600">
                Configuration and management options
              </p>
              <div className="bg-gray-50 rounded-lg p-8 text-center text-gray-500">
                <CpuChipIcon className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                <p>Settings component will be implemented here</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}