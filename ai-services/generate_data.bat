@echo off
echo ?? Generating Synthetic Property Data...

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Run data generator
python data/synthetic_data_generator.py

echo ? Data generation complete!
echo Check the data/ directory for generated files.
pause
