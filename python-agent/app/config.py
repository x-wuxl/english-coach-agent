from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # LLM defaults
    llm_default_model: str = "deepseek/deepseek-chat"
    llm_api_key: str = ""
    llm_api_base: str = ""

    # Per-task model overrides (fall back to llm_default_model)
    llm_coach_model: str = ""
    llm_explanation_model: str = ""
    llm_reflection_model: str = ""

    # Fallback model (used when primary fails)
    llm_fallback_model: str = ""

    # LiteLLM settings
    llm_timeout: int = 30
    llm_max_retries: int = 2

    # Tracing
    tracing_enabled: bool = True

    # Harness
    harness_record_dir: str = "harness_records"

    model_config = {"env_prefix": "", "env_file": ".env", "env_file_encoding": "utf-8", "extra": "ignore"}

    @property
    def coach_model(self) -> str:
        return self.llm_coach_model or self.llm_default_model

    @property
    def explanation_model(self) -> str:
        return self.llm_explanation_model or self.llm_default_model

    @property
    def reflection_model(self) -> str:
        return self.llm_reflection_model or self.llm_default_model


settings = Settings()
