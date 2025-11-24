# Deployment Guide
## CodeSage AI - Self-Hosted Deployment

**Version:** 1.0  
**Date:** November 24, 2025  
**Target Environment:** Self-hosted on spare PC

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Infrastructure Setup](#2-infrastructure-setup)
3. [Application Deployment](#3-application-deployment)
4. [Configuration](#4-configuration)
5. [Monitoring & Maintenance](#5-monitoring--maintenance)
6. [Troubleshooting](#6-troubleshooting)
7. [Backup & Recovery](#7-backup--recovery)

---

## 1. Prerequisites

### 1.1 Hardware Requirements

**Minimum:**
- CPU: 4 cores (Intel i5/Ryzen 5 or equivalent)
- RAM: 8GB
- Storage: 50GB free space
- Network: Stable internet connection

**Recommended:**
- CPU: 6+ cores
- RAM: 16GB
- Storage: 100GB SSD
- Network: 100 Mbps+ connection

### 1.2 Software Requirements

- **OS**: Ubuntu 22.04 LTS (recommended) or compatible Linux
- **Docker**: 24.0+
- **Docker Compose**: 2.0+
- **Git**: Latest version

### 1.3 External Services

- **Gemini API Key**: https://aistudio.google.com/app/apikey (FREE)
- **GitHub OAuth App**: https://github.com/settings/developers
- **DuckDNS Account**: https://www.duckdns.org/ (FREE)

---

## 2. Infrastructure Setup

### 2.1 Install Ubuntu Server

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install essential tools
sudo apt install -y curl wget git vim htop
```

### 2.2 Install Docker

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose-plugin -y

# Verify installation
docker --version
docker compose version

# Reboot to apply group changes
sudo reboot
```

### 2.3 Configure Firewall

```bash
# Enable UFW firewall
sudo ufw enable

# Allow SSH (if using remote access)
sudo ufw allow 22/tcp

# Allow HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Check status
sudo ufw status
```

### 2.4 Set Up DuckDNS

**Step 1: Create Account**
1. Go to https://www.duckdns.org/
2. Sign in with GitHub
3. Create subdomain: `codesage-yourname.duckdns.org`
4. Note your token

**Step 2: Install Updater**
```bash
# Create directory
mkdir ~/duckdns
cd ~/duckdns

# Create update script
cat > duck.sh << 'EOF'
#!/bin/bash
echo url="https://www.duckdns.org/update?domains=YOUR_DOMAIN&token=YOUR_TOKEN&ip=" | curl -k -o ~/duckdns/duck.log -K -
EOF

# Replace YOUR_DOMAIN and YOUR_TOKEN
nano duck.sh

# Make executable
chmod +x duck.sh

# Test
./duck.sh
cat duck.log  # Should show "OK"

# Add to crontab (update every 5 minutes)
(crontab -l 2>/dev/null; echo "*/5 * * * * ~/duckdns/duck.sh >/dev/null 2>&1") | crontab -
```

### 2.5 Configure Router Port Forwarding

**Required Port Forwards:**

| External Port | Internal Port | Protocol | Service |
|---------------|---------------|----------|---------|
| 80 | 80 | TCP | HTTP (Caddy) |
| 443 | 443 | TCP | HTTPS (Caddy) |

**Steps (varies by router):**
1. Find your PC's local IP: `ip addr show`
2. Access router admin (usually 192.168.1.1 or 192.168.0.1)
3. Find "Port Forwarding" or "Virtual Server" section
4. Add rules for ports 80 and 443 pointing to your PC's IP
5. Save and reboot router

---

## 3. Application Deployment

### 3.1 Clone Repository

```bash
# Create projects directory
mkdir -p ~/projects
cd ~/projects

# Clone repository
git clone https://github.com/yourusername/codesage-ai.git
cd codesage-ai
```

### 3.2 Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit environment file
nano .env
```

**Required Variables:**
```bash
# Gemini API (FREE)
GEMINI_API_KEY=your_gemini_api_key_here

# GitHub OAuth
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_WEBHOOK_SECRET=$(openssl rand -hex 32)

# Security
JWT_SECRET=$(openssl rand -hex 32)

# Database
DB_PASSWORD=strong_password_here

# Domain
API_URL=https://codesage-yourname.duckdns.org
FRONTEND_URL=https://codesage-yourname.duckdns.org

# DuckDNS
DUCKDNS_TOKEN=your_duckdns_token
DUCKDNS_DOMAIN=codesage-yourname.duckdns.org

# Monitoring
GRAFANA_PASSWORD=admin_password
```

### 3.3 Configure Caddy

Create `Caddyfile`:
```bash
cat > Caddyfile << 'EOF'
{$DUCKDNS_DOMAIN} {
    # Frontend
    handle / {
        reverse_proxy frontend:80
    }
    
    # Backend API
    handle /api/* {
        reverse_proxy backend:8080
    }
    
    # AI Service
    handle /ai/* {
        reverse_proxy ai-service:8000
    }
    
    # WebSocket
    handle /ws/* {
        reverse_proxy backend:8080
    }
    
    # Enable HTTPS (automatic with Let's Encrypt)
    tls {
        dns duckdns {env.DUCKDNS_TOKEN}
    }
    
    # Security headers
    header {
        Strict-Transport-Security "max-age=31536000;"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "no-referrer-when-downgrade"
    }
    
    # Logging
    log {
        output file /var/log/caddy/access.log
        format json
    }
}
EOF
```

### 3.4 Build and Deploy

```bash
# Build images
docker compose build

# Start services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f
```

**Expected Output:**
```
NAME                    STATUS              PORTS
codesage-postgres       Up (healthy)        5432/tcp
codesage-redis          Up (healthy)        6379/tcp
codesage-backend        Up                  8080/tcp
codesage-ai             Up                  8000/tcp
codesage-frontend       Up                  80/tcp
codesage-caddy          Up                  80/tcp, 443/tcp
codesage-prometheus     Up                  9090/tcp
codesage-grafana        Up                  3000/tcp
```

### 3.5 Verify Deployment

```bash
# Check health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health

# Check HTTPS (after DNS propagates)
curl https://codesage-yourname.duckdns.org

# Check logs for errors
docker compose logs backend | grep ERROR
docker compose logs ai-service | grep ERROR
```

---

## 4. Configuration

### 4.1 GitHub OAuth Setup

1. Go to https://github.com/settings/developers
2. Click "New OAuth App"
3. Fill in details:
   - **Application name**: CodeSage AI
   - **Homepage URL**: `https://codesage-yourname.duckdns.org`
   - **Authorization callback URL**: `https://codesage-yourname.duckdns.org/api/v1/auth/github/callback`
4. Click "Register application"
5. Note Client ID and generate Client Secret
6. Update `.env` file with credentials
7. Restart backend: `docker compose restart backend`

### 4.2 GitHub Webhook Setup

**For each repository:**
1. Go to repository Settings → Webhooks
2. Click "Add webhook"
3. Configure:
   - **Payload URL**: `https://codesage-yourname.duckdns.org/api/v1/webhooks/github`
   - **Content type**: `application/json`
   - **Secret**: Value from `GITHUB_WEBHOOK_SECRET` in `.env`
   - **Events**: Select "Pull requests"
4. Click "Add webhook"
5. Verify webhook is active (green checkmark)

### 4.3 SSL Certificate

Caddy automatically obtains SSL certificates from Let's Encrypt. Verify:

```bash
# Check Caddy logs
docker compose logs caddy | grep certificate

# Test HTTPS
curl -I https://codesage-yourname.duckdns.org
```

---

## 5. Monitoring & Maintenance

### 5.1 Access Monitoring Dashboards

**Prometheus:**
- URL: `http://your-pc-ip:9090`
- Metrics explorer and query interface

**Grafana:**
- URL: `http://your-pc-ip:3000`
- Default login: admin / (value from GRAFANA_PASSWORD)
- Import dashboards from `grafana/dashboards/`

### 5.2 Log Management

**View logs:**
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend

# Last 100 lines
docker compose logs --tail=100 backend

# Follow with timestamp
docker compose logs -f -t backend
```

**Log rotation:**
```bash
# Configure Docker log rotation
sudo nano /etc/docker/daemon.json
```

Add:
```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

```bash
# Restart Docker
sudo systemctl restart docker
```

### 5.3 Resource Monitoring

```bash
# Docker stats
docker stats

# System resources
htop

# Disk usage
df -h
docker system df
```

### 5.4 Automated Updates

**Create update script:**
```bash
cat > ~/update-codesage.sh << 'EOF'
#!/bin/bash
cd ~/projects/codesage-ai

# Pull latest changes
git pull

# Rebuild and restart
docker compose up -d --build

# Clean up old images
docker image prune -f

echo "Update complete!"
EOF

chmod +x ~/update-codesage.sh
```

---

## 6. Troubleshooting

### 6.1 Service Won't Start

**Check logs:**
```bash
docker compose logs service-name
```

**Common issues:**
- Port already in use: `sudo lsof -i :8080`
- Environment variable missing: Check `.env` file
- Database not ready: Wait for health check

### 6.2 Cannot Access via Domain

**Checklist:**
- [ ] DuckDNS updater running: `cat ~/duckdns/duck.log`
- [ ] Port forwarding configured correctly
- [ ] Firewall allows ports 80/443
- [ ] DNS propagated (can take 5-10 minutes)
- [ ] Caddy running: `docker compose ps caddy`

**Test DNS:**
```bash
nslookup codesage-yourname.duckdns.org
```

### 6.3 SSL Certificate Issues

**Check Caddy logs:**
```bash
docker compose logs caddy | grep -i error
```

**Common issues:**
- DuckDNS token incorrect
- Ports 80/443 not accessible from internet
- Rate limit from Let's Encrypt (wait 1 hour)

### 6.4 Database Connection Errors

```bash
# Check PostgreSQL
docker compose exec postgres psql -U codesage -c "SELECT 1;"

# Check connections
docker compose exec postgres psql -U codesage -c "SELECT count(*) FROM pg_stat_activity;"

# Restart database
docker compose restart postgres
```

### 6.5 High Memory Usage

```bash
# Check memory usage
docker stats

# Restart services
docker compose restart

# Increase swap if needed
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

---

## 7. Backup & Recovery

### 7.1 Backup Strategy

**What to backup:**
- PostgreSQL database
- Chroma vector database
- Configuration files (.env, Caddyfile)
- SSL certificates (Caddy data)

**Backup script:**
```bash
cat > ~/backup-codesage.sh << 'EOF'
#!/bin/bash
BACKUP_DIR=~/backups/codesage
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup database
docker compose exec -T postgres pg_dump -U codesage codesage > $BACKUP_DIR/db_$DATE.sql

# Backup Chroma data
docker compose cp ai-service:/app/chroma_data $BACKUP_DIR/chroma_$DATE

# Backup configuration
cp .env $BACKUP_DIR/env_$DATE
cp Caddyfile $BACKUP_DIR/Caddyfile_$DATE

# Compress
tar -czf $BACKUP_DIR/backup_$DATE.tar.gz $BACKUP_DIR/*_$DATE*

# Clean up old backups (keep last 7 days)
find $BACKUP_DIR -name "backup_*.tar.gz" -mtime +7 -delete

echo "Backup complete: $BACKUP_DIR/backup_$DATE.tar.gz"
EOF

chmod +x ~/backup-codesage.sh
```

**Schedule daily backups:**
```bash
# Add to crontab (daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * ~/backup-codesage.sh") | crontab -
```

### 7.2 Recovery Procedures

**Restore database:**
```bash
# Stop services
docker compose down

# Start only database
docker compose up -d postgres

# Wait for database to be ready
sleep 10

# Restore
cat backup_YYYYMMDD_HHMMSS/db_YYYYMMDD_HHMMSS.sql | \
  docker compose exec -T postgres psql -U codesage codesage

# Start all services
docker compose up -d
```

**Restore Chroma data:**
```bash
docker compose down ai-service
docker compose cp backup_YYYYMMDD_HHMMSS/chroma_YYYYMMDD_HHMMSS ai-service:/app/chroma_data
docker compose up -d ai-service
```

### 7.3 Disaster Recovery

**Complete system failure:**
1. Reinstall Ubuntu Server
2. Install Docker and Docker Compose
3. Clone repository
4. Restore `.env` from backup
5. Restore database from backup
6. Restore Chroma data from backup
7. Start services: `docker compose up -d`

**RTO (Recovery Time Objective):** 1 hour  
**RPO (Recovery Point Objective):** 24 hours (daily backups)

---

## 8. Performance Tuning

### 8.1 Database Optimization

```sql
-- Connect to database
docker compose exec postgres psql -U codesage codesage

-- Analyze tables
ANALYZE;

-- Check slow queries
SELECT query, mean_exec_time, calls 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 10;

-- Vacuum
VACUUM ANALYZE;
```

### 8.2 Redis Optimization

```bash
# Check memory usage
docker compose exec redis redis-cli INFO memory

# Set max memory
docker compose exec redis redis-cli CONFIG SET maxmemory 2gb
docker compose exec redis redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

### 8.3 Application Tuning

**Backend (application.yml):**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
```

**AI Service (config.py):**
```python
MAX_GEMINI_REQUESTS_PER_DAY = 1400
MAX_CONCURRENT_ANALYSES = 5
```

---

## 9. Security Hardening

### 9.1 System Security

```bash
# Enable automatic security updates
sudo apt install unattended-upgrades -y
sudo dpkg-reconfigure --priority=low unattended-upgrades

# Install fail2ban
sudo apt install fail2ban -y
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

### 9.2 Application Security

**Update secrets regularly:**
```bash
# Generate new JWT secret
openssl rand -hex 32

# Update .env
nano .env

# Restart services
docker compose restart backend
```

**Review access logs:**
```bash
# Check for suspicious activity
docker compose logs caddy | grep -i "40[13]"
```

---

## 10. Scaling Considerations

### 10.1 Vertical Scaling
- Increase RAM allocation
- Add more CPU cores
- Upgrade to SSD storage

### 10.2 Horizontal Scaling
- Add load balancer (HAProxy/Nginx)
- Run multiple backend instances
- Use external PostgreSQL (managed service)
- Implement message queue (RabbitMQ)

---

## 11. Maintenance Checklist

### Daily
- [ ] Check service status: `docker compose ps`
- [ ] Review error logs
- [ ] Monitor disk space: `df -h`

### Weekly
- [ ] Review Grafana dashboards
- [ ] Check backup success
- [ ] Update system packages: `sudo apt update && sudo apt upgrade`
- [ ] Clean Docker: `docker system prune -f`

### Monthly
- [ ] Review and rotate logs
- [ ] Test backup restoration
- [ ] Review security updates
- [ ] Check SSL certificate expiry
- [ ] Performance review

---

## 12. Useful Commands

```bash
# Start services
docker compose up -d

# Stop services
docker compose down

# Restart service
docker compose restart backend

# View logs
docker compose logs -f backend

# Execute command in container
docker compose exec backend bash

# Check resource usage
docker stats

# Clean up
docker system prune -a

# Update and rebuild
git pull && docker compose up -d --build
```

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-24  
**Maintained By:** DevOps Team
