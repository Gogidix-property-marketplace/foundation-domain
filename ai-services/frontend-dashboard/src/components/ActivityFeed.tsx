'use client'

import { useStore } from '@/store'
import {
  ServerIcon,
  CheckCircleIcon,
  ExclamationCircleIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline'

export function ActivityFeed() {
  const { services, globalMetrics } = useStore()

  // Generate recent activities based on service data
  const activities = [
    {
      id: 1,
      type: 'service_health',
      message: 'Predictive Analytics Service health check passed',
      timestamp: '2 minutes ago',
      icon: CheckCircleIcon,
      iconColor: 'text-green-500',
      bgColor: 'bg-green-100',
    },
    {
      id: 2,
      type: 'service_health',
      message: 'NLP Processing Service showing degraded performance',
      timestamp: '5 minutes ago',
      icon: ExclamationCircleIcon,
      iconColor: 'text-yellow-500',
      bgColor: 'bg-yellow-100',
    },
    {
      id: 3,
      type: 'metrics',
      message: `${globalMetrics.totalRequests.toLocaleString()} total requests processed today`,
      timestamp: '10 minutes ago',
      icon: InformationCircleIcon,
      iconColor: 'text-blue-500',
      bgColor: 'bg-blue-100',
    },
    {
      id: 4,
      type: 'service_start',
      message: 'AI Training Service started successfully',
      timestamp: '1 hour ago',
      icon: ServerIcon,
      iconColor: 'text-purple-500',
      bgColor: 'bg-purple-100',
    },
    {
      id: 5,
      type: 'service_health',
      message: 'All core services operational',
      timestamp: '2 hours ago',
      icon: CheckCircleIcon,
      iconColor: 'text-green-500',
      bgColor: 'bg-green-100',
    },
  ]

  return (
    <div className="bg-white shadow rounded-lg">
      <div className="px-4 py-5 sm:p-6">
        <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
          Recent Activity
        </h3>
        <div className="flow-root">
          <ul className="-mb-8">
            {activities.map((activity, activityIdx) => (
              <li key={activity.id}>
                <div className="relative pb-8">
                  {activityIdx !== activities.length - 1 ? (
                    <span
                      className="absolute top-5 left-5 -ml-px h-full w-0.5 bg-gray-200"
                      aria-hidden="true"
                    />
                  ) : null}
                  <div className="relative flex items-start space-x-3">
                    <div>
                      <span
                        className={classNames(
                          activity.bgColor,
                          'h-8 w-8 rounded-full flex items-center justify-center ring-8 ring-white'
                        )}
                      >
                        <activity.icon className={`h-4 w-4 ${activity.iconColor}`} aria-hidden="true" />
                      </span>
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="text-sm text-gray-500">
                        <p className="font-medium text-gray-900">{activity.message}</p>
                        <p className="mt-0.5">{activity.timestamp}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}

function classNames(...classes: string[]) {
  return classes.filter(Boolean).join(' ')
}