from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    ENVIRONMENT: str = "native"
    DEBUG: bool = True
    VERSION: str = "1.0.0"
    PROJECT_NAME: str = "Gogidix AI Services"

    class Config:
        env_file = ".env.native"

def get_settings() -> Settings:
    return Settings()

# Create global instance
settings = Settings()