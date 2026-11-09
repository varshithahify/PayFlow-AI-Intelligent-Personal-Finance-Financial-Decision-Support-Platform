# PayFlow AI - Docker Deployment

## Architecture

PayFlow AI runs locally using Docker Compose with six services:
- PostgreSQL 18
- Redis 7
- Apache Kafka 4.3.1 (KRaft)
- Python FastAPI Fraud Detection Service
- Java 21 Spring Boot Backend
- React Frontend served by Nginx

Ollama runs separately on the Windows host.

## Ports

| Service | Host port | Container port |
|---|---:|---:|
| Frontend | 3000 | 8080 |
| Backend | 8081 | 8081 |
| Fraud | 8083 | 8083 |
| PostgreSQL | 5433 | 5432 |
| Redis | 6380 | 6379 |
| Kafka external | 9094 | 9094 |

All published ports bind to 127.0.0.1.

## Environment

Copy `.env.docker.example` to `.env.docker` only when creating a new environment. Do not overwrite an existing environment file.
Configure a strong database password and a unique JWT secret.
Never commit `.env.docker`.

## Build and start

```powershell
docker compose --env-file .env.docker config -q
docker compose --env-file .env.docker build
docker compose --env-file .env.docker up -d
docker compose --env-file .env.docker ps
```

## Health checks

```powershell
Invoke-WebRequest http://localhost:3000/health -UseBasicParsing
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8083/health
docker exec payflow-postgres pg_isready -U postgres -d payflow
docker exec payflow-redis redis-cli ping
```

## Persistent storage

Named volumes:
- payflow-postgres-data
- payflow-redis-data
- payflow-kafka-data

Redis uses append-only persistence. Docker volumes are persistent storage, not backups.

Do not run `docker compose down -v` if you need to preserve application data.

## Ollama

The backend connects to Ollama on the Windows host through `host.docker.internal:11434`.
Ensure Ollama is running and the configured model is available.

## Security

Backend, fraud and frontend containers use non-root users, read-only root filesystems, dropped capabilities, no-new-privileges and restricted temporary storage.
Published ports are bound to localhost.
This is a local development deployment, not a production-ready security configuration.

## Troubleshooting

```powershell
docker compose --env-file .env.docker logs --tail 100 backend
docker compose --env-file .env.docker logs --tail 100 kafka
docker compose --env-file .env.docker ps
docker exec payflow-frontend getent hosts backend
```
