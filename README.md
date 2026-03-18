# Expense Tracker

A full-stack expense tracking application built with **Spring Boot**, **MongoDB**, and **React**.

## Features

- 🔐 User authentication with JWT
- 💸 Create, read, update, and delete expenses
- 🗂️ Category management
- 🔍 Search and filter expenses
- 📊 Statistics and spending analytics
- 📱 Responsive React frontend

## Project Structure

```
expense-tracker/
├── backend/          # Spring Boot application (Maven)
│   ├── pom.xml
│   └── src/
└── frontend/         # React + Vite application
    ├── package.json
    └── src/
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- MongoDB 6+ (local or Atlas)

### Backend

```bash
cd backend
mvn spring-boot:run
```

Server starts at `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

App opens at `http://localhost:5173`.

## Documentation

- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Spring Security, Spring Data MongoDB |
| Auth | JWT (jjwt) |
| Database | MongoDB |
| Frontend | React 18, Vite, React Router v6 |
| Styling | TailwindCSS |
| Charts | Recharts |
| HTTP | Axios |