#!/bin/bash
# ═══════════════════════════════════════════════════════════
#  One-click Deploy Script — Run after setup-ec2.sh
#  Usage: bash deploy.sh
# ═══════════════════════════════════════════════════════════

set -e

echo "══════════════════════════════════════════"
echo "  CBSE AI Tutor — Building & Deploying"
echo "══════════════════════════════════════════"

# Check .env exists
if [ ! -f .env ]; then
    echo "❌ .env file not found!"
    echo "   Run: cp .env.production .env && nano .env"
    exit 1
fi

# Check API key is set
if grep -q "sk-your-openai-key-here" .env; then
    echo "❌ Please set your OPENAI_API_KEY in .env"
    exit 1
fi

if grep -q "CHANGE-ME" .env; then
    echo "❌ Please change JWT_SECRET in .env"
    exit 1
fi

# ─── 1. Build Java services ────────────────────────────
echo ""
echo "[1/3] Building Java services (Maven)..."
mvn package -DskipTests -q
echo "✅ Maven build complete"

# ─── 2. Build Docker images ────────────────────────────
echo ""
echo "[2/3] Building Docker images (this takes a few minutes)..."
docker compose -f docker-compose.prod.yml build
echo "✅ Docker images built"

# ─── 3. Start all services ─────────────────────────────
echo ""
echo "[3/3] Starting all services..."
docker compose -f docker-compose.prod.yml up -d
echo "✅ All services starting"

# ─── Wait for health checks ────────────────────────────
echo ""
echo "Waiting for services to be ready..."
sleep 30

# Show service status
echo ""
docker compose -f docker-compose.prod.yml ps

# Get public IP
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "YOUR-EC2-PUBLIC-IP")

echo ""
echo "══════════════════════════════════════════"
echo "  ✅ Deployment complete!"
echo ""
echo "  🌐 App URL: http://${PUBLIC_IP}/"
echo ""
echo "  Share this URL with your testers!"
echo ""
echo "  Useful commands:"
echo "    docker compose -f docker-compose.prod.yml logs -f     # View logs"
echo "    docker compose -f docker-compose.prod.yml ps          # Service status"
echo "    docker compose -f docker-compose.prod.yml restart     # Restart all"
echo "    docker compose -f docker-compose.prod.yml down        # Stop all"
echo "══════════════════════════════════════════"
