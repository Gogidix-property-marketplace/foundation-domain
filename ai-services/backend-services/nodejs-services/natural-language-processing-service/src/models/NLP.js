const mongoose = require('mongoose');

const nlpAnalysisSchema = new mongoose.Schema({
    analysisId: String,
    text: String,
    sentiment: {
        score: Number,
        label: String
    },
    entities: [{
        text: String,
        label: String,
        confidence: Number
    }],
    keywords: [String],
    summary: String,
    language: String
});

module.exports = mongoose.model('NLPAnalysis', nlpAnalysisSchema);
