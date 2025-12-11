'use client'

import { Bars3Icon, BellIcon, XMarkIcon } from '@heroicons/react/24/outline'
import { useRealtimeUpdates } from '@/store'

interface HeaderProps {
  sidebarOpen: boolean
  setSidebarOpen: (open: boolean) => void
}

export function Header({ sidebarOpen, setSidebarOpen }: HeaderProps) {
  const { isConnected, lastUpdated } = useRealtimeUpdates()

  const formatLastUpdated = (timestamp: string | null) => {
    if (!timestamp) return 'Never'
    return new Date(timestamp).toLocaleTimeString()
  }

  return (
    <header className="bg-white shadow-sm border-b border-gray-200">
      <div className="px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Left side */}
          <div className="flex items-center">
            <button
              type="button"
              className="lg:hidden -m-2.5 p-2.5 text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-md"
              onClick={() => setSidebarOpen(!sidebarOpen)}
            >
              <span className="sr-only">Open sidebar</span>
              {sidebarOpen ? (
                <XMarkIcon className="h-6 w-6" aria-hidden="true" />
              ) : (
                <Bars3Icon className="h-6 w-6" aria-hidden="true" />
              )}
            </button>

            <div className="ml-4 lg:ml-0">
              <h1 className="text-xl font-semibold text-gray-900">AI Services Dashboard</h1>
            </div>
          </div>

          {/* Right side */}
          <div className="flex items-center space-x-4">
            {/* Connection Status */}
            <div className="hidden sm:flex items-center space-x-2">
              <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
              <span className="text-sm text-gray-600">
                {isConnected ? 'Connected' : 'Disconnected'}
              </span>
              {lastUpdated && (
                <span className="text-xs text-gray-500">
                  Updated: {formatLastUpdated(lastUpdated)}
                </span>
              )}
            </div>

            {/* Notifications */}
            <button
              type="button"
              className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md relative"
            >
              <BellIcon className="h-5 w-5" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            </button>

            {/* User menu placeholder */}
            <div className="h-8 w-8 bg-indigo-600 rounded-full flex items-center justify-center">
              <span className="text-white text-sm font-medium">A</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  )
}