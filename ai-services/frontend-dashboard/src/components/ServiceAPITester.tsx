'use client'

import { useState } from 'react'
import {
  PlayIcon,
  DocumentTextIcon,
  CommandLineIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  ClipboardIcon,
  ArrowPathIcon
} from '@heroicons/react/24/outline'

interface ServiceAPITesterProps {
  serviceId: string
  serviceName: string
  port: number
  endpoints: any
}

export function ServiceAPITester({ serviceId, serviceName, port, endpoints }: ServiceAPITesterProps) {
  const [selectedEndpoint, setSelectedEndpoint] = useState('predict')
  const [method, setMethod] = useState('POST')
  const [headers, setHeaders] = useState('{\n  "Content-Type": "application/json"\n}')
  const [body, setBody] = useState('')
  const [response, setResponse] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [history, setHistory] = useState<any[]>([])

  const sampleRequests: Record<string, any> = {
    'predictive-analytics': {
      predict: {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: {
          data: [100, 120, 130, 125, 140, 150, 160, 155, 170, 180],
          horizon: 30,
          model: 'prophet',
          confidence: 0.95
        }
      },
      'recommendation-service': {
        recommend: {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: {
            user_id: 'USR-12345',
            num_recommendations: 10,
            algorithm: 'hybrid',
            context: {
              page: 'homepage',
              time_of_day: 'evening'
            }
          }
        }
      },
      'nlp-processing': {
        sentiment: {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: {
            text: 'I love this product! It works amazing and exceeded my expectations.',
            model: 'bert',
            return_confidence: true
          }
        },
        classify: {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: {
            text: 'The latest smartphone features include advanced AI capabilities',
            categories: ['technology', 'business', 'lifestyle', 'science'],
            multi_label: true
          }
        },
        entities: {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: {
            text: 'Apple Inc. announced their new iPhone in Cupertino, California last week.',
            model: 'bert',
            return_positions: true
          }
        }
      },
      'computer-vision': {
        detect: {
          method: 'POST',
          headers: { 'Content-Type': 'multipart/form-data' },
          body: 'Use file upload for image',
          note: 'Upload an image file to detect objects'
        },
        classify: {
          method: 'POST',
          headers: { 'Content-Type': 'multipart/form-data' },
          body: 'Use file upload for image',
          note: 'Upload an image file for classification'
        }
      },
      'ai-training': {
        train: {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: {
            dataset_path: '/data/training.csv',
            model_type: 'classification',
            target_column: 'label',
            feature_columns: ['feature1', 'feature2', 'feature3'],
            test_size: 0.2,
            cv_folds: 5,
            algorithms: ['random_forest', 'xgboost', 'neural_network']
          }
        }
      }
  }

  const loadSampleRequest = () => {
    const samples = sampleRequests[serviceId]
    if (samples && samples[selectedEndpoint]) {
      const sample = samples[selectedEndpoint]
      setMethod(sample.method)
      setHeaders(JSON.stringify(sample.headers, null, 2))
      if (typeof sample.body === 'object') {
        setBody(JSON.stringify(sample.body, null, 2))
      } else {
        setBody(sample.body)
      }
    }
  }

  const sendRequest = async () => {
    setIsLoading(true)
    const startTime = Date.now()

    try {
      let url = `http://localhost:${port}/api/v1/${selectedEndpoint}`

      // Special handling for different service patterns
      if (serviceId === 'nlp-processing' && selectedEndpoint.includes('/')) {
        url = `http://localhost:${port}/api/v1/nlp${selectedEndpoint}`
      }

      const options: any = {
        method: method,
        headers: JSON.parse(headers),
      }

      // Handle body based on content type
      if (method !== 'GET' && method !== 'HEAD') {
        if (headers.includes('multipart/form-data')) {
          // For file uploads, create a different approach
          setResponse('File upload interface would be implemented here\n\nPlease use the file upload component to test image-based endpoints.')
        } else if (body) {
          options.body = typeof body === 'string' ? body : JSON.stringify(body)
        }
      }

      const response = await fetch(url, options)
      const endTime = Date.now()
      const responseTime = endTime - startTime

      const responseText = await response.text()
      let responseData
      try {
        responseData = JSON.parse(responseText)
      } catch {
        responseData = responseText
      }

      const result = {
        timestamp: new Date().toISOString(),
        method: method,
        url: url,
        status: response.status,
        responseTime: `${responseTime}ms`,
        request: {
          headers: JSON.parse(headers),
          body: method !== 'GET' ? body : null
        },
        response: responseData
      }

      setResponse(JSON.stringify(result, null, 2))
      setHistory([result, ...history.slice(0, 9)]) // Keep last 10 requests
    } catch (error) {
      setResponse(JSON.stringify({
        error: true,
        message: error.message,
        timestamp: new Date().toISOString()
      }, null, 2))
    }

    setIsLoading(false)
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
  }

  return (
    <div className="space-y-6">
      {/* Endpoint Selector */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">API Endpoint Tester</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Endpoint</label>
            <select
              value={selectedEndpoint}
              onChange={(e) => setSelectedEndpoint(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {serviceId === 'nlp-processing' && (
                <>
                  <option value="/sentiment">Sentiment Analysis</option>
                  <option value="/classify">Text Classification</option>
                  <option value="/entities">Entity Recognition</option>
                  <option value="/translate">Translation</option>
                  <option value="/summarize">Summarization</option>
                </>
              )}
              {serviceId === 'predictive-analytics' && (
                <>
                  <option value="predict">Predict</option>
                  <option value="forecast">Forecast</option>
                  <option value="anomaly">Anomaly Detection</option>
                </>
              )}
              {serviceId === 'recommendation-service' && (
                <>
                  <option value="recommend">Get Recommendations</option>
                  <option value="similar">Find Similar Items</option>
                  <option value="popular">Popular Items</option>
                </>
              )}
              {serviceId === 'computer-vision' && (
                <>
                  <option value="detect">Object Detection</option>
                  <option value="classify">Image Classification</option>
                  <option value="faces">Face Detection</option>
                </>
              )}
              {serviceId === 'ai-training' && (
                <>
                  <option value="train">Start Training</option>
                  <option value="status">Training Status</option>
                  <option value="models">List Models</option>
                </>
              )}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Method</label>
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>
        </div>

        <button
          onClick={loadSampleRequest}
          className="mb-4 flex items-center px-3 py-1.5 text-sm bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200"
        >
          <DocumentTextIcon className="h-4 w-4 mr-1" />
          Load Sample Request
        </button>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {/* Headers */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Headers
              <button
                onClick={() => copyToClipboard(headers)}
                className="ml-2 text-gray-500 hover:text-gray-700"
              >
                <ClipboardIcon className="h-4 w-4 inline" />
              </button>
            </label>
            <textarea
              value={headers}
              onChange={(e) => setHeaders(e.target.value)}
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-sm"
            />
          </div>

          {/* Body */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Request Body
              <button
                onClick={() => copyToClipboard(body)}
                className="ml-2 text-gray-500 hover:text-gray-700"
              >
                <ClipboardIcon className="h-4 w-4 inline" />
              </button>
            </label>
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={4}
              placeholder="Enter JSON payload or leave empty for GET requests"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-sm"
            />
          </div>
        </div>

        <div className="mt-4 flex items-center space-x-3">
          <button
            onClick={sendRequest}
            disabled={isLoading}
            className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50"
          >
            {isLoading ? (
              <ArrowPathIcon className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <PlayIcon className="h-4 w-4 mr-2" />
            )}
            {isLoading ? 'Sending...' : 'Send Request'}
          </button>

          <button
            onClick={() => {
              setResponse('')
              setBody('')
            }}
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Response */}
      {response && (
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Response</h3>
            <button
              onClick={() => copyToClipboard(response)}
              className="text-gray-500 hover:text-gray-700"
            >
              <ClipboardIcon className="h-5 w-5" />
            </button>
          </div>
          <pre className="p-4 bg-gray-100 rounded-lg overflow-x-auto text-sm">
            <code>{response}</code>
          </pre>
        </div>
      )}

      {/* Request History */}
      {history.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Request History</h3>
          <div className="space-y-3">
            {history.map((req, index) => (
              <div key={index} className="p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-3">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      req.status >= 200 && req.status < 300
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {req.status}
                    </span>
                    <span className="text-sm font-medium text-gray-900">{req.method}</span>
                    <span className="text-sm text-gray-600">{req.url}</span>
                  </div>
                  <div className="flex items-center space-x-3 text-xs text-gray-500">
                    <span>{req.responseTime}</span>
                    <span>{new Date(req.timestamp).toLocaleTimeString()}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* API Documentation Reference */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">API Documentation</h3>
        <div className="space-y-3">
          <a
            href={`http://localhost:${port}/docs`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-between p-3 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            <div className="flex items-center space-x-3">
              <DocumentTextIcon className="h-5 w-5 text-gray-400" />
              <span className="text-sm font-medium text-gray-900">Swagger/OpenAPI Documentation</span>
            </div>
            <span className="text-xs text-gray-500">Live docs at port {port}</span>
          </a>

          <a
            href={`http://localhost:${port}/redoc`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-between p-3 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            <div className="flex items-center space-x-3">
              <CommandLineIcon className="h-5 w-5 text-gray-400" />
              <span className="text-sm font-medium text-gray-900">ReDoc Documentation</span>
            </div>
            <span className="text-xs text-gray-500">Alternative documentation view</span>
          </a>
        </div>
      </div>
    </div>
  )
}