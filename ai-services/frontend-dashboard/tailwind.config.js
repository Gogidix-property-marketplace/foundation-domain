/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        gogidix: {
          50: '#1e40af',
          100: '#3730a3',
          200: '#4338ca',
          300: '#4f46e5',
          400: '#6366f1',
          500: '#818cf8',
          600: '#a78bfa',
          700: '#c7d2fe',
          800: '#ddd6fe',
          900: '#ede9fe',
        },
        success: {
          50: '#10b981',
          100: '#059669',
          500: '#10b981',
        },
        warning: {
          50: '#f59e0b',
          100: '#d97706',
          500: '#f59e0b',
        },
        danger: {
          50: '#ef4444',
          100: '#dc2626',
          500: '#ef4444',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui'],
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'bounce-slow': 'bounce 2s infinite',
      },
      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
  ],
}