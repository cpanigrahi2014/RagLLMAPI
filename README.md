# RAG LLM Multi-Tenant SaaS Platform

A production-ready multi-tenant Retrieval-Augmented Generation (RAG) platform for educational institutions. Built with Java Spring Boot microservices, PostgreSQL + pgvector, Redis, and OpenAI.

## Architecture

```
                    ┌──────────────┐
                    │  API Gateway │  (Port 8080)
                    │ Rate Limiting│
                    └──────┬───────┘
                           │
         ┌─────────┬───────┼───────┬──────────┬──────────┐
         ▼         ▼       ▼       ▼          ▼          ▼
    ┌─────────┐┌────────┐┌─────┐┌────────┐┌────────┐┌──────────┐
    │  Auth   ││Document││Embed││  RAG   ││Billing ││Analytics │
    │ Service ││Service ││ding ││ Query  ││Service ││ Service  │
    │  :8081  ││ :8082  ││:8083││ :8084  ││ :8085  ││  :8086   │
    └────┬────┘└───┬────┘└──┬──┘└───┬────┘└───┬────┘└────┬─────┘
         │         │        │       │         │          │
         └─────────┴────────┴───┬───┴─────────┴──────────┘
                                │
                   ┌────────────┼────────────┐
                   ▼                         ▼
            ┌─────────────┐          ┌──────────┐
            │ PostgreSQL  │          │  Redis   │
            │ + pgvector  │          │  Cache   │
            └─────────────┘          └──────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Route requests, rate limiting, CORS |
| Auth Service | 8081 | JWT authentication, RBAC, user management |
| Document Service | 8082 | PDF upload, text extraction, chunking |
| Embedding Service | 8083 | OpenAI embedding generation |
| RAG Query Service | 8084 | Vector search, prompt construction, LLM completion |
| Billing Service | 8085 | Subscription plans, invoicing, cost tracking |
| Analytics Service | 8086 | Usage stats, dashboards, trend analysis |

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.2, Spring Cloud Gateway
- **Database**: PostgreSQL 16 + pgvector (vector similarity search)
- **Cache**: Redis 7 (query caching, rate limiting)
- **LLM**: OpenAI GPT-4.1-mini + text-embedding-3-small
- **Auth**: JWT + RBAC (ADMIN, TEACHER, STUDENT)
- **Monitoring**: Prometheus + Actuator metrics
- **Deployment**: Docker Compose + Kubernetes

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- OpenAI API key

### 1. Clone and configure
```bash
cp .env.example .env
# Edit .env with your OpenAI API key and database credentials
```

### 2. Run with Docker Compose
```bash
docker-compose up -d
```

### 3. Or build and run locally
```bash
mvn clean package -DskipTests
# Start each service individually
java -jar auth-service/target/auth-service-1.0.0-SNAPSHOT.jar
java -jar document-service/target/document-service-1.0.0-SNAPSHOT.jar
java -jar rag-query-service/target/rag-query-service-1.0.0-SNAPSHOT.jar
# ... etc
```

## API Endpoints

### Authentication
```
POST /api/v1/auth/register  - Register new user + tenant
POST /api/v1/auth/login     - Login and get JWT tokens
```

### Documents
```
POST /api/v1/documents/upload     - Upload PDF book (ADMIN/TEACHER)
GET  /api/v1/documents/books      - List all books for tenant
GET  /api/v1/documents/books/{id} - Get book details
```

### RAG Query
```
POST /api/v1/query       - Submit a question (returns AI answer + sources)
GET  /api/v1/query/usage - Get current month's usage stats
```

### Billing
```
GET  /api/v1/billing/invoices     - Get invoice history (ADMIN)
GET  /api/v1/billing/current-cost - Get current period cost (ADMIN)
POST /api/v1/billing/upgrade      - Upgrade subscription plan (ADMIN)
```

### Analytics
```
GET /api/v1/analytics/dashboard   - Tenant dashboard metrics
GET /api/v1/analytics/users       - Per-user usage breakdown
GET /api/v1/analytics/trend       - Daily query trend
GET /api/v1/analytics/top-queries - Most popular queries
```

## Multi-Tenancy

- **Logical isolation**: `tenant_id` column on all tenant-scoped tables
- **JWT-based**: Tenant ID extracted from JWT claims automatically
- **Thread-local context**: `TenantContext` propagated through request lifecycle
- **Entity listener**: Auto-sets `tenant_id` on persist, validates on update
- **Query isolation**: All vector searches WHERE tenant_id = :tenantId

## Subscription Plans

| Plan | Monthly Price | Token Limit |
|------|--------------|-------------|
| FREE | $0 | 50K tokens |
| BASIC | $29.99 | 500K tokens |
| STANDARD | $99.99 | 2M tokens |
| PREMIUM | $299.99 | 10M tokens |
| ENTERPRISE | $999.99 | 50M tokens |

## Kubernetes Deployment

```bash
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/config.yml
kubectl apply -f k8s/postgres.yml
kubectl apply -f k8s/redis.yml
kubectl apply -f k8s/services.yml
```

The RAG Query Service has HPA configured to scale from 2 to 10 replicas based on CPU/memory utilization.

## Monitoring

- **Prometheus**: http://localhost:9090
- **Health checks**: http://localhost:{port}/actuator/health
- **Metrics**: http://localhost:{port}/actuator/prometheus

## Project Structure

```
RagLLMAPI/
├── pom.xml                    # Parent POM
├── docker-compose.yml         # Full stack deployment
├── init-db.sql               # Database schema + pgvector
├── common/                    # Shared library
│   └── src/main/java/com/ragllm/common/
│       ├── entity/            # JPA entities (Tenant, User, Book, Chunk, etc.)
│       ├── dto/               # Request/Response DTOs
│       ├── security/          # JWT filter, SecurityConfig, UserPrincipal
│       ├── tenant/            # TenantContext, TenantFilterAspect
│       ├── exception/         # Global exception handler
│       ├── config/            # Redis, Async configs
│       └── util/              # InputSanitizer (prompt injection protection)
├── auth-service/              # Authentication & authorization
├── document-service/          # PDF processing & ingestion
├── embedding-service/         # OpenAI embedding generation
├── rag-query-service/         # Core RAG pipeline
├── billing-service/           # Subscriptions & invoicing
├── analytics-service/         # Usage analytics & reporting
├── api-gateway/               # Spring Cloud Gateway
├── k8s/                       # Kubernetes manifests
└── monitoring/                # Prometheus configuration
```
