'use client'

import React from 'react'
import Link from 'next/link'
import {
  ServerIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline'
import { DashboardStats } from '@/components/DashboardStats'
import { ServiceHealth } from '@/components/ServiceHealth'
import { ActivityFeed } from '@/components/ActivityFeed'
import { QuickActions } from '@/components/QuickActions'
import { MetricsOverview } from '@/components/MetricsOverview'
import { useStore } from '@/store'

export default function Dashboard() {
  const {
    services,
    globalMetrics,
    loading,
    error,
    fetchServices,
    fetchHealth,
    fetchStats,
    refreshAllData
  } = useStore()

  if (loading && services.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading AI Services...</p>
        </div>
      </div>
    )
  }

  if (error && services.length === 0) {
    return (
      <div className="text-center py-12">
        <ExclamationTriangleIcon className="h-16 w-16 text-red-500 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Failed to Load Services</h2>
        <p className="text-gray-600 mb-4">{error}</p>
        <button
          onClick={refreshAllData}
          className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
        >
          <ArrowPathIcon className="h-4 w-4 mr-2" />
          Retry
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">AI Services Dashboard</h1>
          <p className="text-gray-600">Monitor and manage your AI services in real-time</p>
        </div>
        <div className="flex space-x-3">
          <button
            onClick={refreshAllData}
            disabled={loading}
            className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
          >
            <ArrowPathIcon className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
          <Link
            href="/services"
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            <ServerIcon className="h-4 w-4 mr-2" />
            View All Services
          </Link>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="rounded-md bg-red-50 p-4">
          <div className="flex">
            <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">Error</h3>
              <div className="mt-2 text-sm text-red-700">
                <p>{error}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Main Content */}
      <div className="space-y-6">
        {/* Stats Overview */}
        <DashboardStats />

        {/* Two Column Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Service Health */}
          <ServiceHealth />

          {/* Quick Actions */}
          <QuickActions />
        </div>

        {/* Bottom Row */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Metrics Overview */}
          <div className="lg:col-span-2">
            <MetricsOverview />
          </div>

          {/* Activity Feed */}
          <ActivityFeed />
        </div>
      </div>
    </div>
  )
}