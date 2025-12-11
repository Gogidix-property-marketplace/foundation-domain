const { TrainingJob, Dataset, Experiment, Pipeline } = require('../models/Training');
const logger = require('../utils/logger');

class TrainingController {
  /**
   * Create a new training job
   */
  createTrainingJob = async (req, res, next) => {
    try {
      const {
        jobName,
        jobDescription,
        jobType,
        modelType,
        config,
        dataSources,
        resources,
        projectId,
        tags,
        metadata
      } = req.body;

      // Create training job
      const trainingJob = new TrainingJob({
        jobName,
        jobDescription,
        jobType,
        modelType,
        config: {
          algorithm: config.algorithm || 'neural_network',
          hyperparameters: config.hyperparameters || {},
          optimizer: config.optimizer || 'adam',
          learningRate: config.learningRate || 0.001,
          batchSize: config.batchSize || 32,
          epochs: config.epochs || 100,
          validationSplit: config.validationSplit || 0.2,
          earlyStopping: config.earlyStopping !== false,
          patience: config.patience || 10
        },
        dataSources: dataSources || [],
        resources: {
          cpuCores: resources?.cpuCores || 2,
          memoryGB: resources?.memoryGB || 8,
          gpuCount: resources?.gpuCount || 0,
          gpuType: resources?.gpuType,
          storageGB: resources?.storageGB || 50
        },
        userId: req.user.id,
        projectId: projectId || 'default',
        tags: tags || [],
        metadata: metadata || {}
      });

      await trainingJob.save();

      // Log job creation
      logger.info('Training job created', {
        jobId: trainingJob._id,
        jobName,
        userId: req.user.id,
        projectId
      });

      // Queue the job for processing
      this.queueTrainingJob(trainingJob._id);

      res.status(201).json({
        success: true,
        data: {
          jobId: trainingJob._id,
          jobName: trainingJob.jobName,
          status: trainingJob.status,
          createdAt: trainingJob.createdAt,
          estimatedCompletionAt: trainingJob.timings.estimatedCompletionAt
        }
      });
    } catch (error) {
      logger.error('Error creating training job:', error);
      next(error);
    }
  };

  /**
   * Get training job details
   */
  getTrainingJob = async (req, res, next) => {
    try {
      const { id } = req.params;

      const trainingJob = await TrainingJob.findById(id)
        .populate('output.modelId', 'name version type')
        .lean();

      if (!trainingJob) {
        return res.status(404).json({
          success: false,
          error: 'Training job not found'
        });
      }

      // Check authorization
      if (trainingJob.userId !== req.user.id && !req.user.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      res.json({
        success: true,
        data: trainingJob
      });
    } catch (error) {
      logger.error('Error getting training job:', error);
      next(error);
    }
  };

  /**
   * List training jobs
   */
  listTrainingJobs = async (req, res, next) => {
    try {
      const {
        status,
        jobType,
        modelType,
        projectId,
        limit = 20,
        offset = 0,
        sortBy = 'createdAt',
        sortOrder = 'desc'
      } = req.query;

      // Build query
      const query = { userId: req.user.id };

      if (status) query.status = status;
      if (jobType) query.jobType = jobType;
      if (modelType) query.modelType = modelType;
      if (projectId) query.projectId = projectId;

      // Build sort
      const sort = {};
      sort[sortBy] = sortOrder === 'desc' ? -1 : 1;

      // Get training jobs
      const trainingJobs = await TrainingJob.find(query)
        .sort(sort)
        .limit(parseInt(limit))
        .skip(parseInt(offset))
        .select('jobName jobType modelType status progress timings createdAt')
        .lean();

      // Get total count
      const totalCount = await TrainingJob.countDocuments(query);

      res.json({
        success: true,
        data: {
          trainingJobs,
          pagination: {
            total: totalCount,
            limit: parseInt(limit),
            offset: parseInt(offset)
          }
        }
      });
    } catch (error) {
      logger.error('Error listing training jobs:', error);
      next(error);
    }
  };

  /**
   * Start a training job
   */
  startTrainingJob = async (req, res, next) => {
    try {
      const { id } = req.params;

      const trainingJob = await TrainingJob.findById(id);

      if (!trainingJob) {
        return res.status(404).json({
          success: false,
          error: 'Training job not found'
        });
      }

      // Check authorization
      if (trainingJob.userId !== req.user.id && !req.user.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      // Check if job can be started
      if (!['queued', 'failed', 'cancelled', 'paused'].includes(trainingJob.status)) {
        return res.status(400).json({
          success: false,
          error: 'Cannot start job in current status'
        });
      }

      // Update job status
      trainingJob.status = 'preparing';
      trainingJob.timings.startedAt = new Date();
      await trainingJob.save();

      // Start training process
      this.startTrainingProcess(trainingJob._id);

      logger.info('Training job started', {
        jobId: trainingJob._id,
        jobName: trainingJob.jobName,
        userId: req.user.id
      });

      res.json({
        success: true,
        data: {
          jobId: trainingJob._id,
          status: trainingJob.status,
          startedAt: trainingJob.timings.startedAt
        }
      });
    } catch (error) {
      logger.error('Error starting training job:', error);
      next(error);
    }
  };

  /**
   * Stop a training job
   */
  stopTrainingJob = async (req, res, next) => {
    try {
      const { id } = req.params;

      const trainingJob = await TrainingJob.findById(id);

      if (!trainingJob) {
        return res.status(404).json({
          success: false,
          error: 'Training job not found'
        });
      }

      // Check authorization
      if (trainingJob.userId !== req.user.id && !req.user.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      // Check if job can be stopped
      if (!['preparing', 'training', 'validating'].includes(trainingJob.status)) {
        return res.status(400).json({
          success: false,
          error: 'Cannot stop job in current status'
        });
      }

      // Update job status
      trainingJob.status = 'cancelled';
      trainingJob.timings.completedAt = new Date();
      await trainingJob.save();

      // Stop training process
      this.stopTrainingProcess(trainingJob._id);

      logger.info('Training job stopped', {
        jobId: trainingJob._id,
        jobName: trainingJob.jobName,
        userId: req.user.id
      });

      res.json({
        success: true,
        data: {
          jobId: trainingJob._id,
          status: trainingJob.status,
          stoppedAt: trainingJob.timings.completedAt
        }
      });
    } catch (error) {
      logger.error('Error stopping training job:', error);
      next(error);
    }
  };

  /**
   * Pause a training job
   */
  pauseTrainingJob = async (req, res, next) => {
    try {
      const { id } = req.params;

      const trainingJob = await TrainingJob.findById(id);

      if (!trainingJob) {
        return res.status(404).json({
          success: false,
          error: 'Training job not found'
        });
      }

      // Check authorization
      if (trainingJob.userId !== req.user.id && !req.user.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      if (trainingJob.status !== 'training') {
        return res.status(400).json({
          success: false,
          error: 'Cannot pause job in current status'
        });
      }

      // Update job status
      trainingJob.status = 'paused';
      await trainingJob.save();

      // Pause training process
      this.pauseTrainingProcess(trainingJob._id);

      res.json({
        success: true,
        data: {
          jobId: trainingJob._id,
          status: trainingJob.status
        }
      });
    } catch (error) {
      logger.error('Error pausing training job:', error);
      next(error);
    }
  };

  /**
   * Resume a training job
   */
  resumeTrainingJob = async (req, res, next) => {
    try {
      const { id } = req.params;

      const trainingJob = await TrainingJob.findById(id);

      if (!trainingJob) {
        return res.status(404).json({
          success: false,
          error: 'Training job not found'
        });
      }

      // Check authorization
      if (trainingJob.userId !== req.user.id && !req.user.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      if (trainingJob.status !== 'paused') {
        return res.status(400).json({
          success: false,
          error: 'Cannot resume job in current status'
        });
      }

      // Update job status
      trainingJob.status = 'training';
      await trainingJob.save();

      // Resume training process
      this.resumeTrainingProcess(trainingJob._id);

      res.json({
        success: true,
        data: {
          jobId: trainingJob._id,
          status: trainingJob.status
        }
      });
    } catch (error) {
      logger.error('Error resuming training job:', error);
      next(error);
    }
  };

  /**
   * Get training job metrics
   */
  getTrainingMetrics = async (req, res, next) => {
    try {
      const { id } = req.params;
      const { metric, timeRange } = req.query;

      const trainingJob = await TrainingJob.findById(id);

      if (!trainingJob) {
        return res.status(404).json({
          success: false,
          error: 'Training job not found'
        });
      }

      // Check authorization
      if (trainingJob.userId !== req.user.id && !req.user.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      // Get metrics from training logs or database
      const metrics = await this.getJobMetrics(id, metric, timeRange);

      res.json({
        success: true,
        data: {
          jobId: id,
          metrics,
          currentMetrics: trainingJob.metrics,
          progress: trainingJob.progress
        }
      });
    } catch (error) {
      logger.error('Error getting training metrics:', error);
      next(error);
    }
  };

  /**
   * Get training job logs
   */
  getTrainingLogs = async (req, res, next) => {
    try {
      const { id } = req.params;
      const { level, limit = 1000, offset = 0 } = req.query;

      const trainingJob = await TrainingJob.findById(id);

      if (!trainingJob) {
        return res.status(404).json({
          success: false,
          error: 'Training job not found'
        });
      }

      // Check authorization
      if (trainingJob.userId !== req.user.id && !req.user.permissions.includes('admin')) {
        return res.status(403).json({
          success: false,
          error: 'Access denied'
        });
      }

      // Get logs from log storage
      const logs = await this.getJobLogs(id, level, parseInt(limit), parseInt(offset));

      res.json({
        success: true,
        data: {
          jobId: id,
          logs,
          total: logs.length
        }
      });
    } catch (error) {
      logger.error('Error getting training logs:', error);
      next(error);
    }
  };

  /**
   * Create a training experiment
   */
  createExperiment = async (req, res, next) => {
    try {
      const {
        name,
        description,
        objective,
        hypothesis,
        baselineModelId,
        projectId,
        tags
      } = req.body;

      const experiment = new Experiment({
        name,
        description,
        objective,
        hypothesis,
        baselineModelId,
        userId: req.user.id,
        projectId: projectId || 'default',
        tags: tags || []
      });

      await experiment.save();

      logger.info('Training experiment created', {
        experimentId: experiment._id,
        name,
        userId: req.user.id
      });

      res.status(201).json({
        success: true,
        data: experiment
      });
    } catch (error) {
      logger.error('Error creating experiment:', error);
      next(error);
    }
  };

  /**
   * Get system-wide training status
   */
  getTrainingStatus = async (req, res, next) => {
    try {
      // Get overall statistics
      const stats = await TrainingJob.aggregate([
        {
          $group: {
            _id: '$status',
            count: { $sum: 1 }
          }
        }
      ]);

      const statusMap = {};
      stats.forEach(stat => {
        statusMap[stat._id] = stat.count;
      });

      // Get active jobs
      const activeJobs = await TrainingJob.find({
        status: { $in: ['preparing', 'training', 'validating'] }
      })
      .select('jobName status progress userId createdAt')
      .limit(10)
      .lean();

      // Get resource utilization
      const resourceUtilization = await this.getResourceUtilization();

      res.json({
        success: true,
        data: {
          statusCounts: statusMap,
          activeJobs,
          resourceUtilization,
          timestamp: new Date()
        }
      });
    } catch (error) {
      logger.error('Error getting training status:', error);
      next(error);
    }
  };

  // Helper methods (would be implemented with actual training infrastructure)

  async queueTrainingJob(jobId) {
    // In a real implementation, this would add the job to a message queue
    // like RabbitMQ, SQS, or Redis queue
    logger.info('Training job queued', { jobId });

    // Simulate queue processing
    setTimeout(() => {
      this.startTrainingProcess(jobId);
    }, 1000);
  }

  async startTrainingProcess(jobId) {
    try {
      const job = await TrainingJob.findById(jobId);
      if (!job) return;

      job.status = 'training';
      job.timings.startedAt = new Date();
      await job.save();

      // Simulate training progress
      this.simulateTrainingProgress(jobId);
    } catch (error) {
      logger.error('Error starting training process:', error);
    }
  }

  async simulateTrainingProgress(jobId) {
    const job = await TrainingJob.findById(jobId);
    if (!job) return;

    const totalEpochs = job.config.epochs;
    let currentEpoch = 0;

    const progressInterval = setInterval(async () => {
      try {
        const currentJob = await TrainingJob.findById(jobId);
        if (!currentJob || currentJob.status !== 'training') {
          clearInterval(progressInterval);
          return;
        }

        currentEpoch++;

        // Update progress
        currentJob.progress.currentEpoch = currentEpoch;
        currentJob.progress.totalEpochs = totalEpochs;
        currentJob.progress.percentage = Math.round((currentEpoch / totalEpochs) * 100);

        // Simulate metrics improvement
        const baseAccuracy = 0.5;
        const improvement = (currentEpoch / totalEpochs) * 0.4;
        currentJob.metrics.trainingAccuracy = baseAccuracy + improvement + (Math.random() * 0.05);
        currentJob.metrics.validationAccuracy = currentJob.metrics.trainingAccuracy - (Math.random() * 0.03);
        currentJob.metrics.trainingLoss = Math.max(0.1, 2.0 * (1 - currentEpoch / totalEpochs));
        currentJob.metrics.validationLoss = currentJob.metrics.trainingLoss + (Math.random() * 0.1);

        await currentJob.save();

        logger.debug('Training progress updated', {
          jobId,
          epoch: currentEpoch,
          accuracy: currentJob.metrics.validationAccuracy
        });

        // Check if training is complete
        if (currentEpoch >= totalEpochs) {
          clearInterval(progressInterval);
          await this.completeTrainingJob(jobId);
        }
      } catch (error) {
        logger.error('Error updating training progress:', error);
        clearInterval(progressInterval);
      }
    }, 2000); // Update every 2 seconds
  }

  async completeTrainingJob(jobId) {
    try {
      const job = await TrainingJob.findById(jobId);
      if (!job) return;

      job.status = 'completed';
      job.timings.completedAt = new Date();
      job.timings.totalDuration = Math.round(
        (job.timings.completedAt - job.timings.startedAt) / 1000
      );

      // Generate model output
      job.output.modelId = `model_${jobId}_${Date.now()}`;
      job.output.modelVersion = '1.0.0';
      job.output.modelPath = `/models/${job.output.modelId}`;
      job.output.modelSize = Math.floor(Math.random() * 100000000) + 1000000; // 1-100MB

      await job.save();

      logger.info('Training job completed', {
        jobId,
        modelId: job.output.modelId,
        duration: job.timings.totalDuration
      });
    } catch (error) {
      logger.error('Error completing training job:', error);
    }
  }

  async stopTrainingProcess(jobId) {
    // In a real implementation, this would send a signal to the training process
    logger.info('Training process stopped', { jobId });
  }

  async pauseTrainingProcess(jobId) {
    // In a real implementation, this would pause the training process
    logger.info('Training process paused', { jobId });
  }

  async resumeTrainingProcess(jobId) {
    // In a real implementation, this would resume the training process
    logger.info('Training process resumed', { jobId });
  }

  async getJobMetrics(jobId, metric, timeRange) {
    // In a real implementation, this would query the metrics database
    // For now, return mock data
    return {
      trainingLoss: [0.8, 0.6, 0.4, 0.3, 0.2],
      validationLoss: [0.9, 0.7, 0.5, 0.4, 0.3],
      trainingAccuracy: [0.6, 0.7, 0.8, 0.85, 0.9],
      validationAccuracy: [0.55, 0.65, 0.75, 0.8, 0.85]
    };
  }

  async getJobLogs(jobId, level, limit, offset) {
    // In a real implementation, this would query the log storage
    // For now, return mock logs
    return [
      {
        timestamp: new Date(),
        level: 'INFO',
        message: 'Training started',
        epoch: 1,
        loss: 0.8,
        accuracy: 0.6
      },
      {
        timestamp: new Date(),
        level: 'INFO',
        message: 'Epoch completed',
        epoch: 1,
        loss: 0.7,
        accuracy: 0.65
      }
    ];
  }

  async getResourceUtilization() {
    // In a real implementation, this would query the resource monitoring system
    return {
      cpu: {
        total: 16,
        used: 8,
        percentage: 50
      },
      memory: {
        total: 64,
        used: 32,
        percentage: 50
      },
      gpu: {
        total: 4,
        used: 2,
        percentage: 50
      },
      activeJobs: 5,
      queuedJobs: 2
    };
  }
}

module.exports = new TrainingController();