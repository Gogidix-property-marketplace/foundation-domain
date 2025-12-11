'use client'

import { useRealtimeUpdates } from '@/store'
import {
  SignalIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline'

export function ConnectionStatus() {
  const { isConnected, lastUpdated } = useRealtimeUpdates()

  const formatLastUpdated = (timestamp: string | null) => {
    if (!timestamp) return 'Never'
    const date = new Date(timestamp)
    const now = new Date()
    const diffSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)

    if (diffSeconds < 60) return `${diffSeconds}s ago`
    if (diffSeconds < 3600) return `${Math.floor(diffSeconds / 60)}m ago`
    if (diffSeconds < 86400) return `${Math.floor(diffSeconds / 3600)}h ago`
    return date.toLocaleDateString()
  }

  return (
    <div className="fixed bottom-4 right-4 z-50">
      <div className={`
        flex items-center space-x-2 px-3 py-2 rounded-lg shadow-lg text-sm
        ${isConnected
          ? 'bg-green-50 border border-green-200 text-green-800'
          : 'bg-red-50 border border-red-200 text-red-800'
        }
      `}>
        {isConnected ? (
          <>
            <CheckCircleIcon className="h-4 w-4" />
            <span className="font-medium">Connected</span>
          </>
        ) : (
          <>
            <ExclamationTriangleIcon className="h-4 w-4" />
            <span className="font-medium">Disconnected</span>
          </>
        )}

        <span className="text-xs opacity-75">
          • Last updated: {formatLastUpdated(lastUpdated)}
        </span>
      </div>
    </div>
  )
}