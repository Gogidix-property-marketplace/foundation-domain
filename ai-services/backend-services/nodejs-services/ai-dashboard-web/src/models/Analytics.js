const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// Analytics Event Schema
const analyticsEventSchema = new Schema({
  eventType: {
    type: String,
    required: true,
    enum: [
      'model_prediction',
      'model_training',
      'user_interaction',
      'system_alert',
      'performance_metric',
      'error_event',
      'data_processing'
    ]
  },
  serviceName: {
    type: String,
    required: true,
    enum: ['dashboard', 'training', 'vision', 'data-quality', 'documents', 'models', 'nlp']
  },
  timestamp: {
    type: Date,
    default: Date.now,
    index: true
  },
  userId: {
    type: String,
    required: false
  },
  sessionId: {
    type: String,
    required: false
  },
  properties: {
    type: Schema.Types.Mixed,
    required: true
  },
  metrics: {
    responseTime: Number,
    throughput: Number,
    accuracy: Number,
    errorRate: Number,
    resourceUsage: {
      cpu: Number,
      memory: Number,
      gpu: Number
    }
  },
  status: {
    type: String,
    enum: ['success', 'error', 'pending'],
    default: 'success'
  },
  metadata: {
    type: Schema.Types.Mixed,
    required: false
  }
}, {
  timestamps: true,
  collection: 'analytics_events'
});

// Model Performance Schema
const modelPerformanceSchema = new Schema({
  modelId: {
    type: String,
    required: true,
    index: true
  },
  modelName: {
    type: String,
    required: true
  },
  version: {
    type: String,
    required: true
  },
  serviceName: {
    type: String,
    required: true
  },
  timestamp: {
    type: Date,
    default: Date.now,
    index: true
  },
  performance: {
    accuracy: {
      type: Number,
      required: true
    },
    precision: {
      type: Number,
      required: true
    },
    recall: {
      type: Number,
      required: true
    },
    f1Score: {
      type: Number,
      required: true
    },
    latency: {
      avg: Number,
      min: Number,
      max: Number,
      p50: Number,
      p95: Number,
      p99: Number
    },
    throughput: {
      requestsPerSecond: Number,
      batchProcessing: Number
    },
    resourceUsage: {
      avgCpuUsage: Number,
      maxMemoryUsage: Number,
      avgGpuUsage: Number
    }
  },
  dataset: {
    name: String,
    size: Number,
    version: String,
    splitRatio: {
      training: Number,
      validation: Number,
      test: Number
    }
  },
  trainingConfig: {
    algorithm: String,
    hyperparameters: Schema.Types.Mixed,
    trainingTime: Number,
    epochs: Number,
    batchSize: Number
  },
  evaluationMetrics: {
    customMetrics: Schema.Types.Mixed,
    confusionMatrix: Schema.Types.Mixed,
    rocAuc: Number
  }
}, {
  timestamps: true,
  collection: 'model_performance'
});

// Real-time Metrics Schema
const realTimeMetricsSchema = new Schema({
  metricName: {
    type: String,
    required: true,
    index: true
  },
  serviceName: {
    type: String,
    required: true
  },
  value: {
    type: Number,
    required: true
  },
  unit: {
    type: String,
    required: true
  },
  timestamp: {
    type: Date,
    default: Date.now,
    index: true
  },
  tags: [String],
  alertThreshold: {
    enabled: {
      type: Boolean,
      default: false
    },
    minValue: Number,
    maxValue: Number
  }
}, {
  timestamps: false,
  collection: 'realtime_metrics'
});

// Dashboard Widget Schema
const dashboardWidgetSchema = new Schema({
  widgetId: {
    type: String,
    required: true,
    unique: true
  },
  title: {
    type: String,
    required: true
  },
  type: {
    type: String,
    required: true,
    enum: [
      'metric',
      'chart',
      'table',
      'gauge',
      'progress',
      'alert',
      'map',
      'text',
      'image'
    ]
  },
  position: {
    x: { type: Number, default: 0 },
    y: { type: Number, default: 0 },
    width: { type: Number, default: 4 },
    height: { type: Number, default: 3 }
  },
  config: {
    type: Schema.Types.Mixed,
    required: true
  },
  dataSource: {
    type: String,
    required: true,
    enum: ['analytics', 'metrics', 'model_performance', 'external_api', 'database']
  },
  refreshInterval: {
    type: Number,
    default: 30000 // 30 seconds
  },
  permissions: {
    roles: [String],
    users: [String]
  },
  isVisible: {
    type: Boolean,
    default: true
  },
  createdBy: {
    type: String,
    required: true
  },
  lastModifiedBy: {
    type: String,
    required: false
  }
}, {
  timestamps: true,
  collection: 'dashboard_widgets'
});

// User Analytics Schema
const userAnalyticsSchema = new Schema({
  userId: {
    type: String,
    required: true,
    index: true
  },
  sessionId: {
    type: String,
    required: true
  },
  timestamp: {
    type: Date,
    default: Date.now,
    index: true
  },
  actions: [{
    type: {
      type: String,
      enum: ['view', 'click', 'submit', 'edit', 'delete', 'download', 'upload']
    },
    target: {
      type: String,
      required: true
    },
    timestamp: {
      type: Date,
      default: Date.now
    },
    properties: Schema.Types.Mixed
  }],
  demographics: {
    userAgent: String,
    ip: String,
    country: String,
    city: String,
    browser: String,
    os: String,
    deviceType: String
  },
  sessionMetrics: {
    duration: Number,
    pageViews: Number,
    interactions: Number,
    conversionFunnel: String,
    bounceRate: Boolean
  }
}, {
  timestamps: false,
  collection: 'user_analytics'
});

// Create compound indexes
analyticsEventSchema.index({ serviceName: 1, timestamp: -1 });
analyticsEventSchema.index({ eventType: 1, timestamp: -1 });
modelPerformanceSchema.index({ modelId: 1, timestamp: -1 });
realTimeMetricsSchema.index({ metricName: 1, timestamp: -1 });
userAnalyticsSchema.index({ userId: 1, timestamp: -1 });

// Static methods
analyticsEventSchema.statics.getEventCount = function(serviceName, eventType, startDate, endDate) {
  return this.countDocuments({
    serviceName,
    eventType,
    timestamp: {
      $gte: startDate,
      $lte: endDate
    }
  });
};

analyticsEventSchema.statics.getAverageResponseTime = function(serviceName, timeWindow) {
  const now = new Date();
  const windowStart = new Date(now.getTime() - timeWindow);

  return this.aggregate([
    {
      $match: {
        serviceName,
        timestamp: { $gte: windowStart },
        'metrics.responseTime': { $exists: true }
      }
    },
    {
      $group: {
        _id: '$serviceName',
        avgResponseTime: { $avg: '$metrics.responseTime' }
      }
    }
  ]);
};

// Instance methods
analyticsEventSchema.methods.toJSON = function() {
  const obj = this.toObject();
  return obj;
};

// Create models
const AnalyticsEvent = mongoose.model('AnalyticsEvent', analyticsEventSchema);
const ModelPerformance = mongoose.model('ModelPerformance', modelPerformanceSchema);
const RealTimeMetric = mongoose.model('RealTimeMetric', realTimeMetricsSchema);
const DashboardWidget = mongoose.model('DashboardWidget', dashboardWidgetSchema);
const UserAnalytics = mongoose.model('UserAnalytics', userAnalyticsSchema);

module.exports = {
  AnalyticsEvent,
  ModelPerformance,
  RealTimeMetric,
  DashboardWidget,
  UserAnalytics
};