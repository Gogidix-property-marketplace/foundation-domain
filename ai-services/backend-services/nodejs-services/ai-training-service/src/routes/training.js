const express = require('express');
const router = express.Router();
const trainingController = require('../controllers/trainingController');
const { asyncHandler } = require('../middleware/errorHandler');
const auth = require('../middleware/auth');

// Apply authentication to all training routes
router.use(auth.authenticate);

/**
 * @swagger
 * /api/v1/training/jobs:
 *   post:
 *     summary: Create a new training job
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - jobName
 *               - jobType
 *               - modelType
 *             properties:
 *               jobName:
 *                 type: string
 *                 description: Name of the training job
 *               jobDescription:
 *                 type: string
 *                 description: Description of the training job
 *               jobType:
 *                 type: string
 *                 enum: [supervised_learning, unsupervised_learning, reinforcement_learning, transfer_learning, fine_tuning]
 *               modelType:
 *                 type: string
 *                 enum: [neural_network, decision_tree, random_forest, svm, linear_regression, logistic_regression, transformer, cnn, rnn, lstm, gan, vae]
 *               config:
 *                 type: object
 *                 properties:
 *                   algorithm:
 *                     type: string
 *                   hyperparameters:
 *                     type: object
 *                   optimizer:
 *                     type: string
 *                     enum: [adam, sgd, rmsprop, adagrad, adamw]
 *                   learningRate:
 *                     type: number
 *                   batchSize:
 *                     type: integer
 *                   epochs:
 *                     type: integer
 *                   validationSplit:
 *                     type: number
 *               dataSources:
 *                 type: array
 *                 items:
 *                   type: object
 *                   properties:
 *                     name:
 *                       type: string
 *                     type:
 *                       type: string
 *                       enum: [file, database, api, stream]
 *                     source:
 *                       type: string
 *                     format:
 *                       type: string
 *               resources:
 *                 type: object
 *                 properties:
 *                   cpuCores:
 *                     type: integer
 *                   memoryGB:
 *                     type: integer
 *                   gpuCount:
 *                     type: integer
 *                   gpuType:
 *                     type: string
 *               projectId:
 *                 type: string
 *               tags:
 *                 type: array
 *                 items:
 *                   type: string
 *     responses:
 *       201:
 *         description: Training job created successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: object
 *                   properties:
 *                     jobId:
 *                       type: string
 *                     jobName:
 *                       type: string
 *                     status:
 *                       type: string
 *                     createdAt:
 *                       type: string
 *                     estimatedCompletionAt:
 *                       type: string
 */
router.post('/jobs', asyncHandler(trainingController.createTrainingJob));

/**
 * @swagger
 * /api/v1/training/jobs:
 *   get:
 *     summary: List training jobs
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [queued, preparing, training, validating, completed, failed, cancelled, paused]
 *         description: Filter by status
 *       - in: query
 *         name: jobType
 *         schema:
 *           type: string
 *         description: Filter by job type
 *       - in: query
 *         name: modelType
 *         schema:
 *           type: string
 *         description: Filter by model type
 *       - in: query
 *         name: projectId
 *         schema:
 *           type: string
 *         description: Filter by project ID
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 20
 *         description: Number of jobs to return
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Number of jobs to skip
 *       - in: query
 *         name: sortBy
 *         schema:
 *           type: string
 *           default: createdAt
 *         description: Sort field
 *       - in: query
 *         name: sortOrder
 *         schema:
 *           type: string
 *           enum: [asc, desc]
 *           default: desc
 *         description: Sort order
 *     responses:
 *       200:
 *         description: Training jobs retrieved successfully
 */
router.get('/jobs', asyncHandler(trainingController.listTrainingJobs));

/**
 * @swagger
 * /api/v1/training/jobs/{id}:
 *   get:
 *     summary: Get training job details
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Training job ID
 *     responses:
 *       200:
 *         description: Training job details retrieved successfully
 *       404:
 *         description: Training job not found
 */
router.get('/jobs/:id', asyncHandler(trainingController.getTrainingJob));

/**
 * @swagger
 * /api/v1/training/jobs/{id}/start:
 *   post:
 *     summary: Start a training job
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Training job ID
 *     responses:
 *       200:
 *         description: Training job started successfully
 *       404:
 *         description: Training job not found
 *       400:
 *         description: Cannot start job in current status
 */
router.post('/jobs/:id/start', asyncHandler(trainingController.startTrainingJob));

/**
 * @swagger
 * /api/v1/training/jobs/{id}/stop:
 *   post:
 *     summary: Stop a training job
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Training job ID
 *     responses:
 *       200:
 *         description: Training job stopped successfully
 *       404:
 *         description: Training job not found
 *       400:
 *         description: Cannot stop job in current status
 */
router.post('/jobs/:id/stop', asyncHandler(trainingController.stopTrainingJob));

/**
 * @swagger
 * /api/v1/training/jobs/{id}/pause:
 *   post:
 *     summary: Pause a training job
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Training job ID
 *     responses:
 *       200:
 *         description: Training job paused successfully
 *       404:
 *         description: Training job not found
 *       400:
 *         description: Cannot pause job in current status
 */
router.post('/jobs/:id/pause', asyncHandler(trainingController.pauseTrainingJob));

/**
 * @swagger
 * /api/v1/training/jobs/{id}/resume:
 *   post:
 *     summary: Resume a training job
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Training job ID
 *     responses:
 *       200:
 *         description: Training job resumed successfully
 *       404:
 *         description: Training job not found
 *       400:
 *         description: Cannot resume job in current status
 */
router.post('/jobs/:id/resume', asyncHandler(trainingController.resumeTrainingJob));

/**
 * @swagger
 * /api/v1/training/jobs/{id}/metrics:
 *   get:
 *     summary: Get training job metrics
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Training job ID
 *       - in: query
 *         name: metric
 *         schema:
 *           type: string
 *         description: Specific metric to retrieve
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: string
 *         description: Time range for metrics
 *     responses:
 *       200:
 *         description: Training metrics retrieved successfully
 *       404:
 *         description: Training job not found
 */
router.get('/jobs/:id/metrics', asyncHandler(trainingController.getTrainingMetrics));

/**
 * @swagger
 * /api/v1/training/jobs/{id}/logs:
 *   get:
 *     summary: Get training job logs
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Training job ID
 *       - in: query
 *         name: level
 *         schema:
 *           type: string
 *           enum: [DEBUG, INFO, WARN, ERROR]
 *         description: Log level filter
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 1000
 *         description: Number of log entries to return
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Number of log entries to skip
 *     responses:
 *       200:
 *         description: Training logs retrieved successfully
 *       404:
 *         description: Training job not found
 */
router.get('/jobs/:id/logs', asyncHandler(trainingController.getTrainingLogs));

/**
 * @swagger
 * /api/v1/training/experiments:
 *   post:
 *     summary: Create a training experiment
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - name
 *               - objective
 *             properties:
 *               name:
 *                 type: string
 *               description:
 *                 type: string
 *               objective:
 *                 type: string
 *               hypothesis:
 *                 type: string
 *               baselineModelId:
 *                 type: string
 *               projectId:
 *                 type: string
 *               tags:
 *                 type: array
 *                 items:
 *                   type: string
 *     responses:
 *       201:
 *         description: Training experiment created successfully
 */
router.post('/experiments', asyncHandler(trainingController.createExperiment));

/**
 * @swagger
 * /api/v1/training/status:
 *   get:
 *     summary: Get system-wide training status
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Training status retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: object
 *                   properties:
 *                     statusCounts:
 *                       type: object
 *                     activeJobs:
 *                       type: array
 *                     resourceUtilization:
 *                       type: object
 *                     timestamp:
 *                       type: string
 */
router.get('/status', asyncHandler(trainingController.getTrainingStatus));

/**
 * @swagger
 * /api/v1/training/models:
 *   get:
 *     summary: Get available model types and algorithms
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Model types and algorithms retrieved successfully
 */
router.get('/models', asyncHandler(async (req, res) => {
  const modelTypes = {
    neural_network: {
      name: 'Neural Network',
      algorithms: ['mlp', 'cnn', 'rnn', 'lstm', 'transformer'],
      useCases: ['classification', 'regression', 'sequence_modeling', 'computer_vision', 'nlp']
    },
    decision_tree: {
      name: 'Decision Tree',
      algorithms: ['cart', 'id3', 'c4.5'],
      useCases: ['classification', 'regression']
    },
    random_forest: {
      name: 'Random Forest',
      algorithms: ['random_forest', 'extra_trees'],
      useCases: ['classification', 'regression', 'feature_selection']
    },
    svm: {
      name: 'Support Vector Machine',
      algorithms: ['svc', 'svr', 'nu_svc', 'nu_svr'],
      useCases: ['classification', 'regression', 'outlier_detection']
    },
    linear_regression: {
      name: 'Linear Regression',
      algorithms: ['linear', 'ridge', 'lasso', 'elastic_net'],
      useCases: ['regression', 'feature_selection']
    },
    logistic_regression: {
      name: 'Logistic Regression',
      algorithms: ['binary', 'multinomial', 'ordinal'],
      useCases: ['classification']
    },
    transformer: {
      name: 'Transformer',
      algorithms: ['bert', 'gpt', 't5', 'xlnet'],
      useCases: ['nlp', 'sequence_modeling', 'classification', 'generation']
    },
    cnn: {
      name: 'Convolutional Neural Network',
      algorithms: ['resnet', 'vgg', 'inception', 'mobilenet', 'efficientnet'],
      useCases: ['computer_vision', 'image_classification', 'object_detection']
    },
    rnn: {
      name: 'Recurrent Neural Network',
      algorithms: ['simple_rnn', 'gru', 'lstm', 'bilstm'],
      useCases: ['sequence_modeling', 'time_series', 'nlp']
    },
    gan: {
      name: 'Generative Adversarial Network',
      algorithms: ['dcgan', 'wgan', 'cycle_gan', 'pix2pix', 'style_gan'],
      useCases: ['generation', 'image_synthesis', 'data_augmentation']
    }
  };

  res.json({
    success: true,
    data: {
      modelTypes,
      supportedFormats: ['csv', 'json', 'parquet', 'hdf5', 'tfrecord', 'image'],
      optimizers: ['adam', 'sgd', 'rmsprop', 'adagrad', 'adamw'],
      lossFunctions: ['mse', 'cross_entropy', 'binary_crossentropy', 'hinge', 'kld']
    }
  });
}));

/**
 * @swagger
 * /api/v1/training/algorithms:
 *   get:
 *     summary: Get supported training algorithms
 *     tags: [Training]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: modelType
 *         schema:
 *           type: string
 *         description: Filter by model type
 *     responses:
 *       200:
 *         description: Supported algorithms retrieved successfully
 */
router.get('/algorithms', asyncHandler(async (req, res) => {
  const { modelType } = req.query;

  const algorithms = {
    supervised_learning: [
      { name: 'Random Forest', id: 'random_forest', modelTypes: ['decision_tree', 'random_forest'] },
      { name: 'Support Vector Machine', id: 'svm', modelTypes: ['svm'] },
      { name: 'Logistic Regression', id: 'logistic_regression', modelTypes: ['logistic_regression'] },
      { name: 'Neural Network', id: 'neural_network', modelTypes: ['neural_network', 'cnn', 'rnn'] },
      { name: 'Transformer', id: 'transformer', modelTypes: ['transformer'] }
    ],
    unsupervised_learning: [
      { name: 'K-Means Clustering', id: 'kmeans', modelTypes: ['neural_network'] },
      { name: 'PCA', id: 'pca', modelTypes: ['neural_network'] },
      { name: 'Autoencoder', id: 'autoencoder', modelTypes: ['neural_network', 'vae'] },
      { name: 'DBSCAN', id: 'dbscan', modelTypes: ['neural_network'] }
    ],
    reinforcement_learning: [
      { name: 'Deep Q-Network', id: 'dqn', modelTypes: ['neural_network'] },
      { name: 'Policy Gradient', id: 'policy_gradient', modelTypes: ['neural_network'] },
      { name: 'Actor-Critic', id: 'actor_critic', modelTypes: ['neural_network'] },
      { name: 'PPO', id: 'ppo', modelTypes: ['neural_network'] }
    ],
    transfer_learning: [
      { name: 'Fine-tuning', id: 'fine_tuning', modelTypes: ['cnn', 'transformer', 'neural_network'] },
      { name: 'Feature Extraction', id: 'feature_extraction', modelTypes: ['cnn', 'transformer'] },
      { name: 'Domain Adaptation', id: 'domain_adaptation', modelTypes: ['neural_network'] }
    ]
  };

  let result = algorithms;
  if (modelType) {
    // Filter algorithms by model type
    result = {};
    Object.keys(algorithms).forEach(category => {
      result[category] = algorithms[category].filter(algo =>
        algo.modelTypes.includes(modelType)
      );
    });
  }

  res.json({
    success: true,
    data: result
  });
}));

module.exports = router;