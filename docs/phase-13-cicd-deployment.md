# PayFlow AI — Phase 13: CI/CD and Deployment

## 1. Overview

Phase 13 implements automated testing, Docker image builds, vulnerability scanning,
container registry publishing, and production deployment preparation.

## 2. Continuous Integration

Workflow:
.github/workflows/ci.yml

The workflow includes:

- Backend tests using Java 21 and Maven.
- Frontend tests and production build using Node.js.
- Playwright end-to-end tests.
- Python fraud-service tests.
- Docker image builds.
- Trivy vulnerability scanning.

## 3. Continuous Delivery

Container registry: GitHub Container Registry (GHCR)

Published images:

- ghcr.io/varshithahify/payflow-backend
- ghcr.io/varshithahify/payflow-frontend
- ghcr.io/varshithahify/payflow-fraud

Images are published with commit SHA tags and a latest tag.

The verified CI commit is:

0730f69aa1b32e05168478c5afb9e32a568794a0

## 4. Docker Production Configuration

Production Compose file:

docker-compose.prod.yml

Services:

- PostgreSQL
- Redis
- Kafka
- Ollama
- Python fraud detection service
- Spring Boot backend
- React frontend with Nginx

Production Compose uses a separate project name:

payflow-ai-prod

Persistent volumes are defined for PostgreSQL, Redis, Kafka and Ollama.

Only the frontend is exposed through the configured HTTP port.

## 5. Environment and Secrets

Production configuration requires:

- DB_USERNAME
- DB_PASSWORD
- JWT_SECRET
- JWT_EXPIRATION
- OLLAMA_MODEL

Secrets must be provided through a secure environment configuration
on the deployment host.

Do not commit .env.docker or production credentials.

The JWT secret must be unique and sufficiently strong.

## 6. Production Validation

Completed checks:

- Docker Compose configuration validation.
- Frontend Docker image build.
- Nginx configuration syntax validation.
- Nginx backend service-name resolution test.
- Git whitespace validation.

Previously completed checks:

- Backend Maven test suite.
- Frontend unit tests and production build.
- Playwright tests.
- Fraud-service tests.
- Docker image builds.
- Trivy security scans.
- GHCR image publishing.
- Local full-stack Docker verification.

## 7. Deployment Procedure

On a suitable deployment host:

1. Install Docker Engine and Docker Compose.
2. Configure production environment variables securely.
3. Authenticate to GHCR if packages are private.
4. Obtain the production Compose file.
5. Pull the required container images.
6. Start services using Docker Compose.
7. Pull the configured Ollama model.
8. Verify application health endpoints.
9. Configure HTTPS and domain routing.

Example commands:

docker compose --env-file .env.prod -f docker-compose.prod.yml pull

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

Ollama model initialization:

docker compose --env-file .env.prod -f docker-compose.prod.yml exec ollama ollama pull llama3.2:3b

These commands are deployment instructions and have not been executed against a remote production host.

## 8. Failure Handling and Notifications

GitHub Actions provides workflow execution logs, failed-job details,
and configurable repository notification settings.

Workflow artifacts should be retained for debugging:

- Backend coverage report.
- Frontend test report.
- Fraud-service test results.

A dedicated external notification integration is not configured.

## 9. Rollback Procedure

Use a previously verified image SHA tag rather than relying on latest.

Update the image references in docker-compose.prod.yml to the
previously deployed image versions.

Then execute:

docker compose --env-file .env.prod -f docker-compose.prod.yml pull

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

Do not delete persistent database volumes during rollback.

## 10. Security Considerations

- Keep database, Redis and Kafka ports private.
- Use HTTPS for public access.
- Store secrets outside Git.
- Use unique production JWT secrets.
- Authenticate to private GHCR packages securely.
- Review Trivy findings before releases.
- Maintain database backups.
- Apply resource limits appropriate to the host.

## 11. Deployment Limitations

The production Compose configuration has been validated locally.

Remote deployment remains pending because no paid cloud hosting
resources are being provisioned.

Production HTTPS, domain configuration, remote health verification,
backup restoration testing and deployment rollback testing remain pending.

The complete application can continue to be demonstrated locally
using Docker Compose.

## 12. Phase 13 Status

13.1 CI workflow audit: Complete
13.2 Backend CI: Complete
13.3 Frontend CI: Complete
13.4 Fraud-service CI: Complete
13.5 Docker image automation: Complete
13.6 Trivy security scanning: Complete
13.7 GHCR integration: Complete
13.8 Image publishing: Complete
13.9 Production deployment preparation: Configuration validated; remote deployment pending
13.10 Failure handling and notifications: Basic GitHub notifications; dedicated integration pending
13.11 End-to-end verification: Local verification performed; final production verification pending
13.12 Documentation: Complete

Phase 13 implementation and deployment preparation are documented.
Actual remote production deployment is not claimed as complete.
