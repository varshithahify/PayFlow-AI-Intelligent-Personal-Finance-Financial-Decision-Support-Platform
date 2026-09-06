# PayFlow AI - System Architecture

## 1. Architecture Overview

PayFlow AI is a payment intelligence and operations platform designed to
detect, investigate, and resolve payment failures and suspicious
transactions.

The system follows a modular backend architecture with asynchronous
event processing and AI-assisted investigation.

## 2. High-Level Architecture

```text
                    ┌──────────────────────┐
                    │      React UI        │
                    │   React + Redux      │
                    └──────────┬───────────┘
                               │
                               │ REST API
                               ▼
                    ┌──────────────────────┐
                    │    Spring Boot API   │
                    │      Gateway/API      │
                    └──────────┬───────────┘
                               │
             ┌─────────────────┼─────────────────┐
             │                 │                 │
             ▼                 ▼                 ▼
      ┌────────────┐    ┌────────────┐    ┌────────────┐
      │  Payment   │    │   Fraud    │    │Investigation│
      │  Service   │    │ Intelligence│   │   Service   │
      └─────┬──────┘    └─────┬──────┘    └─────┬──────┘
            │                 │                  │
            └─────────────────┼──────────────────┘
                              │
                              ▼
                       ┌─────────────┐
                       │    Kafka    │
                       │ Event Bus   │
                       └──────┬──────┘
                              │
              ┌───────────────┼────────────────┐
              ▼               ▼                ▼
        ┌──────────┐    ┌──────────┐     ┌──────────┐
        │PostgreSQL│    │  Redis   │     │ AI/LLM   │
        │ Database │    │  Cache   │     │ Services  │
        └──────────┘    └──────────┘     └──────────┘