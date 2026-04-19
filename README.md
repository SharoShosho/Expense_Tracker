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
- `jwt.secret` (set via `JWT_SECRET` environment variable; avoid hardcoding)
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
(cd backend && mvn test)

# Frontend build
(cd frontend && npm run build)
```

## Deploy Frontend to GitHub Pages

Repository includes a workflow at:

`.github/workflows/deploy-frontend-pages.yml`

To enable deployment:

1. Go to **Settings → Pages** and set **Source** to **GitHub Actions**.
2. Add repository variable `VITE_API_BASE_URL` in **Settings → Secrets and variables → Actions → Variables** with your deployed backend URL (for example `https://your-backend.example.com/api`).
3. Push to `main` or run the workflow manually from the **Actions** tab.

After deployment, frontend is available at:

`https://<github-username>.github.io/<repository-name>/`

### Manual verification for GitHub Pages login/API

1. Open the deployed app URL above.
2. In browser DevTools → **Network**, submit login.
3. Verify an `OPTIONS` preflight request to `<your-backend-domain>/api/auth/login` returns success.
4. Verify the login request is `POST` to `<your-backend-domain>/api/auth/login` (not `https://<github-username>.github.io/api/...`).
5. Verify login response is successful and app navigates to the dashboard.


## Additional Docs

- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)
