const NLPAnalysis = require('../models/NLP');

class NLPController {
    analyzeText = async (req, res, next) => {
        try {
            const { text, analysisType } = req.body;

            const analysis = new NLPAnalysis({
                analysisId: `nlp_${Date.now()}`,
                text: text,
                sentiment: {
                    score: 0.75,
                    label: "positive"
                },
                entities: [
                    { text: "Gogidix", label: "ORGANIZATION", confidence: 0.95 }
                ],
                keywords: text.split(' ').slice(0, 10),
                summary: "Text analysis completed successfully",
                language: "en"
            });

            await analysis.save();

            res.json({
                success: true,
                data: analysis
            });
        } catch (error) {
            next(error);
        }
    };
}

module.exports = new NLPController();
