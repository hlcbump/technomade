# estado compartilhado entre os testes sequenciais
# cada teste grava aqui os IDs que o proximo precisa

import json
import os

ARQUIVO = os.path.join(os.path.dirname(__file__), ".estado_e2e.json")


def salvar(chave: str, valor):
    """salva um valor no estado compartilhado."""
    dados = carregar_tudo()
    dados[chave] = valor
    with open(ARQUIVO, "w") as f:
        json.dump(dados, f, indent=2)


def carregar(chave: str):
    """carrega um valor do estado compartilhado."""
    dados = carregar_tudo()
    assert chave in dados, f"Chave '{chave}' nao encontrada no estado. Rode os testes anteriores primeiro."
    return dados[chave]


def carregar_tudo() -> dict:
    """carrega todo o estado."""
    if not os.path.exists(ARQUIVO):
        return {}
    with open(ARQUIVO) as f:
        return json.load(f)


def limpar():
    """limpa o estado (chamar no inicio da suite)."""
    if os.path.exists(ARQUIVO):
        os.remove(ARQUIVO)
