const mongoose = require('mongoose');

/**
 * Data Quality Assessment Schema
 * Tracks quality assessments for datasets
 */
const qualityAssessmentSchema = new Schema({
  assessmentId: {
    type: String,
    required: true,
    unique: true
  },
  datasetId: {
    type: String,
    required: true
  },
  datasetName: {
    type: String,
    required: true
  },
  dataSource: {
    type: String,
    required: true,
    enum: ['database', 'file', 'api', 'stream', 'upload']
  },
  recordCount: {
    type: Number,
    required: true
  },

  // Quality dimensions
  dimensions: {
    completeness: {
      score: {
        type: Number,
        min: 0,
        max: 100
      },
      missingFields: [String],
      missingRecords: Number,
      details: Map
    },
    accuracy: {
      score: {
        type: Number,
        min: 0,
        max: 100
      },
      errorRecords: Number,
      errorFields: [String],
      validationErrors: [String],
      details: Map
    },
    consistency: {
      score: {
        type: Number,
        min: 0,
        max: 100
      },
      inconsistencies: [String],
      duplicateRecords: Number,
      formattingIssues: [String],
      details: Map
    },
    timeliness: {
      score: {
        type: Number,
        min: 0,
        max: 100
      },
      lastUpdated: Date,
      staleness: String,
      dataAge: Number,
      details: Map
    },
    validity: {
      score: {
        type: Number,
        min: 0,
        max: 100
      },
      invalidRecords: Number,
      invalidFields: [String],
      formatViolations: [String],
      details: Map
    },
    uniqueness: {
      score: {
        type: Number,
        min: 0,
        max: 100
      },
      duplicates: Number,
      uniqueConstraints: [String],
      duplicateGroups: [{
        fields: [String],
        count: Number,
        examples: [String]
      }],
      details: Map
    }
  },

  // Overall quality score
  overallScore: {
    type: Number,
    min: 0,
    max: 100
  },
  qualityGrade: {
    type: String,
    enum: ['A+', 'A', 'B+', 'B', 'C+', 'C', 'D', 'F'],
    default: 'F'
  },

  // Issues found
  issues: [{
    type: {
      type: String,
      enum: ['missing_data', 'invalid_format', 'duplicate', 'outlier', 'inconsistency', 'staleness']
    },
    severity: {
      type: String,
      enum: ['low', 'medium', 'high', 'critical']
    },
    description: String,
    field: String,
    recordIds: [String],
    sampleValues: [String],
    recommendation: String,
    autoFixable: Boolean
  }],

  // Property-specific data
  propertyData: {
    propertyIds: [String],
    assessmentType: {
      type: String,
      enum: ['properties', 'listings', 'transactions', 'agents', 'customers']
    },
    businessRules: [{
      rule: String,
      status: {
        type: String,
        enum: ['passed', 'failed', 'warning']
      },
      details: String
    }],
    dataLineage: {
      source: String,
      transformation: String,
      destination: String,
      lastSync: Date
    }
  },

  // Processing metadata
  processing: {
    startedAt: {
      type: Date,
      default: Date.now
    },
    completedAt: Date,
    duration: Number,
    algorithm: String,
    version: String,
    rulesApplied: [String]
  },

  userId: String,
  tags: [String],
  notes: String,
  metadata: Map
}, {
  timestamps: true,
  collection: 'quality_assessments'
});

/**
 * Data Quality Rule Schema
 * Defines data quality validation rules
 */
const qualityRuleSchema = new Schema({
  ruleId: {
    type: String,
    required: true,
    unique: true
  },
  name: {
    type: String,
    required: true
  },
  description: String,
  category: {
    type: String,
    required: true,
    enum: ['completeness', 'accuracy', 'consistency', 'timeliness', 'validity', 'uniqueness']
  },

  // Rule definition
  definition: {
    type: {
      type: String,
      enum: ['required', 'unique', 'format', 'range', 'pattern', 'custom', 'reference'],
      required: true
    },
    field: String,
    fields: [String],
    conditions: Map,
    parameters: Map,
    sqlExpression: String,
    customFunction: String
  },

  // Rule configuration
  config: {
    severity: {
      type: String,
      enum: ['low', 'medium', 'high', 'critical'],
      default: 'medium'
    },
    threshold: Number,
    active: {
      type: Boolean,
      default: true
    },
    autoFix: Boolean,
    fixAction: String
  },

  // Execution metrics
  metrics: {
    executions: {
      type: Number,
      default: 0
    },
    failures: {
      type: Number,
      default: 0
    },
    lastExecuted: Date,
    averageExecutionTime: Number,
    successRate: Number
  },

  // Associated datasets
  datasets: [{
    datasetId: String,
    lastApplied: Date,
    result: String
  }],

  createdBy: String,
  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true,
  collection: 'quality_rules'
});

/**
 * Data Quality Issue Schema
 * Tracks individual data quality issues
 */
const qualityIssueSchema = new Schema({
  issueId: {
    type: String,
    required: true,
    unique: true
  },
  assessmentId: {
    type: String,
    required: true
  },
  datasetId: String,
  recordId: String,

  // Issue details
  issueType: {
    type: String,
    required: true,
    enum: ['missing_value', 'invalid_value', 'duplicate', 'outlier', 'format_error', 'inconsistency', 'stale_data']
  },
  severity: {
    type: String,
    required: true,
    enum: ['low', 'medium', 'high', 'critical']
  },
  field: String,
  currentValue: String,
  expectedValue: String,
  description: String,

  // Resolution tracking
  status: {
    type: String,
    enum: ['open', 'in_progress', 'resolved', 'ignored'],
    default: 'open'
  },
  resolution: {
    action: String,
    newValue: String,
    resolvedBy: String,
    resolvedAt: Date,
    notes: String
  },

  // Impact assessment
  impact: {
    affectedRecords: Number,
    downstreamEffects: [String],
    businessImpact: String
  },

  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: Date,
  assignedTo: String
}, {
  timestamps: true,
  collection: 'quality_issues'
});

/**
 * Data Profiling Result Schema
 * Stores data profiling statistics
 */
const profilingResultSchema = new Schema({
  profileId: {
    type: String,
    required: true,
    unique: true
  },
  datasetId: {
    type: String,
    required: true
  },
  datasetName: String,
  profilingDate: {
    type: Date,
    default: Date.now
  },

  // Field-level statistics
  fieldStatistics: [{
    fieldName: String,
    dataType: String,
    count: Number,
    nullCount: Number,
    uniqueCount: Number,
    distinctValues: [String],
    sampleValues: [String],
    minLength: Number,
    maxLength: Number,
    avgLength: Number,
    minValue: Number,
    maxValue: Number,
    avgValue: Number,
    standardDeviation: Number,
    patterns: [{
      pattern: String,
      count: Number,
      examples: [String]
    }],
    outliers: [String],
    distribution: Map
  }],

  // Dataset statistics
  datasetStatistics: {
    totalRecords: Number,
    totalFields: Number,
    completeness: Number,
    duplicateRecords: Number,
    correlations: [{
      field1: String,
      field2: String,
      correlation: Number
    }]
  },

  // Data quality indicators
  indicators: {
    nullRatio: Number,
    duplicateRatio: Number,
    uniqueness: Number,
    consistency: Number,
    validity: Number,
    accuracy: Number
  },

  processedBy: String,
  processingTime: Number
}, {
  timestamps: true,
  collection: 'profiling_results'
});

// Indexes
qualityAssessmentSchema.index({ assessmentId: 1 });
qualityAssessmentSchema.index({ datasetId: 1 });
qualityAssessmentSchema.index({ userId: 1 });
qualityAssessmentSchema.index({ 'processing.completedAt': 1 });
qualityAssessmentSchema.index({ overallScore: 1 });

qualityRuleSchema.index({ ruleId: 1 });
qualityRuleSchema.index({ category: 1 });
qualityRuleSchema.index({ 'config.active': 1 });

qualityIssueSchema.index({ issueId: 1 });
qualityIssueSchema.index({ assessmentId: 1 });
qualityIssueSchema.index({ datasetId: 1 });
qualityIssueSchema.index({ status: 1 });
qualityIssueSchema.index({ severity: 1 });

profilingResultSchema.index({ profileId: 1 });
profilingResultSchema.index({ datasetId: 1 });

// Create models
const QualityAssessment = mongoose.model('QualityAssessment', qualityAssessmentSchema);
const QualityRule = mongoose.model('QualityRule', qualityRuleSchema);
const QualityIssue = mongoose.model('QualityIssue', qualityIssueSchema);
const ProfilingResult = mongoose.model('ProfilingResult', profilingResultSchema);

module.exports = {
  QualityAssessment,
  QualityRule,
  QualityIssue,
  ProfilingResult
};