# CBSE AI Tutor — Cloud Deployment Guide (AWS EC2)

Deploy the full platform to AWS EC2 so anyone can access it via a URL.

---

## Architecture

```
Internet → http://<EC2-IP>/
             │
         [Nginx :80]
         ├── /           → React SPA (static files)
         └── /api/*      → API Gateway :8080
                            ├── /api/v1/auth/**     → auth-service :8081
                            ├── /api/v1/documents/**→ document-service :8082
                            ├── /api/v1/embeddings/**→ embedding-service :8083
                            ├── /api/v1/query/**    → rag-query-service :8084
                            ├── /api/v1/studio/**   → rag-query-service :8084
                            ├── /api/v1/models/**   → rag-query-service :8084
                            ├── /api/v1/billing/**  → billing-service :8085
                            └── /api/v1/analytics/**→ analytics-service :8086
```

## Prerequisites

- AWS account (free tier eligible)
- Your API keys: OpenAI (required), Gemini (optional), Anthropic (optional)

---

## Step 1: Create EC2 Instance

1. Go to [AWS EC2 Console](https://console.aws.amazon.com/ec2/)
2. Click **Launch Instance**
3. Choose:
   - **Name**: `cbse-ai-tutor`
   - **AMI**: Ubuntu Server 24.04 LTS (free tier eligible)
   - **Instance type**: `t3.medium` (4 GB RAM, ~$30/mo) or `t3.small` (2 GB RAM, ~$15/mo)
     > ⚠️ `t2.micro` (free tier, 1 GB) is too small for 10 containers. Use t3.small minimum.
   - **Key pair**: Create new → download `.pem` file
   - **Security Group**: Create new with these rules:
     | Type | Port | Source |
     |------|------|--------|
     | SSH | 22 | My IP |
     | HTTP | 80 | Anywhere (0.0.0.0/0) |
     | HTTPS | 443 | Anywhere (0.0.0.0/0) |
   - **Storage**: 30 GB gp3 (free tier allows 30 GB)
4. Click **Launch Instance**
5. Note the **Public IPv4 address** from the instance details

---

## Step 2: Connect to EC2

```bash
# Make key file read-only
chmod 400 your-key.pem

# SSH into the instance
ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>
```

---

## Step 3: Setup Server

```bash
# Download and run the setup script (or copy it manually)
# Option A: If you have the repo
git clone https://github.com/<your-username>/RagLLMAPI.git
cd RagLLMAPI
sudo bash deploy/setup-ec2.sh

# Option B: Run setup commands manually (see deploy/setup-ec2.sh)
```

The setup script installs: Docker, Java 21, Maven, Node.js 20, and configures 4GB swap.

---

## Step 4: Configure Environment

```bash
cd ~/RagLLMAPI

# Create .env from template
cp .env.production .env

# Edit with your actual API keys
nano .env
```

Fill in these values in `.env`:
```
JWT_SECRET=<generate-a-random-32-char-string>
OPENAI_API_KEY=sk-proj-your-actual-key
GEMINI_API_KEY=your-gemini-key-or-leave-empty
ANTHROPIC_API_KEY=your-anthropic-key-or-leave-empty
```

Generate a JWT secret: `openssl rand -base64 32`

---

## Step 5: Deploy

```bash
# One-click deploy
bash deploy/deploy.sh
```

Or step by step:
```bash
# Build Java services
mvn package -DskipTests

# Build Docker images (includes frontend build inside Nginx container)
docker compose -f docker-compose.prod.yml build

# Start everything
docker compose -f docker-compose.prod.yml up -d

# Check status
docker compose -f docker-compose.prod.yml ps
```

---

## Step 6: Access the App

Open in any browser:
```
http://<EC2-PUBLIC-IP>/
```

Share this URL with anyone — they can:
1. Register an account
2. Upload documents
3. Ask questions (RAG + AI Studio)
4. Use all 20 AI models (OpenAI + Gemini + Claude)

---

## Useful Commands

```bash
# View all logs
docker compose -f docker-compose.prod.yml logs -f

# View specific service logs
docker compose -f docker-compose.prod.yml logs -f rag-query-service

# Restart a service
docker compose -f docker-compose.prod.yml restart rag-query-service

# Restart everything
docker compose -f docker-compose.prod.yml restart

# Stop everything
docker compose -f docker-compose.prod.yml down

# Stop and remove all data (fresh start)
docker compose -f docker-compose.prod.yml down -v

# Check resource usage
docker stats
```

---

## Updating the App

```bash
cd ~/RagLLMAPI

# Pull latest code
git pull

# Rebuild and redeploy
mvn package -DskipTests
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

---

## Cost Estimate

| Instance | RAM | Monthly Cost | Notes |
|----------|-----|-------------|-------|
| t3.small | 2 GB | ~$15/mo | Minimum viable, uses swap |
| t3.medium | 4 GB | ~$30/mo | **Recommended** for smooth experience |
| t3.large | 8 GB | ~$60/mo | Comfortable for multiple users |
| t2.micro | 1 GB | Free tier | ❌ Not enough for 10 containers |

> **Tip**: Use a Spot Instance for up to 70% savings (~$9/mo for t3.medium).

Additional costs:
- Storage: 30 GB gp3 = free tier (first year)
- Data transfer: 100 GB/mo = free tier
- After free tier: ~$2.40/mo for 30 GB storage

---

## Optional: Custom Domain + HTTPS

1. Register a domain (e.g., on Namecheap, Route53)
2. Point domain A record to EC2 public IP
3. Install Certbot for free SSL:
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Services won't start | Check logs: `docker compose -f docker-compose.prod.yml logs` |
| Out of memory | Add more swap or upgrade instance |
| Port 80 not accessible | Check EC2 Security Group allows HTTP (port 80) |
| API keys not working | Verify `.env` values, redeploy: `docker compose -f docker-compose.prod.yml up -d` |
| Build fails | Ensure Maven and Java are installed: `java -version && mvn -version` |
