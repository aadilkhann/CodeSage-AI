# CodeSage AI - Documentation

Welcome to the CodeSage AI documentation! This directory contains comprehensive documentation for the AI-powered code review assistant project.

---

## 📚 Documentation Index

### Core Documents

| Document | Description | Audience |
|----------|-------------|----------|
| [REQUIREMENTS.md](REQUIREMENTS.md) | Product Requirements Document (PRD) | Product, Dev, Stakeholders |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System Architecture & Design | Developers, Architects |
| [PLANNING.md](PLANNING.md) | Project Planning & Timeline | Project Managers, Developers |
| [API.md](API.md) | REST API Reference | Frontend Developers, Integrators |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Deployment & Operations Guide | DevOps, System Administrators |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Development Setup & Workflow | Developers |

---

## 🎯 Quick Navigation

### For New Developers
1. Start with [DEVELOPMENT.md](DEVELOPMENT.md) - Set up your environment
2. Review [ARCHITECTURE.md](ARCHITECTURE.md) - Understand the system
3. Check [PLANNING.md](PLANNING.md) - See current progress
4. Reference [API.md](API.md) - API endpoints

### For DevOps/Deployment
1. Read [DEPLOYMENT.md](DEPLOYMENT.md) - Complete deployment guide
2. Review [ARCHITECTURE.md](ARCHITECTURE.md) - Infrastructure overview
3. Check [REQUIREMENTS.md](REQUIREMENTS.md) - Non-functional requirements

### For Product/Planning
1. Review [REQUIREMENTS.md](REQUIREMENTS.md) - Features and requirements
2. Check [PLANNING.md](PLANNING.md) - Timeline and milestones
3. See [ARCHITECTURE.md](ARCHITECTURE.md) - Technical capabilities

---

## 📖 Document Summaries

### REQUIREMENTS.md
**Product Requirements Document**
- Executive summary and problem statement
- User personas and use cases
- Functional requirements (FR-1 through FR-10)
- Non-functional requirements (NFR-1 through NFR-30)
- User stories and acceptance criteria
- Success metrics and KPIs
- Risk management
- Release criteria

### ARCHITECTURE.md
**System Architecture Document**
- High-level architecture diagrams
- Component design (Frontend, Backend, AI Service)
- Database schema (PostgreSQL, Redis, Chroma)
- API design and data flow
- Security architecture
- Deployment architecture
- Technology stack details
- Scalability considerations

### PLANNING.md
**Project Planning & Implementation Guide**
- Project timeline (7-10 weeks)
- Phase 1: MVP (Weeks 1-3)
- Phase 2: Enhanced Features (Weeks 4-6)
- Phase 3: Polish & Launch (Weeks 7-10)
- Detailed task breakdown by day
- Resource planning
- Risk management
- Quality assurance strategy

### API.md
**REST API Reference**
- Authentication endpoints
- Repository management
- Pull request operations
- Analysis and suggestions
- Analytics endpoints
- WebSocket API
- Error handling
- Rate limiting

### DEPLOYMENT.md
**Deployment & Operations Guide**
- Infrastructure setup (Ubuntu, Docker)
- DuckDNS configuration
- Application deployment
- SSL/HTTPS setup
- Monitoring and logging
- Backup and recovery
- Troubleshooting guide
- Maintenance procedures

### DEVELOPMENT.md
**Development Setup & Workflow**
- Project structure overview
- Local development setup
- Backend development (Spring Boot)
- AI service development (FastAPI)
- Frontend development (React)
- Docker development workflow
- Testing strategies
- Useful commands

---

## 🔍 Finding Information

### Common Questions

**"How do I set up my development environment?"**
→ See [DEVELOPMENT.md](DEVELOPMENT.md) - Getting Started section

**"What are the API endpoints?"**
→ See [API.md](API.md) - Complete API reference

**"How do I deploy to production?"**
→ See [DEPLOYMENT.md](DEPLOYMENT.md) - Application Deployment section

**"What's the database schema?"**
→ See [ARCHITECTURE.md](ARCHITECTURE.md) - Data Architecture section

**"What features are we building?"**
→ See [REQUIREMENTS.md](REQUIREMENTS.md) - Functional Requirements section

**"What's the project timeline?"**
→ See [PLANNING.md](PLANNING.md) - Timeline & Milestones section

**"How does the system work?"**
→ See [ARCHITECTURE.md](ARCHITECTURE.md) - System Architecture section

---

## 📊 Project Status

**Current Phase:** Week 1, Day 3 - Database Implementation  
**Overall Progress:** 15% Complete  
**Next Milestone:** Database Schema Complete (Day 4)

See [PLANNING.md](PLANNING.md) for detailed progress tracking.

---

## 🛠️ Technology Stack

### Backend
- **Framework:** Spring Boot 3.2
- **Language:** Java 17
- **Database:** PostgreSQL 15
- **Cache:** Redis 7

### AI Service
- **Framework:** FastAPI
- **Language:** Python 3.11
- **LLM:** Google Gemini 2.5 Flash (FREE)
- **Vector DB:** Chroma

### Frontend
- **Framework:** React 18
- **Language:** TypeScript
- **Build:** Vite
- **Styling:** TailwindCSS

### Infrastructure
- **Containerization:** Docker + Docker Compose
- **Reverse Proxy:** Caddy (Auto HTTPS)
- **Monitoring:** Prometheus + Grafana
- **DNS:** DuckDNS (FREE)

---

## 💰 Cost Structure

**Total Monthly Cost: $0**

All services use free tiers or self-hosting:
- Gemini API: FREE (1,500 requests/day)
- Chroma Vector DB: FREE (self-hosted)
- PostgreSQL: FREE (self-hosted)
- Redis: FREE (self-hosted)
- Hosting: FREE (spare PC)
- DuckDNS: FREE
- SSL Certificates: FREE (Let's Encrypt)

---

## 📝 Document Maintenance

### Updating Documentation

**When to Update:**
- Feature additions/changes
- Architecture modifications
- API endpoint changes
- Deployment procedure updates
- New requirements

**How to Update:**
1. Edit the relevant markdown file
2. Update version number if major changes
3. Update "Last Updated" date
4. Commit with descriptive message
5. Update this README if adding new docs

### Document Owners

| Document | Owner | Last Updated |
|----------|-------|--------------|
| REQUIREMENTS.md | Product Team | 2025-11-24 |
| ARCHITECTURE.md | Architecture Team | 2025-11-24 |
| PLANNING.md | Project Team | 2025-11-24 |
| API.md | Backend Team | 2025-11-24 |
| DEPLOYMENT.md | DevOps Team | 2025-11-24 |
| DEVELOPMENT.md | Dev Team | 2025-11-24 |

---

## 🤝 Contributing

When contributing to documentation:

1. **Be Clear:** Use simple, direct language
2. **Be Specific:** Include examples and code snippets
3. **Be Consistent:** Follow existing formatting
4. **Be Complete:** Cover all aspects of the topic
5. **Be Current:** Keep information up to date

### Markdown Guidelines

- Use headers for structure (# ## ###)
- Use code blocks with language specification
- Use tables for structured data
- Use lists for sequential items
- Use links for cross-references
- Use diagrams for complex concepts

---

## 📞 Support

For questions about documentation:
1. Check the relevant document first
2. Search for keywords
3. Review related documents
4. Create an issue if information is missing

---

## 🔗 External Resources

### Official Documentation
- [Spring Boot](https://spring.io/projects/spring-boot)
- [FastAPI](https://fastapi.tiangolo.com/)
- [React](https://react.dev/)
- [Gemini API](https://ai.google.dev/docs)
- [Docker](https://docs.docker.com/)

### Learning Resources
- [GitHub REST API](https://docs.github.com/en/rest)
- [PostgreSQL](https://www.postgresql.org/docs/)
- [Redis](https://redis.io/documentation)
- [Chroma](https://docs.trychroma.com/)

---

## 📄 License

This documentation is part of the CodeSage AI project and is licensed under the MIT License.

---

**Last Updated:** 2025-11-24  
**Documentation Version:** 1.0  
**Project Version:** 1.0.0-SNAPSHOT
