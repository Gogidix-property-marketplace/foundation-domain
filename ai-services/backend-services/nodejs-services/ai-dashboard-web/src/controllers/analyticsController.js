const { AnalyticsEvent, ModelPerformance, RealTimeMetric, DashboardWidget } = require('../models/Analytics');
const logger = require('../utils/logger');
const { AppError, asyncHandler } = require('../middleware/errorHandler');

class AnalyticsController {
  /**
   * Get real-time metrics
   * @route GET /api/v1/analytics/metrics
   */
  getRealTimeMetrics = asyncHandler(async (req, res, next) => {
    const { service, metricNames, timeRange } = req.query;

    // Build query
    const query = {};
    if (service) query.serviceName = service;
    if (metricNames) query.metricName = { $in: Array.isArray(metricNames) ? metricNames : [metricNames] };

    // Time range filtering
    const now = new Date();
    let startTime = now;
    if (timeRange) {
      const timeRangeMs = parseInt(timeRange) * 60 * 1000; // Convert minutes to milliseconds
      startTime = new Date(now.getTime() - timeRangeMs);
    }

    query.timestamp = { $gte: startTime };

    // Get metrics
    const metrics = await RealTimeMetric
      .find(query)
      .sort({ timestamp: -1 })
      .limit(1000);

    // Group by metric name
    const groupedMetrics = metrics.reduce((acc, metric) => {
      const name = metric.metricName;
      if (!acc[name]) {
        acc[name] = [];
      }
      acc[name].push(metric);
      return acc;
    }, {});

    // Get latest value for each metric
    const latestMetrics = {};
    Object.keys(groupedMetrics).forEach(name => {
      const sortedMetrics = groupedMetrics[name].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
      latestMetrics[name] = sortedMetrics[0];
    });

    res.status(200).json({
      success: true,
      data: {
        metrics: latestMetrics,
        timestamp: now.toISOString(),
        timeRange
      }
    });
  });

  /**
   * Get analytics summary
   * @route GET /api/v1/analytics/summary
   */
  getAnalyticsSummary = asyncHandler(async (req, res, next) => {
    const { timeRange, groupBy = 'service' } = req.query;
    const now = new Date();
    const startTime = timeRange ? new Date(now.getTime() - parseInt(timeRange) * 60 * 1000) : new Date(now.getTime() - 24 * 60 * 60 * 1000);

    // Get total requests
    const totalRequests = await AnalyticsEvent.countDocuments({
      timestamp: { $gte: startTime }
    });

    // Get requests by service
    const requestsByService = await AnalyticsEvent.aggregate([
      {
        $match: { timestamp: { $gte: startTime } }
      },
      {
        $group: {
          _id: '$serviceName',
          count: { $sum: 1 },
          avgResponseTime: { $avg: '$metrics.responseTime' },
          errorRate: {
            $avg: {
              $cond: [{ $eq: ['$status', 'error'] }, 1, 0]
            }
          }
        }
      }
    ]);

    // Get model performance data
    const modelPerformance = await ModelPerformance
      .find({ timestamp: { $gte: startTime } })
      .sort({ timestamp: -1 })
      .limit(10);

    // Get top events
    const topEvents = await AnalyticsEvent.aggregate([
      {
        $match: { timestamp: { $gte: startTime } }
      },
      {
        $group: {
          _id: '$eventType',
          count: { $sum: 1 }
        }
      },
      { $sort: { count: -1 } },
      { $limit: 10 }
    ]);

    res.status(200).json({
      success: true,
      data: {
        summary: {
          totalRequests,
          timeRange,
          period: {
            start: startTime.toISOString(),
            end: now.toISOString()
          }
        },
        requestsByService,
        modelPerformance,
        topEvents
      }
    });
  });

  /**
   * Get model performance analytics
   * @route GET /api/v1/analytics/models
   */
  getModelPerformance = asyncHandler(async (req, res, next) => {
    const { modelId, serviceName, timeRange, sortBy = 'timestamp' } = req.query;
    const now = new Date();
    const startTime = timeRange ? new Date(now.getTime() - parseInt(timeRange) * 60 * 1000) : new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    // Build query
    const query = { timestamp: { $gte: startTime } };
    if (modelId) query.modelId = modelId;
    if (serviceName) query.serviceName = serviceName;

    // Sort options
    const sortOptions = {};
    switch (sortBy) {
      case 'accuracy':
        sortOptions['performance.accuracy'] = -1;
        break;
      case 'latency':
        sortOptions['performance.latency.avg'] = 1;
        break;
      case 'throughput':
        sortOptions['performance.throughput.requestsPerSecond'] = -1;
        break;
      default:
        sortOptions.timestamp = -1;
    }

    const modelData = await ModelPerformance
      .find(query)
      .sort(sortOptions)
      .limit(50);

    // Calculate averages
    const averages = modelData.reduce((acc, model) => {
      acc.totalAccuracy += model.performance.accuracy;
      acc.totalLatency += model.performance.latency.avg;
      acc.totalThroughput += model.performance.throughput.requestsPerSecond;
      acc.totalF1 += model.performance.f1Score;
      acc.count++;
      return acc;
    }, {
        totalAccuracy: 0,
        totalLatency: 0,
        totalThroughput: 0,
        totalF1: 0,
        count: 0
      });

    const averagesResult = {
      accuracy: acc.count > 0 ? acc.totalAccuracy / acc.count : 0,
      latency: acc.count > 0 ? acc.totalLatency / acc.count : 0,
      throughput: acc.count > 0 ? acc.totalThroughput / acc.count : 0,
      f1Score: acc.count > 0 ? acc.totalF1 / acc.count : 0,
      count: acc.count
    };

    res.status(200).json({
      success: true,
      data: {
        models: modelData,
        averages: averagesResult,
        timeRange,
        period: {
          start: startTime.toISOString(),
          end: now.toISOString()
        }
      }
    });
  });

  /**
   * Get user analytics
   * @route GET /api/v1/analytics/users
   */
  getUserAnalytics = asyncHandler(async (req, res, next) => {
    const { timeRange, userId, limit = 100 } = req.query;
    const now = new Date();
    const startTime = timeRange ? new Date(now.getTime() - parseInt(timeRange) * 60 * 1000) : new Date(now.getTime() - 24 * 60 * 60 * 1000);

    // Build query
    const query = { timestamp: { $gte: startTime } };
    if (userId) query.userId = userId;

    const userAnalytics = await UserAnalytics
      .find(query)
      .sort({ timestamp: -1 })
      .limit(parseInt(limit));

    // Calculate user engagement metrics
    const userMetrics = userAnalytics.reduce((acc, user) => {
      const sessionDuration = user.sessionMetrics ? user.sessionMetrics.duration : 0;
      const pageViews = user.sessionMetrics ? user.sessionMetrics.pageViews : 0;
      const interactions = user.sessionMetrics ? user.sessionMetrics.interactions : 0;

      acc.totalSessions++;
      acc.totalDuration += sessionDuration;
      acc.totalPageViews += pageViews;
      acc.totalInteractions += interactions;

      if (pageViews === 1 && interactions === 0) acc.bounceCount++;

      return acc;
    }, {
      totalSessions: 0,
      totalDuration: 0,
      totalPageViews: 0,
      totalInteractions: 0,
      bounceCount: 0
    });

    const engagementMetrics = {
      totalUsers: new Set(userAnalytics.map(u => u.userId)).size,
      averageSessionDuration: userMetrics.totalSessions > 0 ? userMetrics.totalDuration / userMetrics.totalSessions : 0,
      averagePageViews: userMetrics.totalSessions > 0 ? userMetrics.totalPageViews / userMetrics.totalSessions : 0,
      averageInteractions: userMetrics.totalSessions > 0 ? userMetrics.totalInteractions / userMetrics.totalSessions : 0,
      bounceRate: userMetrics.totalSessions > 0 ? (userMetrics.bounceCount / userMetrics.totalSessions) * 100 : 0
    };

    res.status(200).json({
      success: true,
      data: {
        users: userAnalytics,
        metrics: engagementMetrics,
        timeRange,
        period: {
          start: startTime.toISOString(),
          end: now.toISOString()
        }
      }
    });
  });

  /**
   * Get dashboard widgets
   * @route GET /api/v1/analytics/widgets
   */
  getDashboardWidgets = asyncHandler(async (req, res, next) => {
    const { isVisible } = req.query;

    const query = {};
    if (isVisible !== undefined) {
      query.isVisible = isVisible === 'true';
    }

    const widgets = await DashboardWidget
      .find(query)
      .sort({ position: { x: 1, y: 1 } });

    res.status(200).json({
      success: true,
      data: {
        widgets,
        count: widgets.length
      }
    });
  });

  /**
   * Create dashboard widget
   * @route POST /api/v1/analytics/widgets
   */
  createDashboardWidget = asyncHandler(async (req, res, next) => {
    const widgetData = req.body;

    const widget = new DashboardWidget({
      ...widgetData,
      createdBy: req.user?.id || 'system'
    });

    const savedWidget = await widget.save();

    res.status(201).json({
      success: true,
      data: savedWidget
    });
  });

  /**
   * Update dashboard widget
   * @route PUT /api/v1/analytics/widgets/:id
   */
  updateDashboardWidget = asyncHandler(async (req, res, next) => {
    const { id } = req.params;
    const updateData = req.body;

    const widget = await DashboardWidget.findByIdAndUpdate(
      id,
      { ...updateData, lastModifiedBy: req.user?.id || 'system' },
      { new: true, runValidators: true }
    );

    if (!widget) {
      throw new AppError('Widget not found', 404);
    }

    res.status(200).json({
      success: true,
      data: widget
    });
  });

  /**
   * Delete dashboard widget
   * @route DELETE /api/v1/analytics/widgets/:id
   */
  deleteDashboardWidget = asyncHandler(async (req, res, next) => {
    const { id } = req.params;

    const widget = await DashboardWidget.findByIdAndDelete(id);

    if (!widget) {
      throw new AppError('Widget not found', 404);
    }

    res.status(200).json({
      success: true,
      message: 'Widget deleted successfully'
    });
  });

  /**
   * Export analytics data
   * @route GET /api/v1/analytics/export
   */
  exportAnalytics = asyncHandler(async (req, res, next) => {
    const { format, timeRange, type = 'all' } = req.query;
    const now = new Date();
    const startTime = timeRange ? new Date(now.getTime() - parseInt(timeRange) * 60 * 1000) : new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    let data;

    switch (type) {
      case 'events':
        data = await AnalyticsEvent.find({ timestamp: { $gte: startTime } });
        break;
      case 'performance':
        data = await ModelPerformance.find({ timestamp: { $gte: startTime } });
        break;
      case 'realtime':
        data = await RealTimeMetric.find({ timestamp: { $gte: startTime } });
        break;
      case 'users':
        data = await UserAnalytics.find({ timestamp: { $gte: startTime } });
        break;
      default:
        data = {
          events: await AnalyticsEvent.find({ timestamp: { $gte: startTime } }),
          performance: await ModelPerformance.find({ timestamp: { $gte: startTime } }),
          realtime: await RealTimeMetric.find({ timestamp: { $gte: startTime } }),
          users: await UserAnalytics.find({ timestamp: { $gte: startTime } })
        };
    }

    // Handle different export formats
    switch (format) {
      case 'csv':
        res.setHeader('Content-Type', 'text/csv');
        res.setHeader('Content-Disposition', `attachment; filename=analytics_${Date.now().toISOString().split('T')[0]}.csv`);
        res.send(convertToCSV(data));
        break;
      case 'json':
        res.setHeader('Content-Type', 'application/json');
        res.setHeader('Content-Disposition', `attachment; filename=analytics_${Date.now().toISOString().split('T')[0]}.json`);
        res.json(data);
        break;
      default:
        res.json({
          success: true,
          data
        });
    }
  });
}

function convertToCSV(data) {
  if (Array.isArray(data)) {
    const headers = Object.keys(data[0] || {});
    const csvHeaders = headers.join(',');
    const csvRows = data.map(item =>
      headers.map(header => `"${item[header] || ''}"`).join(',')
    );
    return [csvHeaders, ...csvRows].join('\n');
  }
  return JSON.stringify(data, null, 2);
}

module.exports = new AnalyticsController();