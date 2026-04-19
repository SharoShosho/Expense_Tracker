# Expense Tracker - Backend

Spring Boot backend for the Expense Tracker application.

## Requirements

- Java 17+
- Maven 3.8+
- MongoDB 6+

## Setup

### 1. Configure MongoDB

The application connects to MongoDB by default at `mongodb://localhost:27017/expense_tracker`.

To use MongoDB Atlas, update `application.properties`:
```properties
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/expense_tracker
```

### 2. Configure JWT Secret

Update `application.properties` with a strong secret:
```properties
jwt.secret=your-very-long-secret-key-at-least-32-characters
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### 4. Configure CORS origins (for frontend deployments)

Set allowed origin patterns in `application.properties`:

```properties
spring.web.cors.allowed-origin-patterns=${ALLOWED_CORS_ORIGINS:http://localhost:*,http://127.0.0.1:*,https://sharoshosho.github.io}
```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |

**Register request body:**
```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

**Login response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  "id": "60d5ec49f1a2b3c4d5e6f7a8"
}
```

### Expenses (require `Authorization: Bearer <token>` header)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/expenses` | Create expense |
| GET | `/api/expenses` | List expenses (with optional filters) |
| GET | `/api/expenses/{id}` | Get single expense |
| PUT | `/api/expenses/{id}` | Update expense |
| DELETE | `/api/expenses/{id}` | Delete expense |
| GET | `/api/statistics` | Get statistics |

**Query parameters for GET /api/expenses:**
- `category` - filter by category
- `search` - search in description
- `startDate` - ISO date (e.g. `2024-01-01`)
- `endDate` - ISO date (e.g. `2024-12-31`)

**Expense request body:**
```json
{
  "amount": 49.99,
  "category": "Food",
  "description": "Lunch at restaurant",
  "date": "2024-01-15"
}
```

## Build

```bash
mvn clean package
java -jar target/expense-tracker-0.0.1-SNAPSHOT.jar
```
