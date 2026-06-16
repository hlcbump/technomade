# configuração global do playwright pra todos os testes e2e
import pytest

@pytest.fixture(scope="session")
def browser_context_args():
    return {"base_url": "http://localhost:8000"}
