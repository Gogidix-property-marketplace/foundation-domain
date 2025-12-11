const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 9001;

// Middleware
app.use(helmet());
app.use(cors());
app.use(morgan('combined'));
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'OK',
    timestamp: new Date().toISOString(),
    service: 'authentication-service',
    version: '1.0.0'
  });
});

// API Routes
app.get('/api/v1', (req, res) => {
  res.json({
    message: 'Welcome to authentication-service',
    version: '1.0.0'
  });
});

// Start server
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`🚀 ${config.name} is running on port ${PORT}`);
  });
}

module.exports = app;
