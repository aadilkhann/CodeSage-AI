# CodeSage AI

AI-powered code review assistant that learns from your repository's history and provides context-aware suggestions.

## 🎯 Features

- **Automated PR Analysis**: Analyzes pull requests automatically via GitHub webhooks
- **Context-Aware Suggestions**: Learns from approved PRs to understand team-specific patterns
- **Real-time Updates**: WebSocket integration for live analysis progress
- **Zero Cost**: Uses Gemini 2.5 Flash API (free) and self-hosted infrastructure

## 🏗️ Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   React     │────▶│ Spring Boot  │────▶│   FastAPI   │
│  Frontend   │     │   Backend    │     │ AI Service  │
└─────────────┘     └──────────────┘     └─────────────┘
                           │                     │
                           ▼                     ▼
                    ┌──────────────┐     ┌─────────────┐
                    │  PostgreSQL  │     │   Chroma    │
                    │   + Redis    │     │  Vector DB  │
                    └──────────────┘     └─────────────┘
```

## 🚀 Tech Stack

### Backend
- **Framework**: Spring Boot 3.2.x
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Cache**: Redis 7

### AI Service
- **Framework**: FastAPI
- **Language**: Python 3.11
- **LLM**: Google Gemini 2.5 Flash (FREE)
- **Vector DB**: Chroma (self-hosted)

### Frontend
- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **State**: Redux Toolkit
- **UI**: TailwindCSS

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Reverse Proxy**: Caddy (auto HTTPS)
- **Monitoring**: Prometheus + Grafana

## 📋 Prerequisites

- Docker & Docker Compose
- Java 17+
- Node.js 18+
- Python 3.11+
- Gemini API Key (free from https://aistudio.google.com/app/apikey)
- GitHub OAuth App credentials

## 🏃 Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/codesage-ai.git
cd codesage-ai
```

### 2. Set Up Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your credentials
nano .env
```

### 3. Start Services

```bash
# Start all services with Docker Compose
docker compose up -d

# View logs
docker compose logs -f
```

### 4. Access Application

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **AI Service**: http://localhost:8000/docs
- **Grafana**: http://localhost:3000

## 📚 Documentation

- [Quick Start Guide](docs/quick-start.md) - Get started in 30 minutes
- [Implementation Plan](docs/implementation-plan.md) - Detailed development guide
- [API Documentation](docs/api-docs.md) - API reference
- [Self-Hosting Guide](docs/self-hosting.md) - Deploy on your spare PC

## 🛠️ Development

### Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

### Frontend (React)

```bash
cd frontend
npm install
npm run dev
```

### AI Service (FastAPI)

```bash
cd ai-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

## 🧪 Testing

```bash
# Backend tests
cd backend && mvn test

# Frontend tests
cd frontend && npm test

# AI service tests
cd ai-service && pytest
```

## 📊 Project Status

- ✅ Planning & Architecture Complete
- 🚧 Implementation In Progress
- ⏳ Testing & Deployment Pending

## 💰 Cost

**Total Monthly Cost: $0**

- Gemini 2.5 Flash API: FREE (1,500 requests/day)
- Chroma Vector DB: FREE (self-hosted)
- Hosting: FREE (self-hosted on spare PC)

## 🤝 Contributing

This is a portfolio project, but suggestions and feedback are welcome!

## 📝 License

MIT License - See [LICENSE](LICENSE) file for details

## 🙏 Acknowledgments

- Google Gemini for free AI API
- Spring Boot & FastAPI communities
- Open source contributors

## 📧 Contact

- **Author**: Your Name
- **Email**: your.email@example.com
- **LinkedIn**: [Your Profile](https://linkedin.com/in/yourprofile)
- **Portfolio**: [Your Website](https://yourwebsite.com)

---

**Built with ❤️ for learning and portfolio demonstration**
