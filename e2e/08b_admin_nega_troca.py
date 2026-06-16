# 08b - administrador NEGA a troca/devolucao
# fluxo alternativo: compra -> entrega -> solicita troca -> admin nega via painel

from playwright.sync_api import Page, expect
from helpers import *

USUARIO_NEGA = {
    "nome": f"Cliente TrocaNegada {TIMESTAMP}",
    "email": f"e2e.negatroca.{TIMESTAMP}@test.com",
    "senha": "Teste@123",
    "genero": "FEMININO",
    "data_nascimento": "1994-08-12",
}


def test_admin_nega_troca(page: Page):
    """admin recusa a troca solicitada pelo cliente via painel admin."""
    # cadastro e compra do cliente
    registrar_usuario(page, USUARIO_NEGA)
    login_usuario(page, USUARIO_NEGA["email"], USUARIO_NEGA["senha"])
    adicionar_produto_carrinho(page)
    ir_para_checkout(page)
    preencher_endereco(page)
    preencher_cartao(page)
    finalizar_compra_com_cartao(page)

    page.wait_for_selector(".order-card", timeout=15000)

    # admin leva ate ENTREGUE via API (pre-requisito)
    admin_token = admin_login()
    cliente_token = cliente_login(USUARIO_NEGA["email"], USUARIO_NEGA["senha"])
    compras_cliente = api_get(cliente_token, "/api/compras")
    compra = max(compras_cliente, key=lambda c: c["id"])
    compra_id = compra["id"]

    api_put(admin_token, f"/api/compras/{compra_id}/status", {"status": "APROVADA"})
    api_put(admin_token, f"/api/compras/{compra_id}/status", {"status": "EM_TRANSITO"})
    api_put(admin_token, f"/api/compras/{compra_id}/status", {"status": "ENTREGUE"})

    # cliente solicita troca via API
    compras_cliente = api_get(cliente_token, "/api/compras")
    compra_entregue = next(c for c in compras_cliente if c["id"] == compra_id)
    itens = compra_entregue.get("itens", [])

    api_post(cliente_token, "/api/trocas", {
        "compraId": compra_id,
        "motivo": "Nao gostei do produto",
        "itens": [{"produtoId": itens[0]["produto"]["id"], "quantidade": 1}],
    })

    # busca o ID da troca criada
    trocas = api_get(admin_token, "/api/trocas")
    troca = next(t for t in trocas if t.get("compra", {}).get("id") == compra_id)
    troca_id = troca["id"]

    # admin loga na UI e nega a troca pelo painel
    page.goto("/#login")
    page.wait_for_selector("#login-form")
    page.fill('#login-form input[name="email"]', ADMIN_EMAIL)
    page.fill('#login-form input[name="senha"]', ADMIN_SENHA)
    page.click('#login-form button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Login realizado com sucesso", timeout=10000)

    # navega pro painel admin
    page.goto("/#admin")
    page.wait_for_selector(".admin-tabs", timeout=10000)

    # clica na aba "Trocas"
    page.click('button:has-text("Trocas")')
    page.wait_for_timeout(3000)

    # aceita o prompt() do motivo automaticamente
    page.on("dialog", lambda dialog: dialog.accept("Produto fora do prazo de troca"))

    # clica no botao "Negar" da troca especifica
    page.locator(f'button[onclick="denyTroca({troca_id})"]').click()
    expect(page.locator(".toast").last).to_contain_text("Troca negada", timeout=10000)

    # verifica que o status mudou
    page.wait_for_timeout(2000)
    expect(page.locator(".badge-status.status-rejected").first).to_be_visible()
