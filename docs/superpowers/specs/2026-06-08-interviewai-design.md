# InterviewAI — Architecture & Phase 1 Design

## Architecture Overview

```
Browser (Vue3 + Element Plus + TypeScript)
    │
    ▼
Nginx (:80)
    ├── /api/v1/*  →  Spring Boot (:8080)
    └── /*         →  Vue static files
    │
    ▼
Spring Boot 3.x (Java 17, Maven)
    ├── MySQL 8.x       — 7 business tables
    ├── Redis 7.x       — Token cache, interview context, rate limiting
    ├── RabbitMQ 3.x    — Async: resume analysis, interview report, JD matching
    ├── Qdrant 1.x      — Vector search: questions, resume cases, JDs
    ├── DeepSeek API    — Chat model via SpringAI OpenAI starter
    └── 阿里百炼 API     — Embedding model via SpringAI
```

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Build tool | Maven | Dominant in Chinese Java ecosystem |
| Java version | 17 | Spring Boot 3.x minimum, LTS |
| Frontend pkg | npm | Per user preference |
| DeepSeek via SpringAI | `spring-ai-openai-spring-boot-starter` with base-url → `api.deepseek.com` | OpenAI-compatible API |
| Port | BE 8080, FE 5173 (dev) | Convention |
| Auth | JWT + Redis (7-day TTL) | Stateless, revocable |
| SSE (not WebSocket) | `Flux<ServerSentEvent>` | Unidirectional streaming suits interview flow |
| Async tasks | RabbitMQ queues | Prevents blocking API responses |

## Phase 1 Scope: Foundation

### Deliverables
1. Spring Boot project skeleton with full package structure
2. MySQL schema (7 tables) + seed data (100 interview questions)
3. MyBatis-Plus code generation for base CRUD
4. User registration (username + password, no email verification)
5. User login (username + password) returning JWT (stored in Redis, 7-day TTL)
6. Vue3 frontend skeleton: router, Axios interceptor, layout frame
7. SpringAI + DeepSeek connectivity smoke test endpoint

### Package Structure (Backend)
```
com.interviewai
├── controller          — REST controllers
├── service             — Business logic interfaces
│   ├── impl            — Implementations
│   └── ai              — AI services (ChatService, EmbeddingService)
├── mapper              — MyBatis-Plus mappers
├── entity              — JPA/MP entities
├── dto                 — Request DTOs
├── vo                  — Response VOs
├── config              — SpringAI, Qdrant, RabbitMQ, Redis, Security
├── consumer            — RabbitMQ consumers
├── common              — Unified response, exception handling, utils
└── InterviewAiApplication.java
```

### Frontend Structure
```
src/
├── views/              — Page components by module
├── components/         — Shared components
├── router/             — Vue Router config
├── api/                — Axios wrapper + endpoint definitions
├── stores/             — Pinia stores
├── types/              — TypeScript type definitions
├── utils/              — Utility functions
└── assets/             — Static assets
```

### Database Tables (Phase 1 creates all 7)
- `user` — user accounts
- `resume` — uploaded resumes
- `resume_analysis` — AI analysis reports
- `interview_session` — interview records
- `interview_message` — per-round Q&A
- `question` — question bank
- `job_description` — JD analysis records

### Phase 1 API Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/user/register | No | Register (username + password) |
| POST | /api/v1/user/login | No | Login (username + password), returns JWT |
| GET | /api/v1/user/profile | Yes | Get current user info |
| PUT | /api/v1/user/profile | Yes | Update profile |
| GET | /api/v1/ai/ping | Yes | Smoke test: DeepSeek connectivity |

### Redis Keys (Phase 1)
```
access_token:{userId}   — JWT token, 7-day TTL
```
