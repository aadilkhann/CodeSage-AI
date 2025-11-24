# CodeSage AI - Development Guide

## Project Structure

```
codesage-ai/
├── backend/              # Spring Boot backend service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/codesage/
│   │   │   │   ├── config/       # Configuration classes
│   │   │   │   ├── controller/   # REST controllers
│   │   │   │   ├── service/      # Business logic
│   │   │   │   ├── repository/   # Data access
│   │   │   │   ├── model/        # JPA entities
│   │   │   │   ├── security/     # Security & OAuth
│   │   │   │   └── dto/          # Data transfer objects
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/ # Flyway migrations
│   │   └── test/
│   ├── pom.xml
│   └── Dockerfile
│
├── ai-service/           # FastAPI AI service
│   ├── app/
│   │   ├── routers/      # API endpoints
│   │   ├── services/     # Business logic
│   │   ├── models/       # Pydantic models
│   │   ├── utils/        # Utilities
│   │   ├── config.py     # Configuration
│   │   └── main.py       # Application entry
│   ├── tests/
│   ├── requirements.txt
│   └── Dockerfile
│
├── frontend/             # React frontend
│   ├── src/
│   │   ├── components/   # React components
│   │   ├── features/     # Feature modules
│   │   ├── hooks/        # Custom hooks
│   │   ├── services/     # API clients
│   │   ├── store/        # Redux store
│   │   ├── types/        # TypeScript types
│   │   └── utils/        # Utilities
│   ├── public/
│   ├── package.json
│   └── Dockerfile
│
├── docs/                 # Documentation
├── scripts/              # Helper scripts
├── .github/workflows/    # CI/CD workflows
├── docker-compose.yml    # Docker orchestration
├── prometheus.yml        # Monitoring config
└── README.md
```

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17+ (for local backend development)
- Node.js 18+ (for local frontend development)
- Python 3.11+ (for local AI service development)

### Quick Start

1. **Clone and setup**:
   ```bash
   git clone <your-repo-url>
   cd codesage-ai
   cp .env.example .env
   # Edit .env with your API keys
   ```

2. **Get API Keys**:
   - Gemini API: https://aistudio.google.com/app/apikey
   - GitHub OAuth: https://github.com/settings/developers

3. **Start services**:
   ```bash
   ./scripts/start.sh
   ```

## Development Workflow

### Backend Development (Spring Boot)

```bash
cd backend

# Run with Maven
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build
./mvnw clean package
```

**Key files to start with**:
- `CodeSageApplication.java` - Main application
- `application.yml` - Configuration
- `config/` - Add your configuration classes
- `controller/` - Add REST endpoints

### AI Service Development (FastAPI)

```bash
cd ai-service

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run development server
uvicorn app.main:app --reload

# Run tests
pytest

# Format code
black app/
```

**Key files to start with**:
- `app/main.py` - FastAPI application
- `app/config.py` - Configuration
- `app/routers/` - Add API endpoints
- `app/services/` - Add business logic

### Frontend Development (React)

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev

# Run tests
npm test

# Build for production
npm run build

# Lint
npm run lint
```

**Key files to start with**:
- `src/App.tsx` - Main component
- `src/main.tsx` - Entry point
- `src/components/` - Add components
- `src/services/` - Add API clients

## Docker Development

### Start all services:
```bash
docker compose up --build
```

### Start specific service:
```bash
docker compose up backend
docker compose up ai-service
docker compose up frontend
```

### View logs:
```bash
docker compose logs -f
docker compose logs -f backend
```

### Restart service:
```bash
docker compose restart backend
```

### Stop all services:
```bash
docker compose down
```

## Database Management

### Access PostgreSQL:
```bash
docker compose exec postgres psql -U codesage -d codesage
```

### Run migrations:
```bash
# Migrations run automatically on backend startup
# Check status:
cd backend && ./mvnw flyway:info
```

### Create new migration:
```bash
# Create file: backend/src/main/resources/db/migration/V2__description.sql
```

## Testing

### Backend Tests:
```bash
cd backend
./mvnw test
./mvnw test -Dtest=ClassName
```

### AI Service Tests:
```bash
cd ai-service
pytest
pytest tests/test_file.py
pytest --cov=app
```

### Frontend Tests:
```bash
cd frontend
npm test
npm run test:coverage
```

## Monitoring

Access monitoring dashboards:
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## Troubleshooting

### Port conflicts:
```bash
# Check what's using a port
lsof -i :8080
# Kill process
kill -9 <PID>
```

### Docker issues:
```bash
# Clean up
docker compose down -v
docker system prune -a

# Rebuild
docker compose up --build --force-recreate
```

### Database issues:
```bash
# Reset database
docker compose down -v
docker compose up postgres
```

## Next Steps

1. **Implement Database Schema** (Day 3-4)
   - Create Flyway migrations in `backend/src/main/resources/db/migration/`
   - Create JPA entities in `backend/src/main/java/com/codesage/model/`

2. **Implement GitHub OAuth** (Day 5-7)
   - Add security configuration
   - Create auth controllers
   - Implement JWT service

3. **Implement AI Service** (Day 11-14)
   - Add Gemini service
   - Implement RAG with Chroma
   - Create analysis endpoints

4. **Build Frontend** (Day 15-17)
   - Create dashboard layout
   - Add authentication
   - Build PR analysis view

## Useful Commands

```bash
# Generate JWT secret
openssl rand -hex 32

# Generate webhook secret
openssl rand -hex 32

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health

# View Docker stats
docker stats

# Clean logs
rm -rf backend/logs/*
```

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [React Documentation](https://react.dev/)
- [Gemini API Documentation](https://ai.google.dev/docs)
- [Docker Documentation](https://docs.docker.com/)

## Support

For issues or questions:
1. Check the documentation in `docs/`
2. Review the implementation plan
3. Check existing issues
4. Create a new issue with details

---

**Happy Coding! 🚀**
