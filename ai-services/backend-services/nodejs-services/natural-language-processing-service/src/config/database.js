const mongoose = require('mongoose');
const logger = require('../utils/logger');

let connection = null;

const connectDatabase = async () => {
  try {
    if (connection) {
      logger.info('Database already connected');
      return connection;
    }

    const mongoUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/natural-language-processing-service';
    const options = process.env.MONGODB_OPTIONS ?
      JSON.parse(process.env.MONGODB_OPTIONS) : {
        useNewUrlParser: true,
        useUnifiedTopology: true,
        maxPoolSize: 10,
        serverSelectionTimeoutMS: 5000,
        socketTimeoutMS: 45000,
        bufferCommands: false,
        bufferMaxEntries: 0
      };

    connection = await mongoose.connect(mongoUri, options);

    mongoose.connection.on('connected', () => {
      logger.info('Connected to MongoDB database');
    });

    mongoose.connection.on('error', (err) => {
      logger.error('MongoDB connection error:', err);
    });

    mongoose.connection.on('disconnected', () => {
      logger.warn('Disconnected from MongoDB database');
    });

    // Graceful shutdown
    process.on('SIGINT', async () => {
      await mongoose.connection.close();
      logger.info('MongoDB connection closed through app termination');
      process.exit(0);
    });

    logger.info(`Database connected successfully to: ${mongoUri}`);
    return connection;
  } catch (error) {
    logger.error('Database connection failed:', error);
    throw error;
  }
};

const checkConnection = async () => {
  try {
    if (mongoose.connection.readyState === 1) {
      return true;
    }
    await connectDatabase();
    return true;
  } catch (error) {
    logger.error('Database connection check failed:', error);
    return false;
  }
};

const disconnectDatabase = async () => {
  try {
    if (connection) {
      await mongoose.connection.close();
      connection = null;
      logger.info('Database disconnected successfully');
    }
  } catch (error) {
    logger.error('Error disconnecting from database:', error);
    throw error;
  }
};

module.exports = {
  connectDatabase,
  checkConnection,
  disconnectDatabase,
  mongoose
};
