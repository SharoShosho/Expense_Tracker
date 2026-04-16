# Expense Tracker

A full-stack personal finance app for tracking expenses, managing category budgets, and getting AI-powered spending insights.

## Tech Stack

- **Backend:** Spring Boot 3, Spring Security, Spring Data MongoDB, Maven
- **Frontend:** React 18, Vite, React Router, TailwindCSS, Axios, Recharts
- **Database:** MongoDB
- **Auth:** JWT

## Key Features

- User registration/login with JWT authentication
- Expense CRUD with category/date/search filtering
- Soft delete, restore, and permanent delete support
- Budget setup per category and monthly budget status
- Statistics dashboard with charts and spending summaries
- AI insights:
  - Tips overview
  - Spending pattern analysis
  - Behavioral analysis
  - Benchmarking
  - Predictions
  - Anomaly alerts
  - Category deep dive
  - Wellness score
  - History trend
- Currency selection with exchange-rate conversion
- Light/dark theme support

## Project Structure

```text
Expense_Tracker/
├── backend/    # Spring Boot API
├── frontend/   # React + Vite app
└── README.md
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- npm 9+
- MongoDB 6+ (local or Atlas)

## Configuration

Backend config is in:

`backend/src/main/resources/application.properties`

Important properties:

- `spring.data.mongodb.uri` (MongoDB connection string)
- `jwt.secret` (set a strong secret in production; can be overridden by `JWT_SECRET`)
- `server.port` (default `8080`)

## Run Locally

### 1) Start Backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs at: `http://localhost:8080`

### 2) Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at: `http://localhost:5173`

By default, the frontend proxies `/api/*` to `http://localhost:8080` (see `frontend/vite.config.js`).

## Main API Areas

- `/api/auth/*` - login/register
- `/api/expenses` - expense CRUD + statistics
- `/api/budgets/*` - budget management and status
- `/api/data-management/*` - soft delete/restore/bulk operations
- `/api/ai/*` - AI analytics endpoints
- `/api/ai/train/*` - model training endpoints
- `/api/ai/tips/feedback` - tip feedback/personalization

## Validation Commands

```bash
# Backend tests
cd backend
mvn test

# Frontend build
cd frontend
npm run build
```

## Additional Docs

- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)
