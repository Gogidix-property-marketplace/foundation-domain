const mongoose = require('mongoose');

/**
 * Image Analysis Schema
 * Stores results from image processing and analysis
 */
const imageAnalysisSchema = new Schema({
  imageId: {
    type: String,
    required: true,
    unique: true
  },
  imageUrl: {
    type: String,
    required: true
  },
  fileName: {
    type: String,
    required: true
  },
  fileSize: {
    type: Number,
    required: true
  },
  mimeType: {
    type: String,
    required: true
  },

  // Analysis results
  analysis: {
    classification: {
      primary: {
        label: String,
        confidence: Number
      },
      secondary: [{
        label: String,
        confidence: Number
      }]
    },
    objects: [{
      id: String,
      label: String,
      confidence: Number,
      bbox: {
        x: Number,
        y: Number,
        width: Number,
        height: Number
      },
      attributes: Map
    }],
    faces: [{
      id: String,
      bbox: {
        x: Number,
        y: Number,
        width: Number,
        height: Number
      },
      confidence: Number,
      attributes: {
        age: Number,
        gender: String,
        emotion: String,
        glasses: Boolean,
        beard: Boolean
      }
    }],
    text: [{
      content: String,
      bbox: {
        x: Number,
        y: Number,
        width: Number,
        height: Number
      },
      confidence: Number
    }],
    colors: {
      dominant: [String],
      palette: [{
        color: String,
        percentage: Number
      }]
    },
    features: {
      edges: Number,
      corners: Number,
      brightness: Number,
      contrast: Number,
      sharpness: Number,
      blur: Number,
      noise: Number
    }
  },

  // Processing metadata
  processing: {
    modelUsed: String,
    modelVersion: String,
    processingTime: Number,
    timestamp: {
      type: Date,
      default: Date.now
    },
    parameters: Map,
    status: {
      type: String,
      enum: ['pending', 'processing', 'completed', 'failed'],
      default: 'pending'
    }
  },

  // Property specific data
  property: {
    propertyId: String,
    propertyType: {
      type: String,
      enum: ['residential', 'commercial', 'industrial', 'land']
    },
    rooms: [{
      type: String,
      count: Number,
      features: [String]
    }],
    amenities: [String],
    condition: {
      type: String,
      enum: ['excellent', 'good', 'fair', 'needs_repair', 'poor']
    },
    estimatedValue: Number,
    area: Number
  },

  // Quality metrics
  quality: {
    resolution: String,
    clarity: {
      type: Number,
      min: 0,
      max: 1
    },
    brightness: {
      type: Number,
      min: 0,
      max: 1
    },
    contrast: {
      type: Number,
      min: 0,
      max: 1
    },
    noise: {
      type: Number,
      min: 0,
      max: 1
    },
    overall: {
      type: Number,
      min: 0,
      max: 5
    }
  },

  userId: String,
  tags: [String],
  metadata: Map
}, {
  timestamps: true,
  collection: 'image_analyses'
});

/**
 * Video Processing Schema
 * Tracks video analysis jobs and results
 */
const videoProcessingSchema = new Schema({
  videoId: {
    type: String,
    required: true,
    unique: true
  },
  videoUrl: {
    type: String,
    required: true
  },
  fileName: {
    type: String,
    required: true
  },
  duration: {
    type: Number,
    required: true
  },
  resolution: {
    width: Number,
    height: Number
  },
  frameRate: Number,
  fileSize: Number,

  // Processing configuration
  config: {
    analysisTypes: [{
      type: String,
      enum: ['object_detection', 'face_recognition', 'scene_classification', 'text_detection', 'motion_analysis']
    }],
    frameInterval: {
      type: Number,
      default: 1
    },
    confidenceThreshold: {
      type: Number,
      default: 0.5
    },
    maxFrames: Number
  },

  // Processing status
  status: {
    type: String,
    enum: ['queued', 'processing', 'completed', 'failed', 'cancelled'],
    default: 'queued'
  },
  progress: {
    currentFrame: Number,
    totalFrames: Number,
    percentage: Number,
    estimatedTimeRemaining: Number
  },

  // Results
  results: {
    summary: {
      totalObjects: Number,
      uniqueObjects: Number,
      totalFaces: Number,
      uniqueFaces: Number,
      scenes: Number,
      framesAnalyzed: Number
    },
    frames: [{
      frameNumber: Number,
      timestamp: Number,
      analysis: {
        objects: [{
          label: String,
          confidence: Number,
          bbox: Object
        }],
        faces: [{
          id: String,
          bbox: Object,
          confidence: Number,
          attributes: Object
        }],
        scene: {
          label: String,
          confidence: Number
        },
        text: [String],
        motion: {
          detected: Boolean,
      intensity: Number
        }
      }
    }],
    objects: [{
      id: String,
      label: String,
      firstSeen: Number,
      lastSeen: Number,
      appearance: Number,
      trackId: String
    }],
    faces: [{
      trackId: String,
      firstSeen: Number,
      lastSeen: Number,
      appearance: Number,
      attributes: Object
    }],
    scenes: [{
      startTime: Number,
      endTime: Number,
      label: String,
      confidence: Number
    }]
  },

  // Processing metadata
  processing: {
    startedAt: Date,
    completedAt: Date,
    duration: Number,
    modelUsed: String,
    modelVersion: String,
    errorMessage: String
  },

  userId: String,
  createdAt: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true,
  collection: 'video_processings'
});

/**
 * Vision Model Schema
 * Manages available computer vision models
 */
const visionModelSchema = new Schema({
  modelId: {
    type: String,
    required: true,
    unique: true
  },
  name: {
    type: String,
    required: true
  },
  type: {
    type: String,
    required: true,
    enum: ['classification', 'object_detection', 'face_recognition', 'ocr', 'scene_analysis', 'custom']
  },
  version: {
    type: String,
    required: true
  },

  // Model configuration
  config: {
    framework: {
      type: String,
      enum: ['tensorflow', 'pytorch', 'onnx', 'custom']
    },
    modelPath: String,
    labelsPath: String,
    inputSize: {
      width: Number,
      height: Number
    },
    preprocessing: {
      normalize: Boolean,
      mean: [Number],
      std: [Number]
    },
    threshold: {
      type: Number,
      default: 0.5
    },
    maxDetections: {
      type: Number,
      default: 100
    }
  },

  // Performance metrics
  performance: {
    accuracy: Number,
    precision: Number,
    recall: Number,
    f1Score: Number,
    inferenceTime: Number,
    memoryUsage: Number
  },

  // Deployment information
  deployment: {
    endpoint: String,
    status: {
      type: String,
      enum: ['active', 'inactive', 'training', 'deprecated'],
      default: 'inactive'
    },
    lastUsed: Date,
    useCount: {
      type: Number,
      default: 0
    }
  },

  capabilities: [{
    type: String,
    enum: ['image', 'video', 'real_time', 'batch']
  }],

  tags: [String],
  description: String,
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
  collection: 'vision_models'
});

// Indexes
imageAnalysisSchema.index({ imageId: 1 });
imageAnalysisSchema.index({ 'processing.status': 1 });
imageAnalysisSchema.index({ userId: 1 });
imageAnalysisSchema.index({ 'property.propertyId': 1 });
imageAnalysisSchema.index({ 'analysis.classification.primary.label': 1 });

videoProcessingSchema.index({ videoId: 1 });
videoProcessingSchema.index({ status: 1 });
videoProcessingSchema.index({ userId: 1 });
videoProcessingSchema.index({ 'processing.startedAt': 1 });

visionModelSchema.index({ modelId: 1 });
visionModelSchema.index({ type: 1 });
visionModelSchema.index({ 'deployment.status': 1 });

// Create models
const ImageAnalysis = mongoose.model('ImageAnalysis', imageAnalysisSchema);
const VideoProcessing = mongoose.model('VideoProcessing', videoProcessingSchema);
const VisionModel = mongoose.model('VisionModel', visionModelSchema);

module.exports = {
  ImageAnalysis,
  VideoProcessing,
  VisionModel
};