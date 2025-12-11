const { AnalyticsEvent, ModelPerformance, RealTimeMetric } = require('../models/Analytics');
const logger = require('../utils/logger');
const fs = require('fs').promises;
const path = require('path');

class ReportsController {
  constructor() {
    this.reportsDir = path.join(__dirname, '../../reports');
    this.ensureReportsDir();
  }

  async ensureReportsDir() {
    try {
      await fs.access(this.reportsDir);
    } catch {
      await fs.mkdir(this.reportsDir, { recursive: true });
    }
  }

  /**
   * Generate a new report
   */
  generateReport = async (req, res, next) => {
    try {
      const { reportType, timeRange, filters = {}, format = 'json' } = req.body;

      const reportId = `report_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

      // Start report generation in background
      this.generateReportData(reportId, reportType, timeRange, filters, format)
        .catch(error => logger.error('Report generation failed:', error));

      res.status(201).json({
        success: true,
        data: {
          reportId,
          status: 'generating',
          estimatedCompletion: new Date(Date.now() + 60000), // 1 minute estimate
          downloadUrl: `/api/v1/reports/${reportId}/download?format=${format}`
        }
      });
    } catch (error) {
      logger.error('Error generating report:', error);
      next(error);
    }
  };

  /**
   * Get list of available reports
   */
  getReports = async (req, res, next) => {
    try {
      const { status, type, limit = 20, offset = 0 } = req.query;

      // In a real implementation, this would query a reports collection
      // For now, we'll return mock data
      const reports = [
        {
          id: 'report_001',
          name: 'Performance Report - Daily',
          type: 'performance',
          status: 'completed',
          createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000),
          size: '2.3 MB',
          format: 'json'
        },
        {
          id: 'report_002',
          name: 'Usage Analytics - Weekly',
          type: 'usage',
          status: 'completed',
          createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000),
          size: '15.7 MB',
          format: 'csv'
        }
      ];

      // Apply filters
      let filteredReports = reports;
      if (status) {
        filteredReports = filteredReports.filter(r => r.status === status);
      }
      if (type) {
        filteredReports = filteredReports.filter(r => r.type === type);
      }

      // Apply pagination
      const paginatedReports = filteredReports.slice(
        parseInt(offset),
        parseInt(offset) + parseInt(limit)
      );

      res.json({
        success: true,
        data: {
          reports: paginatedReports,
          pagination: {
            total: filteredReports.length,
            limit: parseInt(limit),
            offset: parseInt(offset)
          }
        }
      });
    } catch (error) {
      logger.error('Error getting reports:', error);
      next(error);
    }
  };

  /**
   * Get specific report details
   */
  getReportById = async (req, res, next) => {
    try {
      const { id } = req.params;

      // Mock report data - in real implementation, query database
      const report = {
        id,
        name: 'Performance Report - Daily',
        type: 'performance',
        status: 'completed',
        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000),
        completedAt: new Date(Date.now() - 2 * 60 * 60 * 1000 + 30000),
        size: '2.3 MB',
        format: 'json',
        parameters: {
          timeRange: { start: '2025-11-29T00:00:00Z', end: '2025-11-30T00:00:00Z' },
          filters: { services: ['ml-model-service', 'computer-vision-service'] }
        },
        downloadUrls: {
          json: `/api/v1/reports/${id}/download?format=json`,
          csv: `/api/v1/reports/${id}/download?format=csv`
        }
      };

      if (!report) {
        return res.status(404).json({
          success: false,
          error: 'Report not found'
        });
      }

      res.json({
        success: true,
        data: report
      });
    } catch (error) {
      logger.error('Error getting report by ID:', error);
      next(error);
    }
  };

  /**
   * Download report file
   */
  downloadReport = async (req, res, next) => {
    try {
      const { id } = req.params;
      const { format = 'json' } = req.query;

      // In a real implementation, this would fetch the generated report
      // For now, we'll generate it on-demand
      const reportData = await this.generateReportData(id, 'performance', {
        start: new Date(Date.now() - 24 * 60 * 60 * 1000),
        end: new Date()
      }, {}, format);

      // Set appropriate headers
      const filename = `report_${id}.${format}`;

      if (format === 'csv') {
        res.setHeader('Content-Type', 'text/csv');
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
      } else if (format === 'pdf') {
        res.setHeader('Content-Type', 'application/pdf');
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
      } else {
        res.setHeader('Content-Type', 'application/json');
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
      }

      res.send(reportData);
    } catch (error) {
      logger.error('Error downloading report:', error);
      next(error);
    }
  };

  /**
   * Get scheduled reports
   */
  getScheduledReports = async (req, res, next) => {
    try {
      // Mock scheduled reports data
      const scheduledReports = [
        {
          id: 'scheduled_001',
          name: 'Daily Performance Report',
          type: 'performance',
          schedule: {
            frequency: 'daily',
            time: '09:00',
            enabled: true
          },
          recipients: ['admin@gogidix.com', 'ops@gogidix.com'],
          lastRun: new Date(Date.now() - 24 * 60 * 60 * 1000),
          nextRun: new Date(Date.now() + (24 - new Date().getHours()) * 60 * 60 * 1000)
        },
        {
          id: 'scheduled_002',
          name: 'Weekly Usage Analytics',
          type: 'usage',
          schedule: {
            frequency: 'weekly',
            time: '10:00',
            dayOfWeek: 1, // Monday
            enabled: true
          },
          recipients: ['analytics@gogidix.com'],
          lastRun: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
          nextRun: new Date(Date.now() + (7 - new Date().getDay() + 1) * 24 * 60 * 60 * 1000)
        }
      ];

      res.json({
        success: true,
        data: {
          scheduledReports,
          totalCount: scheduledReports.length
        }
      });
    } catch (error) {
      logger.error('Error getting scheduled reports:', error);
      next(error);
    }
  };

  /**
   * Create a scheduled report
   */
  createScheduledReport = async (req, res, next) => {
    try {
      const { name, reportType, schedule, recipients, format } = req.body;

      const scheduledReportId = `scheduled_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

      const scheduledReport = {
        id: scheduledReportId,
        name,
        type: reportType,
        schedule: {
          frequency: schedule.frequency,
          time: schedule.time,
          dayOfWeek: schedule.dayOfWeek,
          dayOfMonth: schedule.dayOfMonth,
          enabled: true
        },
        recipients,
        format: format || 'json',
        createdAt: new Date(),
        lastRun: null,
        nextRun: this.calculateNextRun(schedule)
      };

      // In a real implementation, save to database
      logger.info('Created scheduled report:', scheduledReport);

      res.status(201).json({
        success: true,
        data: scheduledReport
      });
    } catch (error) {
      logger.error('Error creating scheduled report:', error);
      next(error);
    }
  };

  /**
   * Update scheduled report
   */
  updateScheduledReport = async (req, res, next) => {
    try {
      const { id } = req.params;
      const updates = req.body;

      // In a real implementation, update in database
      logger.info('Updated scheduled report:', { id, updates });

      res.json({
        success: true,
        data: {
          id,
          ...updates,
          updatedAt: new Date()
        }
      });
    } catch (error) {
      logger.error('Error updating scheduled report:', error);
      next(error);
    }
  };

  /**
   * Delete scheduled report
   */
  deleteScheduledReport = async (req, res, next) => {
    try {
      const { id } = req.params;

      // In a real implementation, delete from database
      logger.info('Deleted scheduled report:', id);

      res.json({
        success: true,
        message: 'Scheduled report deleted successfully'
      });
    } catch (error) {
      logger.error('Error deleting scheduled report:', error);
      next(error);
    }
  };

  /**
   * Get report templates
   */
  getReportTemplates = async (req, res, next) => {
    try {
      const templates = [
        {
          id: 'template_performance',
          name: 'Performance Report Template',
          type: 'performance',
          description: 'Daily/weekly/monthly performance metrics for AI models',
          parameters: {
            timeRange: 'required',
            services: 'optional',
            models: 'optional'
          },
          formats: ['json', 'csv', 'pdf']
        },
        {
          id: 'template_usage',
          name: 'Usage Analytics Template',
          type: 'usage',
          description: 'User interaction and API usage analytics',
          parameters: {
            timeRange: 'required',
            users: 'optional',
            endpoints: 'optional'
          },
          formats: ['json', 'csv']
        },
        {
          id: 'template_errors',
          name: 'Error Report Template',
          type: 'errors',
          description: 'System errors and failure analysis',
          parameters: {
            timeRange: 'required',
            severity: 'optional',
            services: 'optional'
          },
          formats: ['json', 'csv']
        },
        {
          id: 'template_model_comparison',
          name: 'Model Comparison Template',
          type: 'model_comparison',
          description: 'Compare performance across different AI models',
          parameters: {
            models: 'required',
            timeRange: 'required',
            metrics: 'optional'
          },
          formats: ['json', 'pdf']
        }
      ];

      res.json({
        success: true,
        data: {
          templates,
          totalCount: templates.length
        }
      });
    } catch (error) {
      logger.error('Error getting report templates:', error);
      next(error);
    }
  };

  /**
   * Share report with users
   */
  shareReport = async (req, res, next) => {
    try {
      const { id } = req.params;
      const { emails, message, permissions = 'view' } = req.body;

      // In a real implementation, create sharing links and send notifications
      const shareData = {
        reportId: id,
        sharedBy: req.user?.id || 'system',
        sharedWith: emails,
        permissions,
        message,
        shareLink: `https://gogidix.com/reports/${id}/shared/${this.generateShareToken()}`,
        sharedAt: new Date(),
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7 days
      };

      logger.info('Report shared:', shareData);

      // Send notification emails (mock)
      for (const email of emails) {
        logger.info(`Sharing report ${id} with ${email}`);
      }

      res.json({
        success: true,
        data: {
          message: 'Report shared successfully',
          shareLink: shareData.shareLink,
          sharedWith: emails
        }
      });
    } catch (error) {
      logger.error('Error sharing report:', error);
      next(error);
    }
  };

  // Helper methods
  async generateReportData(reportId, reportType, timeRange, filters, format) {
    const startTime = new Date(timeRange.start);
    const endTime = new Date(timeRange.end);

    let data = {};

    switch (reportType) {
      case 'performance':
        data = await this.generatePerformanceReport(startTime, endTime, filters);
        break;
      case 'usage':
        data = await this.generateUsageReport(startTime, endTime, filters);
        break;
      case 'errors':
        data = await this.generateErrorReport(startTime, endTime, filters);
        break;
      case 'model_comparison':
        data = await this.generateModelComparisonReport(startTime, endTime, filters);
        break;
      case 'service_health':
        data = await this.generateServiceHealthReport(startTime, endTime, filters);
        break;
      default:
        throw new Error(`Unknown report type: ${reportType}`);
    }

    if (format === 'csv') {
      return this.convertToCSV(data);
    } else if (format === 'pdf') {
      return this.convertToPDF(data);
    }

    return JSON.stringify(data, null, 2);
  }

  async generatePerformanceReport(startTime, endTime, filters) {
    const query = {
      timestamp: { $gte: startTime, $lte: endTime },
      ...filters
    };

    const performanceData = await ModelPerformance.find(query)
      .populate('modelId')
      .sort({ timestamp: -1 });

    return {
      reportType: 'performance',
      timeRange: { start: startTime, end: endTime },
      summary: {
        totalModels: performanceData.length,
        avgAccuracy: this.calculateAverage(performanceData, 'metrics.accuracy'),
        avgResponseTime: this.calculateAverage(performanceData, 'metrics.responseTime'),
        totalRequests: this.calculateSum(performanceData, 'metrics.requestCount')
      },
      models: performanceData
    };
  }

  async generateUsageReport(startTime, endTime, filters) {
    const query = {
      timestamp: { $gte: startTime, $lte: endTime },
      ...filters
    };

    const usageData = await AnalyticsEvent.find(query)
      .sort({ timestamp: -1 });

    return {
      reportType: 'usage',
      timeRange: { start: startTime, end: endTime },
      summary: {
        totalEvents: usageData.length,
        uniqueUsers: new Set(usageData.map(e => e.userId)).size,
        topServices: this.getTopServices(usageData),
        eventTypes: this.getEventTypeBreakdown(usageData)
      },
      events: usageData
    };
  }

  async generateErrorReport(startTime, endTime, filters) {
    const query = {
      timestamp: { $gte: startTime, $lte: endTime },
      status: 'error',
      ...filters
    };

    const errorData = await AnalyticsEvent.find(query)
      .sort({ timestamp: -1 });

    return {
      reportType: 'errors',
      timeRange: { start: startTime, end: endTime },
      summary: {
        totalErrors: errorData.length,
        errorRate: (errorData.length / await AnalyticsEvent.countDocuments({
          timestamp: { $gte: startTime, $lte: endTime }
        })) * 100,
        topErrorTypes: this.getTopErrorTypes(errorData),
        servicesWithErrors: this.getServicesWithErrors(errorData)
      },
      errors: errorData
    };
  }

  async generateModelComparisonReport(startTime, endTime, filters) {
    const { models } = filters;

    const comparisonData = await ModelPerformance.find({
      modelId: { $in: models },
      timestamp: { $gte: startTime, $lte: endTime }
    }).sort({ timestamp: -1 });

    return {
      reportType: 'model_comparison',
      timeRange: { start: startTime, end: endTime },
      models: models,
      comparison: this.compareModels(comparisonData)
    };
  }

  async generateServiceHealthReport(startTime, endTime, filters) {
    // Mock service health data
    return {
      reportType: 'service_health',
      timeRange: { start: startTime, end: endTime },
      services: [
        {
          name: 'ai-dashboard-web',
          uptime: '99.9%',
          avgResponseTime: 120,
          errorRate: 0.1
        },
        {
          name: 'ml-model-service',
          uptime: '99.5%',
          avgResponseTime: 250,
          errorRate: 0.5
        }
      ]
    };
  }

  convertToCSV(data) {
    // Simple CSV conversion - would be more sophisticated in production
    if (data.models) {
      const headers = ['Model ID', 'Accuracy', 'Response Time', 'Requests', 'Timestamp'];
      const rows = data.models.map(model => [
        model.modelId,
        model.metrics.accuracy,
        model.metrics.responseTime,
        model.metrics.requestCount,
        model.timestamp
      ]);
      return [headers, ...rows].map(row => row.join(',')).join('\n');
    }
    return JSON.stringify(data, null, 2);
  }

  convertToPDF(data) {
    // PDF generation would require a library like puppeteer or jsPDF
    // For now, return JSON
    return JSON.stringify(data, null, 2);
  }

  calculateAverage(data, path) {
    if (data.length === 0) return 0;
    const sum = data.reduce((acc, item) => acc + this.getNestedValue(item, path), 0);
    return (sum / data.length).toFixed(3);
  }

  calculateSum(data, path) {
    return data.reduce((acc, item) => acc + this.getNestedValue(item, path), 0);
  }

  getNestedValue(obj, path) {
    return path.split('.').reduce((current, key) => current?.[key], obj) || 0;
  }

  getTopServices(data) {
    const serviceCount = {};
    data.forEach(event => {
      serviceCount[event.serviceName] = (serviceCount[event.serviceName] || 0) + 1;
    });
    return Object.entries(serviceCount)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 5);
  }

  getEventTypeBreakdown(data) {
    const typeCount = {};
    data.forEach(event => {
      typeCount[event.eventType] = (typeCount[event.eventType] || 0) + 1;
    });
    return typeCount;
  }

  getTopErrorTypes(data) {
    const errorTypes = {};
    data.forEach(error => {
      errorTypes[error.properties?.errorType || 'unknown'] =
        (errorTypes[error.properties?.errorType || 'unknown'] || 0) + 1;
    });
    return Object.entries(errorTypes)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 5);
  }

  getServicesWithErrors(data) {
    return [...new Set(data.map(error => error.serviceName))];
  }

  compareModels(data) {
    const modelStats = {};
    data.forEach(item => {
      if (!modelStats[item.modelId]) {
        modelStats[item.modelId] = { accuracies: [], responseTimes: [], requests: 0 };
      }
      if (item.metrics.accuracy) modelStats[item.modelId].accuracies.push(item.metrics.accuracy);
      if (item.metrics.responseTime) modelStats[item.modelId].responseTimes.push(item.metrics.responseTime);
      modelStats[item.modelId].requests += item.metrics.requestCount || 0;
    });

    return Object.entries(modelStats).map(([modelId, stats]) => ({
      modelId,
      avgAccuracy: stats.accuracies.reduce((a, b) => a + b, 0) / stats.accuracies.length,
      avgResponseTime: stats.responseTimes.reduce((a, b) => a + b, 0) / stats.responseTimes.length,
      totalRequests: stats.requests
    }));
  }

  calculateNextRun(schedule) {
    const now = new Date();
    let nextRun = new Date(now);

    switch (schedule.frequency) {
      case 'daily':
        const [hours, minutes] = schedule.time.split(':').map(Number);
        nextRun.setHours(hours, minutes, 0, 0);
        if (nextRun <= now) {
          nextRun.setDate(nextRun.getDate() + 1);
        }
        break;
      case 'weekly':
        const [weekHours, weekMinutes] = schedule.time.split(':').map(Number);
        nextRun.setHours(weekHours, weekMinutes, 0, 0);
        const daysUntilNext = ((schedule.dayOfWeek - now.getDay() + 7) % 7) || 7;
        nextRun.setDate(nextRun.getDate() + daysUntilNext);
        break;
      case 'monthly':
        const [monthHours, monthMinutes] = schedule.time.split(':').map(Number);
        nextRun.setHours(monthHours, monthMinutes, 0, 0);
        nextRun.setDate(schedule.dayOfMonth);
        if (nextRun <= now) {
          nextRun.setMonth(nextRun.getMonth() + 1);
        }
        break;
    }

    return nextRun;
  }

  generateShareToken() {
    return Math.random().toString(36).substr(2) + Date.now().toString(36);
  }
}

module.exports = new ReportsController();