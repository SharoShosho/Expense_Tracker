# Expense Tracker - Frontend

React + Vite frontend for the Expense Tracker application.

## Requirements

- Node.js 18+
- npm 9+

## Setup

### 1. Install dependencies

```bash
npm install
```

### 2. Configure API URL

For local development, the frontend proxies API requests to `http://localhost:8080` by default (configured in `vite.config.js`).

If your backend runs on a different port, update `vite.config.js`:
```js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
},
```

For production builds (for example GitHub Pages), set:

- `VITE_API_BASE_URL=https://your-backend-domain/api`

> GitHub Pages deployments require an **absolute** `VITE_API_BASE_URL` (must start with `http://` or `https://`). Do not use a relative `/api` value in production.

### 3. Start the development server

```bash
npm run dev
```

The app opens at `http://localhost:5173`.

Routing on GitHub Pages uses hash-based URLs (`#/login`, `#/dashboard`, etc.), so refresh/deep links continue to work without a custom `404.html` fallback.

## Build for Production

```bash
npm run build
npm run preview
```

## Pages

| Route | Description |
|-------|-------------|
| `/login` | Login page |
| `/register` | Register page |
| `/dashboard` | Expense list with filters |
| `/statistics` | Charts and statistics |

## Features

- 🔐 JWT authentication (token stored in localStorage)
- 📋 Add, edit, and delete expenses
- 🔍 Filter by category, description, or date range
- 📊 Visual statistics with pie and bar charts
- 📱 Responsive design with TailwindCSS

## Tech Stack

- React 18
- Vite
- React Router v6
- Axios
- Recharts
- TailwindCSS
