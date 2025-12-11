const mongoose = require('mongoose');

const mlModelSchema = new mongoose.Schema({
    modelId: String,
    name: String,
    version: String,
    type: String,
    performance: {
        accuracy: Number,
        precision: Number,
        recall: Number,
        f1Score: Number
    },
    deployment: {
        endpoint: String,
        status: String
    },
    metrics: [{
        timestamp: Date,
        accuracy: Number,
        requests: Number
    }]
});

module.exports = mongoose.model('MLModel', mlModelSchema);
