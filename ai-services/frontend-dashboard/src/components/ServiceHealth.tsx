'use client'

import { useStore } from '@/store'
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  XCircleIcon
} from '@heroicons/react/24/outline'

export function ServiceHealth() {
  const { services } = useStore()

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'healthy':
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />
      case 'degraded':
        return <ExclamationTriangleIcon className="h-5 w-5 text-yellow-500" />
      case 'down':
        return <XCircleIcon className="h-5 w-5 text-red-500" />
      default:
        return <ExclamationTriangleIcon className="h-5 w-5 text-gray-500" />
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
        return 'text-green-800 bg-green-100'
      case 'degraded':
        return 'text-yellow-800 bg-yellow-100'
      case 'down':
        return 'text-red-800 bg-red-100'
      default:
        return 'text-gray-800 bg-gray-100'
    }
  }

  return (
    <div className="bg-white shadow rounded-lg">
      <div className="px-4 py-5 sm:p-6">
        <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
          Service Health Overview
        </h3>
        <div className="space-y-4">
          {services.slice(0, 6).map((service) => (
            <div key={service.id} className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                {getStatusIcon(service.health.status)}
                <div>
                  <p className="text-sm font-medium text-gray-900">{service.name}</p>
                  <p className="text-xs text-gray-500">{service.technology}</p>
                </div>
              </div>
              <div className="flex items-center space-x-4">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(service.health.status)}`}>
                  {service.health.status}
                </span>
                <span className="text-sm text-gray-500">Port: {service.port}</span>
              </div>
            </div>
          ))}
          {services.length > 6 && (
            <div className="text-center pt-2">
              <button className="text-sm text-indigo-600 hover:text-indigo-900 font-medium">
                View all {services.length} services →
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}