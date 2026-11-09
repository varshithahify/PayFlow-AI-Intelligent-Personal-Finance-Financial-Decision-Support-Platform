# PayFlow AI

## Intelligent Payment Reconciliation and Orchestration Platform

PayFlow AI is a full-stack payment reconciliation platform designed to detect transaction discrepancies, manage investigations, and provide AI-assisted explanations of payment issues.

## Key Features

- Transaction and payment management
- Payment reconciliation and discrepancy detection
- Investigation management
- Asynchronous event processing using Kafka
- Redis-based caching
- JWT authentication and Spring Security
- AI-assisted investigation using Spring AI and Ollama
- Python-based fraud detection service
- React-based dashboard

## Technology Stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot |
| Frontend | React, Redux Toolkit, HTML, CSS, JavaScript |
| Database | PostgreSQL, Spring Data JPA, Hibernate, Flyway |
| Security | Spring Security, JWT |
| Messaging | Apache Kafka |
| Caching | Redis |
| AI | Spring AI, Ollama, Llama 3.2 |
| Fraud Detection | Python, FastAPI, XGBoost |
| Infrastructure | Docker, Docker Compose, Nginx |
| Testing | JUnit, Mockito, Playwright, k6 |

## Architecture

The application runs as six Docker Compose services:

1. React frontend served through Nginx.
2. Spring Boot backend.
3. Python fraud detection service.
4. PostgreSQL database.
5. Redis cache.
6. Apache Kafka message broker.

Ollama runs separately on the host machine and is accessed by the backend.

## Run Locally with Docker

Prerequisites: Docker Desktop, Ollama, and the configured Ollama model.

Configure `.env.docker` using `.env.docker.example` as a reference. Never overwrite an existing `.env.docker` containing real credentials.

```powershell
docker compose --env-file .env.docker config -q
docker compose --env-file .env.docker build
docker compose --env-file .env.docker up -d
docker compose --env-file .env.docker ps
```

## Application URLs

| Component | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend Health | http://localhost:8081/actuator/health |
| Fraud Service Health | http://localhost:8083/health |

## Testing

Backend tests:

```powershell
cd backend/payflow-backend
.\mvnw.cmd clean test
```

Performance tests are available under `performance/k6/`.

## Documentation

- `docs/PRD.md` - Product requirements.
- `docs/architecture.md` - Target architecture.
- `docs/current-architecture.md` - Implemented architecture review.
- `docs/database-design.md` - Database design.
- `docs/api-design.md` - API design.
- `docs/security-design.md` - Security design.
- `docs/ai-architecture.md` - AI architecture.
- `docs/docker-deployment.md` - Docker deployment instructions.
- `docs/testing-and-quality-report.md` - Testing and quality report.

## AI Safety

AI-generated investigation findings are advisory. They do not independently execute payments, modify financial records, or resolve investigations. Human review is required where indicated.

## Project Status

Docker-based local deployment, backend testing, fraud service integration, and AI investigation workflow have been implemented and tested. This repository does not claim a production cloud deployment.
