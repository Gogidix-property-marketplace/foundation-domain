@echo off
echo ?? Training Property Valuation Model...

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Check if data exists
if not exist "data\properties_sample_1000.parquet" (
    echo ? No training data found. Generating sample data first...
    python data/synthetic_data_generator.py
)

REM Train model
python training/train_property_valuation.py --data data/properties_sample_1000.parquet

echo ? Model training complete!
echo Check the models/ directory for trained models.
pause
