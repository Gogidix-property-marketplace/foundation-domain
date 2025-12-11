@echo off
echo ?? Testing Native API Services...

echo Testing AI Gateway health...
curl -s http://localhost:8000/health

echo.
echo Testing property valuation...
curl -X POST http://localhost:8000/api/v1/property-valuation ^
  -H "Content-Type: application/json" ^
  -d "{""property_type"": ""apartment"", ""bedrooms"": 2, ""bathrooms"": 2, ""square_feet"": 1200, ""city"": ""New York""}"

echo.
echo Testing chat endpoint...
curl -X POST http://localhost:8000/api/v1/chat ^
  -H "Content-Type: application/json" ^
  -d "{""message"": ""Hello, can you help me find a property?""}"

echo.
echo ? Tests completed!
pause
