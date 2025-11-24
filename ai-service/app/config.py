from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # Gemini API Configuration
    gemini_api_key: str
    gemini_model: str = "gemini-2.0-flash-exp"
    gemini_temperature: float = 0.1
    gemini_max_output_tokens: int = 8192
    
    # Database Configuration
    database_url: str = "postgresql://codesage:devpassword@localhost:5432/codesage"
    
    # Chroma Vector Database
    chroma_persist_dir: str = "./chroma_data"
    chroma_collection_name: str = "code_patterns"
    
    # Application Settings
    environment: str = "development"
    log_level: str = "INFO"
    debug: bool = False
    
    # Rate Limiting
    max_gemini_requests_per_day: int = 1400
    max_gemini_requests_per_minute: int = 60
    
    # CORS Settings
    cors_origins: list[str] = [
        "http://localhost:5173",
        "http://localhost:3000",
        "http://localhost:8080"
    ]
    
    # API Settings
    api_v1_prefix: str = "/api/v1"
    
    class Config:
        env_file = ".env"
        case_sensitive = False


# Global settings instance
settings = Settings()
