'use client'

import { useStore } from '@/store'
import {
  ChartBarIcon,
  ClockIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline'

export function MetricsOverview() {
  const { services } = useStore()

  // Calculate metrics
  const totalRequests = services.reduce((acc, service) => acc + service.metrics.totalRequests, 0)
  const successfulRequests = services.reduce((acc, service) => acc + service.metrics.successfulRequests, 0)
  const averageResponseTime = services.reduce((acc, service) => acc + service.metrics.averageResponseTime, 0) / services.length
  const averageUptime = services.reduce((acc, service) => acc + service.metrics.uptime, 0) / services.length
  const successRate = totalRequests > 0 ? (successfulRequests / totalRequests * 100) : 100

  const metrics = [
    {
      label: 'Success Rate',
      value: `${successRate.toFixed(1)}%`,
      icon: CheckCircleIcon,
      trend: 'up',
      trendValue: '+2.1%',
      color: 'text-green-600',
    },
    {
      label: 'Avg Response Time',
      value: `${averageResponseTime.toFixed(0)}ms`,
      icon: ClockIcon,
      trend: 'down',
      trendValue: '-12ms',
      color: 'text-blue-600',
    },
    {
      label: 'System Uptime',
      value: `${averageUptime.toFixed(1)}%`,
      icon: ChartBarIcon,
      trend: 'stable',
      trendValue: '0%',
      color: 'text-purple-600',
    },
    {
      label: 'Error Rate',
      value: `${(100 - successRate).toFixed(1)}%`,
      icon: ExclamationTriangleIcon,
      trend: 'down',
      trendValue: '-0.8%',
      color: 'text-yellow-600',
    },
  ]

  const getTrendColor = (trend: string) => {
    switch (trend) {
      case 'up':
        return 'text-green-600'
      case 'down':
        return 'text-red-600'
      default:
        return 'text-gray-600'
    }
  }

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'up':
        return '↗'
      case 'down':
        return '↘'
      default:
        return '→'
    }
  }

  return (
    <div className="bg-white shadow rounded-lg">
      <div className="px-4 py-5 sm:p-6">
        <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
          Performance Metrics
        </h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {metrics.map((metric) => (
            <div key={metric.label} className="text-center">
              <div className="flex justify-center">
                <metric.icon className={`h-8 w-8 ${metric.color}`} />
              </div>
              <p className="mt-2 text-2xl font-semibold text-gray-900">{metric.value}</p>
              <p className="text-sm text-gray-500">{metric.label}</p>
              <div className={`mt-1 flex items-center justify-center text-xs ${getTrendColor(metric.trend)}`}>
                <span className="mr-1">{getTrendIcon(metric.trend)}</span>
                <span>{metric.trendValue}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Additional Performance Insights */}
        <div className="mt-6 pt-6 border-t border-gray-200">
          <h4 className="text-sm font-medium text-gray-900 mb-3">Performance Insights</h4>
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-500">Peak Traffic Hours</span>
              <span className="font-medium text-gray-900">2:00 PM - 4:00 PM</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-500">Most Active Service</span>
              <span className="font-medium text-gray-900">Recommendation Service</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-500">Average Daily Requests</span>
              <span className="font-medium text-gray-900">{(totalRequests * 24).toLocaleString()}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}