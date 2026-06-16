# 07 - cliente solicita troca/devolucao de um item do pedido
# pega compra_id (ja ENTREGUE do teste 06)
# salva: troca_id

from playwright.sync_api import Page, expect
from helpers import *
from estado import salvar, carregar


def test_cliente_solicita_troca(page: Page):
    """cliente solicita troca de item do pedido entregue via UI."""
    email = carregar("cliente_email")
    senha = carregar("cliente_senha")
    compra_id = carregar("compra_id")

    # login do cliente
    page.goto("/#login")
    login_usuario(page, email, senha)
    page.wait_for_timeout(2000)

    # vai para pedidos e localiza a compra correta pelo ID
    page.goto("/#orders")
    page.wait_for_timeout(3000)
    page.reload()
    page.wait_for_selector(".order-card", timeout=15000)
    order_card = page.locator(".order-card", has=page.locator(f"text=Pedido #{compra_id}"))
    expect(order_card.locator(".badge-status")).to_contain_text("Entregue")

    # expande detalhes da compra correta
    order_card.locator('button:has-text("Detalhes")').click()
    page.wait_for_timeout(1000)

    # clica em solicitar troca
    page.locator(f'button[onclick="showExchangeForm({compra_id})"]').click()
    page.wait_for_selector(f'#exchange-form-{compra_id} form', timeout=5000)

    # seleciona o primeiro item do formulario de troca desta compra
    exchange_form = page.locator(f'#exchange-form-{compra_id}')
    exchange_form.locator('input[type="checkbox"]').first.check()
    exchange_form.locator('textarea[name="motivo"]').fill("Produto com defeito - solicito troca")
    exchange_form.locator('button[type="submit"]:has-text("Solicitar Troca")').click()
    expect(page.locator(".toast").last).to_contain_text("Troca solicitada com sucesso", timeout=10000)

    # verifica status mudou na compra correta
    page.wait_for_timeout(2000)
    expect(order_card.locator(".badge-status")).to_contain_text("Em Troca")

    # busca o ID da troca via API
    token = cliente_login(email, senha)
    admin_token = admin_login()
    trocas = api_get(admin_token, "/api/trocas")
    troca = next(t for t in trocas if t.get("compra", {}).get("id") == compra_id)

    salvar("troca_id", troca["id"])
