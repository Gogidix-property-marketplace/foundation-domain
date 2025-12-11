const mongoose = require('mongoose');
const logger = require('../utils/logger');

const mongoUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/ai-gateway';
const options = {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  maxPoolSize: 10,
  serverSelectionTimeoutMS: 5000,
  socketTimeoutMS: 45000,
};

// MongoDB connection instance
let dbConnection = null;

/**
 * Connect to MongoDB database
 * @returns {Promise<void>}
 */
async function connectDatabase() {
  try {
    if (dbConnection) {
      logger.info('Database already connected');
      return;
    }

    dbConnection = await mongoose.connect(mongoUri, options);
    logger.info('Connected to MongoDB successfully');

    // Handle connection events
    mongoose.connection.on('error', (error) => {
      logger.error('MongoDB connection error:', error);
    });

    mongoose.connection.on('disconnected', () => {
      logger.warn('MongoDB disconnected');
    });

    mongoose.connection.on('reconnected', () => {
      logger.info('MongoDB reconnected');
    });

  } catch (error) {
    logger.error('Failed to connect to MongoDB:', error);
    throw error;
  }
}

/**
 * Disconnect from MongoDB database
 * @returns {Promise<void>}
 */
async function disconnectDatabase() {
  try {
    if (dbConnection) {
      await mongoose.disconnect();
      dbConnection = null;
      logger.info('Disconnected from MongoDB');
    }
  } catch (error) {
    logger.error('Error disconnecting from MongoDB:', error);
    throw error;
  }
}

/**
 * Check database connection status
 * @returns {Promise<boolean>} Connection status
 */
async function checkConnection() {
  try {
    if (!dbConnection) {
      return false;
    }

    // Ping the database
    await mongoose.connection.db.admin().ping();
    return true;
  } catch (error) {
    logger.error('Database connection check failed:', error);
    return false;
  }
}

/**
 * Get database statistics
 * @returns {Promise<Object>} Database statistics
 */
async function getDatabaseStats() {
  try {
    const stats = await mongoose.connection.db.stats();
    return {
      collections: stats.collections,
      documents: stats.objects,
      avgObjSize: stats.avgObjSize,
      dataSize: stats.dataSize,
      storageSize: stats.storageSize,
      indexes: stats.indexes,
      indexSize: stats.indexSize
    };
  } catch (error) {
    logger.error('Failed to get database stats:', error);
    throw error;
  }
}

module.exports = {
  connectDatabase,
  disconnectDatabase,
  checkConnection,
  getDatabaseStats
};