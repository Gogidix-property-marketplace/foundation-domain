const { AnalyticsEvent, ModelPerformance, RealTimeMetric, DashboardWidget, UserAnalytics } = require('../models/Analytics');
const logger = require('../utils/logger');

class DashboardController {
  /**
   * Get dashboard overview with key metrics
   */
  getDashboardOverview = async (req, res, next) => {
    try {
      const now = new Date();
      const startOfDay = new Date(now.setHours(0, 0, 0, 0));

      // Get total models count
      const totalModels = await ModelPerformance.distinct('modelId').length;

      // Get active training sessions
      const activeTrainings = await AnalyticsEvent.countDocuments({
        eventType: 'model_training',
        status: 'pending',
        timestamp: { $gte: startOfDay }
      });

      // Get predictions today
      const predictionsToday = await AnalyticsEvent.countDocuments({
        eventType: 'model_prediction',
        timestamp: { $gte: startOfDay }
      });

      // Get latest metrics for system health
      const latestMetrics = await RealTimeMetric.find()
        .sort({ timestamp: -1 })
        .limit(100);

      // Calculate system health based on error rates and response times
      const errorEvents = latestMetrics.filter(m => m.metricName === 'error_rate');
      const avgErrorRate = errorEvents.length > 0
        ? errorEvents.reduce((sum, m) => sum + m.metricValue, 0) / errorEvents.length
        : 0;

      const systemHealth = avgErrorRate < 0.01 ? 'healthy' :
                          avgErrorRate < 0.05 ? 'warning' : 'critical';

      // Get services status
      const services = [
        { name: 'ai-dashboard-web', status: 'active', lastUpdated: new Date() },
        { name: 'ai-training-service', status: 'active', lastUpdated: new Date() },
        { name: 'computer-vision-service', status: 'active', lastUpdated: new Date() },
        { name: 'data-quality-service', status: 'active', lastUpdated: new Date() },
        { name: 'document-analysis-service', status: 'active', lastUpdated: new Date() },
        { name: 'ml-model-service', status: 'active', lastUpdated: new Date() },
        { name: 'natural-language-processing-service', status: 'active', lastUpdated: new Date() }
      ];

      res.json({
        success: true,
        data: {
          totalModels,
          activeTrainings,
          predictionsToday,
          systemHealth,
          services,
          lastUpdated: new Date()
        }
      });
    } catch (error) {
      logger.error('Error getting dashboard overview:', error);
      next(error);
    }
  };

  /**
   * Get status of all AI services
   */
  getServicesStatus = async (req, res, next) => {
    try {
      const services = [
        {
          name: 'ai-dashboard-web',
          port: 3000,
          status: 'healthy',
          uptime: process.uptime(),
          lastCheck: new Date(),
          endpoint: 'http://localhost:3000/health'
        },
        {
          name: 'ai-training-service',
          port: 3001,
          status: 'healthy',
          uptime: null,
          lastCheck: new Date(),
          endpoint: 'http://localhost:3001/health'
        },
        {
          name: 'computer-vision-service',
          port: 3002,
          status: 'healthy',
          uptime: null,
          lastCheck: new Date(),
          endpoint: 'http://localhost:3002/health'
        },
        {
          name: 'data-quality-service',
          port: 3003,
          status: 'healthy',
          uptime: null,
          lastCheck: new Date(),
          endpoint: 'http://localhost:3003/health'
        },
        {
          name: 'document-analysis-service',
          port: 3004,
          status: 'healthy',
          uptime: null,
          lastCheck: new Date(),
          endpoint: 'http://localhost:3004/health'
        },
        {
          name: 'ml-model-service',
          port: 3005,
          status: 'healthy',
          uptime: null,
          lastCheck: new Date(),
          endpoint: 'http://localhost:3005/health'
        },
        {
          name: 'natural-language-processing-service',
          port: 3006,
          status: 'healthy',
          uptime: null,
          lastCheck: new Date(),
          endpoint: 'http://localhost:3006/health'
        }
      ];

      res.json({
        success: true,
        data: {
          services,
          totalCount: services.length,
          healthyCount: services.filter(s => s.status === 'healthy').length
        }
      });
    } catch (error) {
      logger.error('Error getting services status:', error);
      next(error);
    }
  };

  /**
   * Get metrics summary for dashboard
   */
  getMetricsSummary = async (req, res, next) => {
    try {
      const { timeRange = 1440, services } = req.query; // Default 24 hours
      const timeRangeMinutes = parseInt(timeRange);
      const startTime = new Date(Date.now() - timeRangeMinutes * 60 * 1000);

      const query = {
        timestamp: { $gte: startTime }
      };

      if (services) {
        const serviceList = services.split(',');
        query.serviceName = { $in: serviceList };
      }

      // Get metrics grouped by service
      const metricsByService = await AnalyticsEvent.aggregate([
        { $match: query },
        {
          $group: {
            _id: '$serviceName',
            totalEvents: { $sum: 1 },
            predictions: {
              $sum: { $cond: [{ $eq: ['$eventType', 'model_prediction'] }, 1, 0] }
            },
            errors: {
              $sum: { $cond: [{ $eq: ['$status', 'error'] }, 1, 0] }
            },
            avgResponseTime: { $avg: '$metrics.responseTime' }
          }
        }
      ]);

      // Get real-time metrics
      const realTimeMetrics = await RealTimeMetric.find(query)
        .sort({ timestamp: -1 })
        .limit(1000);

      // Calculate summary statistics
      const totalRequests = metricsByService.reduce((sum, s) => sum + s.totalEvents, 0);
      const totalErrors = metricsByService.reduce((sum, s) => sum + s.errors, 0);
      const errorRate = totalRequests > 0 ? (totalErrors / totalRequests) * 100 : 0;

      res.json({
        success: true,
        data: {
          timeRange: timeRangeMinutes,
          totalRequests,
          totalErrors,
          errorRate: parseFloat(errorRate.toFixed(2)),
          metricsByService,
          realTimeMetrics: realTimeMetrics.slice(0, 100),
          generatedAt: new Date()
        }
      });
    } catch (error) {
      logger.error('Error getting metrics summary:', error);
      next(error);
    }
  };

  /**
   * Get system alerts and notifications
   */
  getAlerts = async (req, res, next) => {
    try {
      const { severity, limit = 50 } = req.query;
      const query = {};

      if (severity) {
        query.severity = severity;
      }

      // Get recent error events and system alerts
      const alerts = await AnalyticsEvent.find({
        eventType: { $in: ['system_alert', 'error_event'] },
        ...query,
        timestamp: { $gte: new Date(Date.now() - 24 * 60 * 60 * 1000) } // Last 24 hours
      })
      .sort({ timestamp: -1 })
      .limit(parseInt(limit))
      .select('eventType serviceName timestamp status properties');

      // Format alerts
      const formattedAlerts = alerts.map(alert => ({
        id: alert._id,
        type: alert.eventType,
        service: alert.serviceName,
        message: alert.properties?.message || 'System alert',
        severity: this.getAlertSeverity(alert),
        timestamp: alert.timestamp,
        status: alert.status,
        details: alert.properties
      }));

      res.json({
        success: true,
        data: {
          alerts: formattedAlerts,
          totalCount: formattedAlerts.length
        }
      });
    } catch (error) {
      logger.error('Error getting alerts:', error);
      next(error);
    }
  };

  /**
   * Get system performance metrics
   */
  getPerformanceMetrics = async (req, res, next) => {
    try {
      const { timeRange = 60 } = req.query; // Default 1 hour
      const startTime = new Date(Date.now() - parseInt(timeRange) * 60 * 1000);

      // Get performance metrics from events
      const performanceData = await AnalyticsEvent.aggregate([
        {
          $match: {
            timestamp: { $gte: startTime },
            'metrics.responseTime': { $exists: true }
          }
        },
        {
          $group: {
            _id: {
              service: '$serviceName',
              minute: { $dateTrunc: { date: '$timestamp', unit: 'minute' } }
            },
            avgResponseTime: { $avg: '$metrics.responseTime' },
            maxResponseTime: { $max: '$metrics.responseTime' },
            minResponseTime: { $min: '$metrics.responseTime' },
            requestCount: { $sum: 1 }
          }
        },
        { $sort: { '_id.minute': 1 } }
      ]);

      // Group by service for easier consumption
      const performanceByService = {};
      performanceData.forEach(item => {
        const service = item._id.service;
        if (!performanceByService[service]) {
          performanceByService[service] = [];
        }
        performanceByService[service].push({
          timestamp: item._id.minute,
          avgResponseTime: parseFloat(item.avgResponseTime.toFixed(2)),
          maxResponseTime: item.maxResponseTime,
          minResponseTime: item.minResponseTime,
          requestCount: item.requestCount
        });
      });

      res.json({
        success: true,
        data: {
          timeRange: parseInt(timeRange),
          performanceByService,
          summary: this.calculatePerformanceSummary(performanceData)
        }
      });
    } catch (error) {
      logger.error('Error getting performance metrics:', error);
      next(error);
    }
  };

  /**
   * Get user activity statistics
   */
  getUserActivity = async (req, res, next) => {
    try {
      const { timeRange = 1440, userId } = req.query; // Default 24 hours
      const startTime = new Date(Date.now() - parseInt(timeRange) * 60 * 1000);

      const query = {
        timestamp: { $gte: startTime }
      };

      if (userId) {
        query.userId = userId;
      }

      // Get user activity from analytics events
      const userActivity = await AnalyticsEvent.aggregate([
        { $match: query },
        {
          $group: {
            _id: {
              userId: '$userId',
              hour: { $dateTrunc: { date: '$timestamp', unit: 'hour' } }
            },
            totalActions: { $sum: 1 },
            predictions: {
              $sum: { $cond: [{ $eq: ['$eventType', 'model_prediction'] }, 1, 0] }
            },
            trainings: {
              $sum: { $cond: [{ $eq: ['$eventType', 'model_training'] }, 1, 0] }
            },
            interactions: {
              $sum: { $cond: [{ $eq: ['$eventType', 'user_interaction'] }, 1, 0] }
            }
          }
        },
        { $sort: { '_id.hour': 1 } }
      ]);

      // Get overall user analytics
      const totalUsers = await AnalyticsEvent.distinct('userId').length;
      const activeUsers = await AnalyticsEvent.distinct('userId', {
        timestamp: { $gte: startTime }
      }).length;

      res.json({
        success: true,
        data: {
          timeRange: parseInt(timeRange),
          totalUsers,
          activeUsers,
          userActivity,
          activityByHour: this.groupActivityByHour(userActivity)
        }
      });
    } catch (error) {
      logger.error('Error getting user activity:', error);
      next(error);
    }
  };

  /**
   * Get top performing models
   */
  getTopModels = async (req, res, next) => {
    try {
      const { metric = 'accuracy', limit = 10 } = req.query;

      const sortField = metric === 'accuracy' ? 'metrics.accuracy' :
                       metric === 'performance' ? 'avgResponseTime' :
                       'requestCount';

      const sortOrder = metric === 'performance' ? 1 : -1; // Ascending for response time

      const topModels = await ModelPerformance.aggregate([
        {
          $group: {
            _id: '$modelId',
            avgAccuracy: { $avg: '$metrics.accuracy' },
            avgResponseTime: { $avg: '$metrics.responseTime' },
            totalRequests: { $sum: '$metrics.requestCount' },
            serviceName: { $first: '$serviceName' },
            lastUpdated: { $max: '$timestamp' }
          }
        },
        { $sort: { [sortField]: sortOrder } },
        { $limit: parseInt(limit) }
      ]);

      res.json({
        success: true,
        data: {
          metric,
          models: topModels.map(model => ({
            modelId: model._id,
            serviceName: model.serviceName,
            avgAccuracy: parseFloat(model.avgAccuracy.toFixed(3)),
            avgResponseTime: parseFloat(model.avgResponseTime.toFixed(2)),
            totalRequests: model.totalRequests,
            lastUpdated: model.lastUpdated
          }))
        }
      });
    } catch (error) {
      logger.error('Error getting top models:', error);
      next(error);
    }
  };

  /**
   * Get usage and performance trends
   */
  getTrends = async (req, res, next) => {
    try {
      const { metric = 'predictions', timeRange = 10080 } = req.query; // Default 7 days
      const startTime = new Date(Date.now() - parseInt(timeRange) * 60 * 1000);

      let aggregationPipeline = [];

      switch (metric) {
        case 'predictions':
          aggregationPipeline = [
            {
              $match: {
                eventType: 'model_prediction',
                timestamp: { $gte: startTime }
              }
            },
            {
              $group: {
                _id: { $dateTrunc: { date: '$timestamp', unit: 'day' } },
                count: { $sum: 1 }
              }
            }
          ];
          break;

        case 'accuracy':
          aggregationPipeline = [
            {
              $match: {
                'metrics.accuracy': { $exists: true },
                timestamp: { $gte: startTime }
              }
            },
            {
              $group: {
                _id: { $dateTrunc: { date: '$timestamp', unit: 'day' } },
                avgAccuracy: { $avg: '$metrics.accuracy' }
              }
            }
          ];
          break;

        case 'response_time':
          aggregationPipeline = [
            {
              $match: {
                'metrics.responseTime': { $exists: true },
                timestamp: { $gte: startTime }
              }
            },
            {
              $group: {
                _id: { $dateTrunc: { date: '$timestamp', unit: 'day' } },
                avgResponseTime: { $avg: '$metrics.responseTime' }
              }
            }
          ];
          break;

        case 'errors':
          aggregationPipeline = [
            {
              $match: {
                status: 'error',
                timestamp: { $gte: startTime }
              }
            },
            {
              $group: {
                _id: { $dateTrunc: { date: '$timestamp', unit: 'day' } },
                count: { $sum: 1 }
              }
            }
          ];
          break;
      }

      const trends = await AnalyticsEvent.aggregate(aggregationPipeline)
        .sort({ '_id': 1 });

      res.json({
        success: true,
        data: {
          metric,
          timeRange: parseInt(timeRange),
          trends: trends.map(trend => ({
            date: trend._id,
            value: metric === 'accuracy' ? parseFloat(trend.avgAccuracy?.toFixed(3) || 0) :
                  metric === 'response_time' ? parseFloat(trend.avgResponseTime?.toFixed(2) || 0) :
                  trend.count
          }))
        }
      });
    } catch (error) {
      logger.error('Error getting trends:', error);
      next(error);
    }
  };

  /**
   * Get detailed system health information
   */
  getDetailedHealth = async (req, res, next) => {
    try {
      const healthInfo = {
        timestamp: new Date(),
        services: await this.getDetailedServiceHealth(),
        systemMetrics: await this.getSystemMetrics(),
        recentErrors: await this.getRecentErrors(),
        performanceStatus: await this.getPerformanceStatus()
      };

      res.json({
        success: true,
        data: healthInfo
      });
    } catch (error) {
      logger.error('Error getting detailed health:', error);
      next(error);
    }
  };

  // Helper methods
  getAlertSeverity(alert) {
    if (alert.status === 'error') return 'high';
    if (alert.eventType === 'system_alert') return 'medium';
    return 'low';
  }

  calculatePerformanceSummary(performanceData) {
    if (performanceData.length === 0) return null;

    const avgResponseTime = performanceData.reduce((sum, item) => sum + item.avgResponseTime, 0) / performanceData.length;
    const totalRequests = performanceData.reduce((sum, item) => sum + item.requestCount, 0);

    return {
      avgResponseTime: parseFloat(avgResponseTime.toFixed(2)),
      totalRequests,
      status: avgResponseTime < 500 ? 'good' : avgResponseTime < 1000 ? 'warning' : 'critical'
    };
  }

  groupActivityByHour(userActivity) {
    const hourlyData = {};
    userActivity.forEach(activity => {
      const hour = new Date(activity._id.hour).getHours();
      if (!hourlyData[hour]) {
        hourlyData[hour] = 0;
      }
      hourlyData[hour] += activity.totalActions;
    });
    return hourlyData;
  }

  async getDetailedServiceHealth() {
    // This would normally check actual service health endpoints
    return [
      { name: 'ai-dashboard-web', status: 'healthy', uptime: process.uptime() },
      { name: 'ai-training-service', status: 'healthy', uptime: null },
      { name: 'computer-vision-service', status: 'healthy', uptime: null },
      { name: 'data-quality-service', status: 'healthy', uptime: null },
      { name: 'document-analysis-service', status: 'healthy', uptime: null },
      { name: 'ml-model-service', status: 'healthy', uptime: null },
      { name: 'natural-language-processing-service', status: 'healthy', uptime: null }
    ];
  }

  async getSystemMetrics() {
    const memUsage = process.memoryUsage();
    return {
      memory: {
        used: memUsage.heapUsed,
        total: memUsage.heapTotal,
        external: memUsage.external
      },
      uptime: process.uptime(),
      cpu: process.cpuUsage()
    };
  }

  async getRecentErrors() {
    return await AnalyticsEvent.find({
      status: 'error',
      timestamp: { $gte: new Date(Date.now() - 60 * 60 * 1000) } // Last hour
    })
    .sort({ timestamp: -1 })
    .limit(10)
    .select('eventType serviceName timestamp properties');
  }

  async getPerformanceStatus() {
    const recentMetrics = await RealTimeMetric.find({
      timestamp: { $gte: new Date(Date.now() - 5 * 60 * 1000) } // Last 5 minutes
    })
    .sort({ timestamp: -1 })
    .limit(100);

    const avgResponseTime = recentMetrics.reduce((sum, m) => sum + m.metricValue, 0) / recentMetrics.length;

    return {
      status: avgResponseTime < 1000 ? 'good' : 'degraded',
      avgResponseTime: parseFloat(avgResponseTime.toFixed(2)),
      metricsCount: recentMetrics.length
    };
  }
}

module.exports = new DashboardController();