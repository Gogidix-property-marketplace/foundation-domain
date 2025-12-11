const mongoose = require('mongoose');

const documentAnalysisSchema = new mongoose.Schema({
    documentId: String,
    fileName: String,
    extractedText: String,
    entities: [{
        type: String,
        value: String,
        confidence: Number
    }],
    classification: String,
    sentiment: {
        score: Number,
        magnitude: Number
    },
    keywords: [String],
    summary: String
});

module.exports = mongoose.model('DocumentAnalysis', documentAnalysisSchema);
