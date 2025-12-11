/** @type {import('next').NextConfig} */
const nextConfig = {
  experimental: {
    serverComponents: true,
  },
  env: {
    CUSTOM_KEY: process.env.CUSTOM_KEY,
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:3002/api/:path*', // API Gateway
      },
      {
        source: '/services/:path*',
        destination: 'http://localhost:3002/api/:path*' // Direct service access
      },
    ]
  },
  images: {
    domains: ['localhost'],
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '3002',
        path: '/api/**',
      },
    ],
  },
  webpack: (config, { isServer }) => {
    if (!isServer) {
      config.resolve.fallback = {
        fs: false,
        path: false,
      }
    }

    return config
  },
}

module.exports = nextConfig