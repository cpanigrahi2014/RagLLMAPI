#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
#  EC2 Server Setup Script — CBSE AI Tutor (RAG LLM Platform)
#  Run this on a fresh Ubuntu 22.04/24.04 EC2 instance
#  Usage: sudo bash setup-ec2.sh
# ═══════════════════════════════════════════════════════════════════

set -e

echo "══════════════════════════════════════════"
echo "  CBSE AI Tutor — EC2 Server Setup"
echo "══════════════════════════════════════════"

# ─── 1. System Updates ──────────────────────────────────
echo "[1/6] Updating system packages..."
apt-get update -y && apt-get upgrade -y

# ─── 2. Install Docker ─────────────────────────────────
echo "[2/6] Installing Docker..."
apt-get install -y ca-certificates curl gnupg lsb-release

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow ubuntu user to run Docker
usermod -aG docker ubuntu

systemctl enable docker
systemctl start docker

echo "Docker version: $(docker --version)"
echo "Docker Compose version: $(docker compose version)"

# ─── 3. Install Git & Java (for Maven build) ──────────
echo "[3/6] Installing Git & Java 21..."
apt-get install -y git

# Install Amazon Corretto 21 (free JDK for AWS)
curl -fsSL https://apt.corretto.aws/corretto.key | gpg --dearmor -o /etc/apt/keyrings/corretto.gpg
echo "deb [signed-by=/etc/apt/keyrings/corretto.gpg] https://apt.corretto.aws stable main" | \
  tee /etc/apt/sources.list.d/corretto.list > /dev/null
apt-get update -y
apt-get install -y java-21-amazon-corretto-jdk

# Install Maven
apt-get install -y maven

echo "Java: $(java -version 2>&1 | head -1)"
echo "Maven: $(mvn -version 2>&1 | head -1)"

# ─── 4. Install Node.js (for frontend build in Docker) ─
echo "[4/6] Installing Node.js 20..."
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt-get install -y nodejs
echo "Node: $(node -v), npm: $(npm -v)"

# ─── 5. Setup Swap (important for memory-constrained instances) ─
echo "[5/6] Configuring 4GB swap..."
if [ ! -f /swapfile ]; then
    fallocate -l 4G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    echo "Swap configured: $(swapon --show)"
else
    echo "Swap already exists"
fi

# Optimize swap behavior
sysctl vm.swappiness=10
echo 'vm.swappiness=10' >> /etc/sysctl.conf

# ─── 6. Firewall ───────────────────────────────────────
echo "[6/6] Configuring firewall..."
apt-get install -y ufw
ufw allow 22/tcp   # SSH
ufw allow 80/tcp   # HTTP (Nginx)
ufw allow 443/tcp  # HTTPS (future)
ufw --force enable

echo ""
echo "══════════════════════════════════════════"
echo "  ✅ Server setup complete!"
echo ""
echo "  Next steps:"
echo "  1. Clone your repo:"
echo "     git clone <your-repo-url> ~/RagLLMAPI"
echo "     cd ~/RagLLMAPI"
echo ""
echo "  2. Create .env file:"
echo "     cp .env.production .env"
echo "     nano .env   # fill in your API keys"
echo ""
echo "  3. Build & deploy:"
echo "     mvn package -DskipTests"
echo "     docker compose -f docker-compose.prod.yml build"
echo "     docker compose -f docker-compose.prod.yml up -d"
echo ""
echo "  4. Access the app at:"
echo "     http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo '<YOUR-EC2-PUBLIC-IP>')/"
echo "══════════════════════════════════════════"
