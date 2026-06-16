# 02 - cliente paga com N cartoes e compra N produtos
# testa: N cartoes dividindo o valor + N produtos de todos disponiveis
# salva: compra_ncartoes_id

from playwright.sync_api import Page, expect
from helpers import *
from estado import salvar, carregar


NUM_CARTOES = 4
NUM_PRODUTOS = 3

BANDEIRAS = ["VISA", "MASTERCARD", "ELO", "AMEX", "HIPERCARD", "DINERS"]

USUARIO2 = {
    "nome": f"Cliente NCartoes {TIMESTAMP}",
    "email": f"e2e.ncartoes.{TIMESTAMP}@test.com",
    "senha": "Teste@123",
    "genero": "FEMININO",
    "data_nascimento": "1992-05-20",
}


def test_compra_com_n_cartoes_e_n_produtos(page: Page):
    """cliente adiciona N produtos e divide pagamento entre N cartoes."""
    registrar_usuario(page, USUARIO2)
    login_usuario(page, USUARIO2["email"], USUARIO2["senha"])

    # adiciona N produtos diferentes (um produto de N produtos)
    adicionar_n_produtos_carrinho(page, quantidade=NUM_PRODUTOS)

    ir_para_checkout(page)
    preencher_endereco(page)

    # cria N cartoes com bandeiras diferentes
    for i in range(NUM_CARTOES):
        bandeira = BANDEIRAS[i % len(BANDEIRAS)]
        if i == 0:
            preencher_cartao(page)
        else:
            preencher_cartao_extra(page, nome=f"CARTAO {i+1}", bandeira=bandeira)

    # pega valor total e divide entre N cartoes
    valor_total = obter_valor_total_checkout(page)

    for i in range(NUM_CARTOES):
        if i < NUM_CARTOES - 1:
            # cada cartao paga uma parcela igual (min R$10)
            valor = max(round(valor_total / NUM_CARTOES, 2), 10.0)
        else:
            # ultimo cartao paga o restante
            pago = max(round(valor_total / NUM_CARTOES, 2), 10.0) * (NUM_CARTOES - 1)
            valor = round(valor_total - pago, 2)

        adicionar_cartao_checkout(page, valor)

    # confirma compra
    page.click('button:has-text("Confirmar Compra")')
    expect(page.locator(".toast").last).to_contain_text("Compra realizada com sucesso", timeout=15000)

    page.wait_for_selector(".order-card", timeout=15000)
    expect(page.locator(".badge-status").first).to_contain_text("Em Processamento")

    # salva ID
    token = cliente_login(USUARIO2["email"], USUARIO2["senha"])
    compras = api_get(token, "/api/compras")
    compra_id = max(c["id"] for c in compras)
    salvar("compra_ncartoes_id", compra_id)
