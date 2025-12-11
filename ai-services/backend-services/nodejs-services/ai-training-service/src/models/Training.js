const mongoose = require('mongoose');

/**
 * Training Job Schema
 * Represents ML model training jobs in the system
 */
const trainingJobSchema = new Schema({
  jobName: {
    type: String,
    required: true,
    trim: true
  },
  jobDescription: {
    type: String,
    trim: true
  },
  jobType: {
    type: String,
    required: true,
    enum: ['supervised_learning', 'unsupervised_learning', 'reinforcement_learning', 'transfer_learning', 'fine_tuning']
  },
  modelType: {
    type: String,
    required: true,
    enum: ['neural_network', 'decision_tree', 'random_forest', 'svm', 'linear_regression', 'logistic_regression', 'transformer', 'cnn', 'rnn', 'lstm', 'gan', 'vae']
  },

  // Training configuration
  config: {
    algorithm: {
      type: String,
      required: true
    },
    hyperparameters: {
      type: Map,
      of: Schema.Types.Mixed,
      default: {}
    },
    optimizer: {
      type: String,
      enum: ['adam', 'sgd', 'rmsprop', 'adagrad', 'adamw']
    },
    learningRate: {
      type: Number,
      min: 0,
      max: 1,
      default: 0.001
    },
    batchSize: {
      type: Number,
      min: 1,
      default: 32
    },
    epochs: {
      type: Number,
      min: 1,
      default: 100
    },
    validationSplit: {
      type: Number,
      min: 0,
      max: 1,
      default: 0.2
    },
    earlyStopping: {
      type: Boolean,
      default: true
    },
    patience: {
      type: Number,
      default: 10
    }
  },

  // Data sources
  dataSources: [{
    name: String,
    type: {
      type: String,
      enum: ['file', 'database', 'api', 'stream']
    },
    source: String,
    format: {
      type: String,
      enum: ['csv', 'json', 'parquet', 'avro', 'image', 'text']
    },
    size: Number, // in bytes
    recordCount: Number,
    lastModified: Date
  }],

  // Training status and progress
  status: {
    type: String,
    required: true,
    enum: ['queued', 'preparing', 'training', 'validating', 'completed', 'failed', 'cancelled', 'paused'],
    default: 'queued'
  },

  progress: {
    currentEpoch: {
      type: Number,
      default: 0
    },
    totalEpochs: {
      type: Number,
      default: 0
    },
    currentStep: {
      type: Number,
      default: 0
    },
    totalSteps: {
      type: Number,
      default: 0
    },
    percentage: {
      type: Number,
      default: 0,
      min: 0,
      max: 100
    },
    estimatedTimeRemaining: Number // in seconds
  },

  // Training metrics
  metrics: {
    trainingLoss: Number,
    validationLoss: Number,
    trainingAccuracy: Number,
    validationAccuracy: Number,
    f1Score: Number,
    precision: Number,
    recall: Number,
    auc: Number,
    customMetrics: {
      type: Map,
      of: Schema.Types.Mixed
    }
  },

  // Resource allocation
  resources: {
    cpuCores: {
      type: Number,
      default: 2
    },
    memoryGB: {
      type: Number,
      default: 8
    },
    gpuCount: {
      type: Number,
      default: 0
    },
    gpuType: {
      type: String,
      enum: ['tesla_v100', 'tesla_t4', 'rtx_3090', 'rtx_4090', 'a100']
    },
    storageGB: {
      type: Number,
      default: 50
    }
  },

  // Timing information
  timings: {
    createdAt: {
      type: Date,
      default: Date.now
    },
    startedAt: Date,
    completedAt: Date,
    lastCheckpointAt: Date,
    estimatedCompletionAt: Date,
    totalDuration: Number // in seconds
  },

  // Model output
  output: {
    modelId: String,
    modelVersion: String,
    modelPath: String,
    modelSize: Number, // in bytes
    checkpoints: [{
      epoch: Number,
      path: String,
      metrics: Map,
      createdAt: Date
    }],
    artifacts: [{
      name: String,
      type: {
        type: String,
        enum: ['model', 'weights', 'config', 'logs', 'plots', 'data']
      },
      path: String,
      size: Number,
      checksum: String
    }]
  },

  // User and project information
  userId: {
    type: String,
    required: true
  },
  projectId: {
    type: String,
    required: true
  },
  tags: [String],

  // Error information
  error: {
    message: String,
    stack: String,
    type: {
      type: String,
      enum: ['data_error', 'memory_error', 'timeout', 'algorithm_error', 'infrastructure_error', 'user_error']
    },
    occurredAt: Date
  },

  // Metadata
  metadata: {
    type: Map,
    of: Schema.Types.Mixed
  }
}, {
  timestamps: true,
  collection: 'training_jobs'
});

// Indexes for efficient querying
trainingJobSchema.index({ userId: 1, createdAt: -1 });
trainingJobSchema.index({ projectId: 1, status: 1 });
trainingJobSchema.index({ status: 1, createdAt: -1 });
trainingJobSchema.index({ jobType: 1, modelType: 1 });
trainingJobSchema.index({ 'output.modelId': 1 });

/**
 * Training Dataset Schema
 * Manages datasets used for training
 */
const datasetSchema = new Schema({
  name: {
    type: String,
    required: true,
    trim: true
  },
  description: {
    type: String,
    trim: true
  },
  type: {
    type: String,
    required: true,
    enum: ['training', 'validation', 'test', 'production']
  },

  // Dataset information
  datasetInfo: {
    recordCount: Number,
    featureCount: Number,
    classCount: Number,
    size: Number, // in bytes
    format: String,
    schema: Map,
    statistics: Map
  },

  // Storage information
  storage: {
    location: String, // S3, HDFS, local path, etc.
    path: String,
    format: {
      type: String,
      enum: ['csv', 'json', 'parquet', 'hdf5', 'tfrecord', 'image_dir', 'text_files']
    },
    compression: {
      type: String,
      enum: ['none', 'gzip', 'zip', 'tar', 'lz4', 'snappy']
    }
  },

  // Data preprocessing
  preprocessing: {
    steps: [{
      type: {
        type: String,
        enum: ['normalize', 'standardize', 'min_max', 'encode', 'impute', 'filter', 'augment']
      },
      parameters: Map,
      order: Number
    }],
    transformedAt: Date
  },

  // Access control
  access: {
    isPublic: {
      type: Boolean,
      default: false
    },
    allowedUsers: [String],
    allowedProjects: [String]
  },

  // Metadata
  tags: [String],
  metadata: {
    type: Map,
    of: Schema.Types.Mixed
  },

  createdBy: {
    type: String,
    required: true
  }
}, {
  timestamps: true,
  collection: 'datasets'
});

/**
 * Training Experiment Schema
 * Groups related training jobs for experiment tracking
 */
const experimentSchema = new Schema({
  name: {
    type: String,
    required: true,
    trim: true
  },
  description: {
    type: String,
    trim: true
  },

  // Experiment configuration
  objective: {
    type: String,
    required: true
  },
  hypothesis: String,
  baselineModelId: String,

  // Jobs in this experiment
  jobIds: [{
    type: Schema.Types.ObjectId,
    ref: 'TrainingJob'
  }],

  // Experiment results
  results: {
    bestModelId: String,
    bestScore: Number,
    bestJobId: {
      type: Schema.Types.ObjectId,
      ref: 'TrainingJob'
    },
    comparisons: [{
      jobA: {
        type: Schema.Types.ObjectId,
        ref: 'TrainingJob'
      },
      jobB: {
        type: Schema.Types.ObjectId,
        ref: 'TrainingJob'
      },
      metric: String,
      winner: String,
      significance: Number
    }]
  },

  // Status
  status: {
    type: String,
    enum: ['active', 'completed', 'failed', 'paused'],
    default: 'active'
  },

  // User and project
  userId: {
    type: String,
    required: true
  },
  projectId: {
    type: String,
    required: true
  },

  // Metadata
  tags: [String],
  metadata: {
    type: Map,
    of: Schema.Types.Mixed
  }
}, {
  timestamps: true,
  collection: 'training_experiments'
});

/**
 * Training Pipeline Schema
 * Defines automated training pipelines
 */
const pipelineSchema = new Schema({
  name: {
    type: String,
    required: true,
    trim: true
  },
  description: {
    type: String,
    trim: true
  },

  // Pipeline definition
  steps: [{
    name: {
      type: String,
      required: true
    },
    type: {
      type: String,
      required: true,
      enum: ['data_ingestion', 'preprocessing', 'feature_engineering', 'model_training', 'validation', 'deployment']
    },
    config: {
      type: Map,
      of: Schema.Types.Mixed
    },
    dependencies: [String], // Step names this step depends on
    condition: String, // Condition for running this step
    retryCount: {
      type: Number,
      default: 0
    },
    timeout: Number // in seconds
  }],

  // Schedule
  schedule: {
    type: {
      type: String,
      enum: ['manual', 'cron', 'interval', 'event']
    },
    cronExpression: String,
    intervalMinutes: Number,
    eventTrigger: String,
    timezone: {
      type: String,
      default: 'UTC'
    }
  },

  // Resources
  resources: {
    maxConcurrentJobs: {
      type: Number,
      default: 1
    },
    defaultCpuCores: Number,
    defaultMemoryGB: Number,
    defaultGpuCount: Number
  },

  // Status
  status: {
    type: String,
    enum: ['active', 'paused', 'disabled'],
    default: 'active'
  },
  lastRunAt: Date,
  nextRunAt: Date,

  // User and project
  userId: {
    type: String,
    required: true
  },
  projectId: {
    type: String,
    required: true
  },

  // Metadata
  tags: [String],
  metadata: {
    type: Map,
    of: Schema.Types.Mixed
  }
}, {
  timestamps: true,
  collection: 'training_pipelines'
});

// Create models
const TrainingJob = mongoose.model('TrainingJob', trainingJobSchema);
const Dataset = mongoose.model('Dataset', datasetSchema);
const Experiment = mongoose.model('Experiment', experimentSchema);
const Pipeline = mongoose.model('Pipeline', pipelineSchema);

module.exports = {
  TrainingJob,
  Dataset,
  Experiment,
  Pipeline
};