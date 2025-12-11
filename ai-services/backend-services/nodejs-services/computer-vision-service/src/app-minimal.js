const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();

// Basic middleware
app.use(cors());
app.use(express.json());

// Vision routes
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        timestamp: new Date().toISOString(),
        service: 'computer-vision-service',
        version: '1.0.0',
        port: process.env.PORT || 3003,
        uptime: process.uptime()
    });
});

app.get('/api/v1/vision/analyze', (req, res) => {
    res.json({
        success: true,
        data: {
            message: 'Image analysis endpoint working',
            features: ['object_detection', 'face_recognition', 'ocr', 'classification'],
            status: 'ready'
        }
    });
});

app.get('/api/v1/vision/models', (req, res) => {
    res.json({
        success: true,
        data: {
            models: [
                { id: 'yolov5', name: 'YOLOv5 Object Detection', type: 'object_detection' },
                { id: 'face_net', name: 'FaceNet Face Recognition', type: 'face_recognition' },
                { id: 'tesseract', name: 'Tesseract OCR', type: 'ocr' }
            ]
        }
    });
});

app.get('/', (req, res) => {
    res.json({
        message: 'Computer Vision Service API',
        version: '1.0.0',
        service: 'computer-vision-service',
        endpoints: {
            health: '/health',
            analyze: '/api/v1/vision/analyze',
            models: '/api/v1/vision/models'
        }
    });
});

// Error handling
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({
        success: false,
        error: 'Internal Server Error',
        timestamp: new Date().toISOString()
    });
});

// Start server
const PORT = process.env.PORT || 3003;
app.listen(PORT, () => {
    console.log(`\n🚀 Computer Vision Service running on http://localhost:${PORT}`);
    console.log(`📊 Health Check: http://localhost:${PORT}/health`);
    console.log(`👁️ Vision API: http://localhost:${PORT}/api/v1/vision/analyze`);
    console.log(`⏰ Started at: ${new Date()}\n`);
});