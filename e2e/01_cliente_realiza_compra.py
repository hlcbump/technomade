# 01 - cliente realiza compra com cartao de credito
# registra endereco e cartao no ato da compra
# salva: email, senha, compra_id

from playwright.sync_api import Page, expect
from helpers import *
from estado import salvar, limpar


USUARIO = {
    "nome": f"Cliente E2E {TIMESTAMP}",
    "email": f"e2e.venda.{TIMESTAMP}@test.com",
    "senha": "Teste@123",
    "genero": "MASCULINO",
    "data_nascimento": "1990-01-15",
}


def test_cliente_realiza_compra(page: Page):
    """cliente cadastra, loga, adiciona produto, cria endereco+cartao no checkout e finaliza."""
    # limpa estado de execucoes anteriores
    limpar()

    # cadastro
    registrar_usuario(page, USUARIO)
    login_usuario(page, USUARIO["email"], USUARIO["senha"])

    # adiciona produto ao carrinho
    adicionar_produto_carrinho(page)

    # checkout
    ir_para_checkout(page)
    preencher_endereco(page, estado="SP")
    preencher_cartao(page)
    finalizar_compra_com_cartao(page)

    # verifica pedido criado com status EM_PROCESSAMENTO
    page.wait_for_selector(".order-card", timeout=15000)
    expect(page.locator(".badge-status").first).to_contain_text("Em Processamento")

    # pega o ID da compra via API pra passar pros proximos testes
    token = cliente_login(USUARIO["email"], USUARIO["senha"])
    compras = api_get(token, f"/api/compras?usuarioId=0")
    # como o endpoint sem admin retorna as do proprio usuario:
    compras = api_get(token, "/api/compras")
    compra_id = max(c["id"] for c in compras)

    # salva estado para os proximos testes
    salvar("cliente_email", USUARIO["email"])
    salvar("cliente_senha", USUARIO["senha"])
    salvar("compra_id", compra_id)
    salvar("cliente_token", token)
