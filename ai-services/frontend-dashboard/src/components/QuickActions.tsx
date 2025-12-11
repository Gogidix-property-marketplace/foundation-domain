'use client'

import { useState } from 'react'
import { useStore } from '@/store'
import {
  PlayIcon,
  PauseIcon,
  ArrowPathIcon,
  DocumentTextIcon,
  CogIcon,
  ChartBarIcon
} from '@heroicons/react/24/outline'

export function QuickActions() {
  const [isRefreshing, setIsRefreshing] = useState(false)
  const { fetchServices, fetchHealth, fetchStats } = useStore()

  const handleRefresh = async () => {
    setIsRefreshing(true)
    try {
      await Promise.all([
        fetchServices(),
        fetchHealth(),
        fetchStats()
      ])
    } catch (error) {
      console.error('Failed to refresh data:', error)
    } finally {
      setIsRefreshing(false)
    }
  }

  const actions = [
    {
      label: 'Refresh Services',
      description: 'Update service status and metrics',
      icon: ArrowPathIcon,
      action: handleRefresh,
      disabled: isRefreshing,
      color: 'text-blue-600 hover:bg-blue-50',
    },
    {
      label: 'View Logs',
      description: 'Access service logs and events',
      icon: DocumentTextIcon,
      action: () => console.log('Navigate to logs'),
      disabled: false,
      color: 'text-gray-700 hover:bg-gray-50',
    },
    {
      label: 'API Documentation',
      description: 'Open API documentation',
      icon: ChartBarIcon,
      action: () => window.open('http://localhost:3002/swagger', '_blank'),
      disabled: false,
      color: 'text-purple-600 hover:bg-purple-50',
    },
    {
      label: 'Settings',
      description: 'Configure dashboard settings',
      icon: CogIcon,
      action: () => console.log('Open settings'),
      disabled: false,
      color: 'text-gray-700 hover:bg-gray-50',
    },
  ]

  return (
    <div className="bg-white shadow rounded-lg">
      <div className="px-4 py-5 sm:p-6">
        <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
          Quick Actions
        </h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {actions.map((action) => (
            <button
              key={action.label}
              onClick={action.action}
              disabled={action.disabled}
              className={classNames(
                'relative block w-full p-4 border-2 border-dashed border-gray-300 rounded-lg text-left hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors',
                action.disabled && 'opacity-50 cursor-not-allowed'
              )}
            >
              <div className="flex items-center space-x-3">
                <action.icon className={`h-6 w-6 ${action.disabled ? 'text-gray-400' : action.color.split(' ')[0]}`} />
                <div>
                  <p className={`text-sm font-medium ${action.disabled ? 'text-gray-400' : 'text-gray-900'}`}>
                    {action.label}
                  </p>
                  <p className={`text-xs ${action.disabled ? 'text-gray-400' : 'text-gray-500'}`}>
                    {action.description}
                  </p>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

function classNames(...classes: string[]) {
  return classes.filter(Boolean).join(' ')
}