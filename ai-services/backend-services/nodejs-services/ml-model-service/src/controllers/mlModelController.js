const MLModel = require('../models/MLModel');

class MLModelController {
    getModels = async (req, res, next) => {
        try {
            const models = await MLModel.find();
            res.json({
                success: true,
                data: models
            });
        } catch (error) {
            next(error);
        }
    };
}

module.exports = new MLModelController();
