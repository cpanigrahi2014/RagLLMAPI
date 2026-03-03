You are a senior SaaS architect and AI backend engineer.

Design a production-ready multi-tenant RAG (Retrieval-Augmented Generation) 
platform using:

Backend: Java Spring Boot (microservices architecture)
Database: PostgreSQL + pgvector
Cache: Redis
LLM Provider: OpenAI API
Auth: JWT + Role-Based Access Control
Deployment: Docker + Kubernetes ready

Goal:
Build a SaaS platform where multiple schools (tenants) can:
- Upload their own CBSE/NCERT books
- Store documents isolated per tenant
- Query only their own content
- Track student usage and analytics
- Support subscription-based billing

--------------------------------------------------
ARCHITECTURE REQUIREMENTS
--------------------------------------------------

1. Multi-Tenancy Strategy:
   Implement logical isolation using:
   - tenant_id column in all tables
   - Hibernate filter or interceptor to enforce tenant isolation
   - Tenant extracted from JWT

2. Database Schema:

   tenants (
       id UUID PK,
       name,
       subscription_plan,
       status,
       created_at
   )

   users (
       id UUID PK,
       tenant_id FK,
       role (ADMIN, TEACHER, STUDENT),
       email,
       password_hash
   )

   books (
       id UUID PK,
       tenant_id FK,
       class,
       subject,
       name
   )

   chapters (
       id UUID PK,
       book_id FK
   )

   chunks (
       id UUID PK,
       tenant_id FK,
       chapter_id FK,
       content TEXT,
       embedding VECTOR(1536),
       page_number
   )

   usage_logs (
       id UUID,
       tenant_id,
       user_id,
       tokens_used,
       query,
       created_at
   )

3. Document Ingestion Flow:

   - Upload PDF
   - Extract text (Apache PDFBox)
   - Chunk by heading
   - Generate embeddings (OpenAI text-embedding-3-small)
   - Store with tenant_id

4. Query Flow:

   - Extract tenant_id from JWT
   - Generate query embedding
   - Perform similarity search:
       SELECT * FROM chunks
       WHERE tenant_id = :tenantId
       ORDER BY embedding <-> :queryVector
       LIMIT 5;

   - Construct system prompt:
       "You are a CBSE assistant for tenant {tenant_name}.
        Answer only using provided context.
        If not found, say Not available."

   - Call OpenAI GPT-4.1-mini

5. Subscription & Rate Limiting:

   - Store token limits per plan
   - Track usage_logs
   - Enforce monthly quota
   - Block over-limit users

6. Services (Microservices design):

   - auth-service
   - document-service
   - embedding-service
   - rag-query-service
   - billing-service
   - analytics-service

7. Add:

   - Redis caching for repeated questions
   - Async processing for embedding generation
   - Structured logging
   - Prometheus metrics
   - Health checks
   - API Gateway pattern
   - Kubernetes YAML samples
   - Horizontal scaling design

8. Security:

   - Tenant-level data isolation
   - Input sanitization
   - Prompt injection protection
   - Rate limiting

9. Provide:

   - Full project structure
   - Entity classes
   - Repository layer
   - Service layer
   - Controller layer
   - Dockerfile
   - application.yml
   - Sample JWT filter
   - Vector similarity query implementation
   - Usage tracking logic
   - Cost calculation per tenant

Write clean enterprise-grade code.
Follow SOLID principles.
Make it scalable to 100k+ users.