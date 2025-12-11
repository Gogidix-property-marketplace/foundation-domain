const DocumentAnalysis = require('../models/Document');

class DocumentController {
    analyzeDocument = async (req, res, next) => {
        try {
            const analysis = new DocumentAnalysis({
                documentId: `doc_${Date.now()}`,
                fileName: req.file?.originalname,
                extractedText: "Sample extracted text from document",
                entities: [
                    { type: "PERSON", value: "John Doe", confidence: 0.95 },
                    { type: "ORGANIZATION", value: "Gogidix", confidence: 0.92 }
                ],
                classification: "Contract",
                sentiment: { score: 0.1, magnitude: 0.5 },
                keywords: ["property", "contract", "agreement"],
                summary: "Document contains property agreement details"
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

module.exports = new DocumentController();
