const { ImageAnalysis, VideoProcessing, VisionModel } = require('../models/Vision');
const logger = require('../utils/logger');
const sharp = require('sharp');
const fs = require('fs').promises;
const path = require('path');

class VisionController {
  /**
   * Analyze an image
   */
  analyzeImage = async (req, res, next) => {
    try {
      const { imageUrl, analysisTypes, modelId, propertyId } = req.body;

      // Create analysis record
      const analysis = new ImageAnalysis({
        imageId: `img_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        imageUrl,
        fileName: path.basename(imageUrl),
        fileSize: req.file?.size || 0,
        mimeType: req.file?.mimetype || 'image/jpeg',
        processing: {
          modelUsed: modelId || 'default',
          modelVersion: '1.0.0',
          status: 'processing'
        },
        userId: req.user?.id,
        property: propertyId ? { propertyId } : undefined
      });

      await analysis.save();

      // Start async processing
      this.processImageAnalysis(analysis._id, req.file?.path || imageUrl, analysisTypes);

      res.status(202).json({
        success: true,
        data: {
          analysisId: analysis._id,
          imageId: analysis.imageId,
          status: 'processing',
          message: 'Image analysis started'
        }
      });
    } catch (error) {
      logger.error('Error in analyzeImage:', error);
      next(error);
    }
  };

  /**
   * Get image analysis results
   */
  getImageAnalysis = async (req, res, next) => {
    try {
      const { id } = req.params;

      const analysis = await ImageAnalysis.findById(id);

      if (!analysis) {
        return res.status(404).json({
          success: false,
          error: 'Analysis not found'
        });
      }

      // Check authorization
      if (analysis.userId !== req.user?.id && !req.user?.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      res.json({
        success: true,
        data: analysis
      });
    } catch (error) {
      logger.error('Error getting image analysis:', error);
      next(error);
    }
  };

  /**
   * Process video
   */
  processVideo = async (req, res, next) => {
    try {
      const { videoUrl, config, propertyId } = req.body;

      const videoProcessing = new VideoProcessing({
        videoId: `vid_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        videoUrl,
        fileName: req.file?.originalname || 'video.mp4',
        duration: 0, // Will be updated during processing
        config: {
          analysisTypes: config?.analysisTypes || ['object_detection'],
          frameInterval: config?.frameInterval || 1,
          confidenceThreshold: config?.confidenceThreshold || 0.5
        },
        userId: req.user?.id
      });

      await videoProcessing.save();

      // Start async processing
      this.processVideoAnalysis(videoProcessing._id, req.file?.path || videoUrl);

      res.status(202).json({
        success: true,
        data: {
          videoId: videoProcessing.videoId,
          processingId: videoProcessing._id,
          status: 'queued',
          message: 'Video processing started'
        }
      });
    } catch (error) {
      logger.error('Error in processVideo:', error);
      next(error);
    }
  };

  /**
   * Get video processing status
   */
  getVideoProcessingStatus = async (req, res, next) => {
    try {
      const { id } = req.params;

      const processing = await VideoProcessing.findById(id);

      if (!processing) {
        return res.status(404).json({
          success: false,
          error: 'Video processing not found'
        });
      }

      // Check authorization
      if (processing.userId !== req.user?.id && !req.user?.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      res.json({
        success: true,
        data: {
          videoId: processing.videoId,
          status: processing.status,
          progress: processing.progress,
          results: processing.status === 'completed' ? processing.results : null
        }
      });
    } catch (error) {
      logger.error('Error getting video processing status:', error);
      next(error);
    }
  };

  /**
   * Detect objects in image
   */
  detectObjects = async (req, res, next) => {
    try {
      const { imageUrl, modelId, confidenceThreshold = 0.5 } = req.body;

      // Load model
      const model = await VisionModel.findOne({
        modelId: modelId || 'yolov5',
        type: 'object_detection',
        'deployment.status': 'active'
      });

      if (!model) {
        return res.status(404).json({
          success: false,
          error: 'Model not found'
        });
      }

      // Process image
      const detections = await this.runObjectDetection(imageUrl, model, confidenceThreshold);

      res.json({
        success: true,
        data: {
          modelId: model.modelId,
          modelName: model.name,
          detections,
          count: detections.length,
          timestamp: new Date()
        }
      });
    } catch (error) {
      logger.error('Error in detectObjects:', error);
      next(error);
    }
  };

  /**
   * Recognize faces in image
   */
  recognizeFaces = async (req, res, next) => {
    try {
      const { imageUrl, modelId, detectAttributes = true } = req.body;

      const model = await VisionModel.findOne({
        modelId: modelId || 'face_net',
        type: 'face_recognition',
        'deployment.status': 'active'
      });

      if (!model) {
        return res.status(404).json({
          success: false,
          error: 'Model not found'
        });
      }

      const faces = await this.runFaceRecognition(imageUrl, model, detectAttributes);

      res.json({
        success: true,
        data: {
          modelId: model.modelId,
          modelName: model.name,
          faces,
          count: faces.length,
          timestamp: new Date()
        }
      });
    } catch (error) {
      logger.error('Error in recognizeFaces:', error);
      next(error);
    }
  };

  /**
   * Extract text from image (OCR)
   */
  extractText = async (req, res, next) => {
    try {
      const { imageUrl, language = 'eng' } = req.body;

      const model = await VisionModel.findOne({
        modelId: 'tesseract',
        type: 'ocr',
        'deployment.status': 'active'
      });

      const textData = await this.runOCR(imageUrl, model, language);

      res.json({
        success: true,
        data: {
          text: textData.text,
          words: textData.words,
          confidence: textData.confidence,
          timestamp: new Date()
        }
      });
    } catch (error) {
      logger.error('Error in extractText:', error);
      next(error);
    }
  };

  /**
   * Get available models
   */
  getAvailableModels = async (req, res, next) => {
    try {
      const { type, status = 'active' } = req.query;

      const query = {};
      if (type) query.type = type;
      if (status) query['deployment.status'] = status;

      const models = await VisionModel.find(query)
        .select('-__v')
        .sort({ 'deployment.useCount': -1 });

      res.json({
        success: true,
        data: {
          models,
          count: models.length
        }
      });
    } catch (error) {
      logger.error('Error getting available models:', error);
      next(error);
    }
  };

  /**
   * Get analytics for vision service
   */
  getAnalytics = async (req, res, next) => {
    try {
      const { timeRange = 24 } = req.query; // Default 24 hours
      const startTime = new Date(Date.now() - timeRange * 60 * 60 * 1000);

      // Get analytics data
      const [imageStats, videoStats, modelStats] = await Promise.all([
        // Image analytics
        ImageAnalysis.aggregate([
          { $match: { 'processing.timestamp': { $gte: startTime } } },
          {
            $group: {
              _id: null,
              totalProcessed: { $sum: 1 },
              completed: {
                $sum: { $cond: [{ $eq: ['$processing.status', 'completed'] }, 1, 0] }
              },
              avgProcessingTime: { $avg: '$processing.processingTime' },
              avgQuality: { $avg: '$quality.overall' }
            }
          }
        ]),
        // Video analytics
        VideoProcessing.aggregate([
          { $match: { createdAt: { $gte: startTime } } },
          {
            $group: {
              _id: null,
              totalProcessed: { $sum: 1 },
              completed: {
                $sum: { $cond: [{ $eq: ['$status', 'completed'] }, 1, 0] }
              },
              avgDuration: { $avg: '$duration' },
              totalFrames: { $sum: '$results.summary.framesAnalyzed' }
            }
          }
        ]),
        // Model analytics
        VisionModel.aggregate([
          { $match: { 'deployment.status': 'active' } },
          {
            $group: {
              _id: '$type',
              count: { $sum: 1 },
              totalUsage: { $sum: '$deployment.useCount' },
              avgAccuracy: { $avg: '$performance.accuracy' }
            }
          }
        ])
      ]);

      // Get recent activities
      const recentActivities = await ImageAnalysis.find({
        'processing.timestamp': { $gte: startTime }
      })
      .sort({ 'processing.timestamp': -1 })
      .limit(10)
      .select('imageId fileName processing.status processing.timestamp userId')
      .lean();

      res.json({
        success: true,
        data: {
          timeRange: parseInt(timeRange),
          imageProcessing: imageStats[0] || {},
          videoProcessing: videoStats[0] || {},
          models: modelStats,
          recentActivities,
          systemHealth: await this.getSystemHealth()
        }
      });
    } catch (error) {
      logger.error('Error getting analytics:', error);
      next(error);
    }
  };

  // Helper methods for image processing
  async processImageAnalysis(analysisId, imagePath, analysisTypes) {
    try {
      const analysis = await ImageAnalysis.findById(analysisId);
      if (!analysis) return;

      analysis.processing.status = 'processing';
      analysis.processing.startedAt = new Date();
      await analysis.save();

      const startTime = Date.now();

      // Mock image processing - in production, integrate with actual ML models
      await new Promise(resolve => setTimeout(resolve, 2000)); // Simulate processing

      const mockResults = {
        classification: {
          primary: {
            label: 'property_interior',
            confidence: 0.92
          },
          secondary: [
            { label: 'living_room', confidence: 0.85 },
            { label: 'modern', confidence: 0.78 }
          ]
        },
        objects: [
          {
            id: 'obj_1',
            label: 'sofa',
            confidence: 0.95,
            bbox: { x: 100, y: 200, width: 300, height: 150 },
            attributes: new Map([['condition', 'good'], ['color', 'blue']])
          },
          {
            id: 'obj_2',
            label: 'table',
            confidence: 0.88,
            bbox: { x: 450, y: 250, width: 200, height: 100 },
            attributes: new Map([['material', 'wood']])
          }
        ],
        colors: {
          dominant: ['#3B82F6', '#1F2937', '#F3F4F6'],
          palette: [
            { color: '#3B82F6', percentage: 35 },
            { color: '#1F2937', percentage: 25 },
            { color: '#F3F4F6', percentage: 40 }
          ]
        },
        quality: {
          resolution: '1920x1080',
          clarity: 0.85,
          brightness: 0.75,
          contrast: 0.80,
          noise: 0.15,
          overall: 4.2
        }
      };

      analysis.analysis = mockResults;
      analysis.processing.status = 'completed';
      analysis.processing.completedAt = new Date();
      analysis.processing.processingTime = Date.now() - startTime;
      await analysis.save();

      logger.info('Image analysis completed', {
        analysisId,
        processingTime: analysis.processing.processingTime
      });
    } catch (error) {
      logger.error('Error processing image analysis:', error);
      const analysis = await ImageAnalysis.findById(analysisId);
      if (analysis) {
        analysis.processing.status = 'failed';
        analysis.processing.errorMessage = error.message;
        await analysis.save();
      }
    }
  }

  async processVideoAnalysis(processingId, videoPath) {
    // Mock video processing
    // In production, integrate with FFmpeg and ML models
  }

  async runObjectDetection(imageUrl, model, threshold) {
    // Mock object detection
    // In production, integrate with YOLO, SSD, or other models
    return [
      {
        class: 'person',
        confidence: 0.92,
        bbox: [100, 100, 200, 300]
      },
      {
        class: 'car',
        confidence: 0.85,
        bbox: [300, 200, 400, 350]
      }
    ];
  }

  async runFaceRecognition(imageUrl, model, detectAttributes) {
    // Mock face recognition
    // In production, integrate with FaceNet, ArcFace, or other models
    return [
      {
        bbox: [150, 150, 250, 350],
        confidence: 0.95,
        attributes: detectAttributes ? {
          age: 35,
          gender: 'female',
          emotion: 'happy'
        } : null
      }
    ];
  }

  async runOCR(imageUrl, model, language) {
    // Mock OCR
    // In production, integrate with Tesseract or other OCR engines
    return {
      text: 'Sample extracted text from image',
      words: [
        { text: 'Sample', bbox: [10, 10, 60, 30], confidence: 0.95 },
        { text: 'extracted', bbox: [70, 10, 140, 30], confidence: 0.92 }
      ],
      confidence: 0.93
    };
  }

  async getSystemHealth() {
    // Mock system health
    return {
      cpu: 45,
      memory: 62,
      gpu: 78,
      activeModels: 5,
      queuedJobs: 2
    };
  }
}

module.exports = new VisionController();