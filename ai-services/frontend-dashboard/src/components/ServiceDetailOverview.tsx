'use client'

import { useStore } from '@/store'
import {
  ServerIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  ClockIcon,
  ChartBarIcon,
  DocumentTextIcon,
  Cog6ToothIcon
} from '@heroicons/react/24/outline'

interface ServiceDetailOverviewProps {
  serviceId: string
}

export function ServiceDetailOverview({ serviceId }: ServiceDetailOverviewProps) {
  const { services } = useStore()
  const service = services.find(s => s.id === serviceId)

  if (!service) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Service not found</p>
      </div>
    )
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
        return 'bg-green-100 text-green-800'
      case 'degraded':
        return 'bg-yellow-100 text-yellow-800'
      case 'down':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'healthy':
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />
      case 'degraded':
        return <ExclamationTriangleIcon className="h-5 w-5 text-yellow-500" />
      case 'down':
        return <ExclamationTriangleIcon className="h-5 w-5 text-red-500" />
      default:
        return <ClockIcon className="h-5 w-5 text-gray-500" />
    }
  }

  const info = [
    {
      label: 'Service Name',
      value: service.name,
      icon: ServerIcon,
    },
    {
      label: 'Technology',
      value: service.technology,
      icon: Cog6ToothIcon,
    },
    {
      label: 'Category',
      value: service.category,
      icon: ChartBarIcon,
    },
    {
      label: 'Port',
      value: service.port.toString(),
      icon: DocumentTextIcon,
    },
  ]

  return (
    <div className="space-y-6">
      {/* Service Status Card */}
      <div className="bg-white shadow rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-medium text-gray-900">Service Status</h3>
          <div className="flex items-center space-x-2">
            {getStatusIcon(service.health.status)}
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(service.health.status)}`}>
              {service.health.status}
            </span>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="text-center">
            <p className="text-2xl font-semibold text-gray-900">{service.metrics.uptime.toFixed(1)}%</p>
            <p className="text-sm text-gray-500">Uptime</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-semibold text-gray-900">{service.metrics.averageResponseTime.toFixed(0)}ms</p>
            <p className="text-sm text-gray-500">Avg Response Time</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-semibold text-gray-900">{((service.metrics.successfulRequests / service.metrics.totalRequests) * 100).toFixed(1)}%</p>
            <p className="text-sm text-gray-500">Success Rate</p>
          </div>
        </div>
      </div>

      {/* Service Information */}
      <div className="bg-white shadow rounded-lg p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Service Information</h3>
        <dl className="space-y-4">
          {info.map((item) => (
            <div key={item.label} className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <item.icon className="h-5 w-5 text-gray-400" />
                <dt className="text-sm font-medium text-gray-500">{item.label}</dt>
              </div>
              <dd className="text-sm text-gray-900">{item.value}</dd>
            </div>
          ))}
          <div className="flex items-center justify-between pt-4 border-t border-gray-200">
            <div className="flex items-center space-x-3">
              <ClockIcon className="h-5 w-5 text-gray-400" />
              <dt className="text-sm font-medium text-gray-500">Last Health Check</dt>
            </div>
            <dd className="text-sm text-gray-900">
              {new Date(service.health.lastCheck).toLocaleString()}
            </dd>
          </div>
        </dl>
      </div>

      {/* Description */}
      <div className="bg-white shadow rounded-lg p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-2">Description</h3>
        <p className="text-gray-600">{service.description}</p>
      </div>

      {/* Quick Actions */}
      <div className="bg-white shadow rounded-lg p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            onClick={() => window.open(service.documentation, '_blank')}
            className="flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            <DocumentTextIcon className="h-4 w-4 mr-2" />
            View Documentation
          </button>
          <button
            onClick={() => window.open(service.endpoint, '_blank')}
            className="flex items-center justify-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            <ChartBarIcon className="h-4 w-4 mr-2" />
            Open API Endpoint
          </button>
        </div>
      </div>
    </div>
  )
}