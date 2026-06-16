# 03 - cliente paga com N cupons + N cartoes de credito
# testa: 1 cupom promocional + N cupons de troca + N cartoes
# RN0033: max 1 cupom promocional por compra
# salva: compra_cupom_id

from playwright.sync_api import Page, expect
from helpers import *
from estado import salvar


NUM_CUPONS_TROCA = 2
NUM_CARTOES = 2

BANDEIRAS = ["VISA", "MASTERCARD", "ELO", "AMEX", "HIPERCARD", "DINERS"]

CUPOM_PROMO = f"PROMO{TIMESTAMP}"
CUPONS_TROCA = [f"TROCA{TIMESTAMP}{i}" for i in range(NUM_CUPONS_TROCA)]

USUARIO3 = {
    "nome": f"Cliente NCupons {TIMESTAMP}",
    "email": f"e2e.ncupons.{TIMESTAMP}@test.com",
    "senha": "Teste@123",
    "genero": "MASCULINO",
    "data_nascimento": "1988-03-10",
}


def test_compra_com_n_cupons_e_n_cartoes(page: Page):
    """cliente usa 1 cupom promocional + N cupons de troca + N cartoes."""

    # admin cria cupons: 1 promocional + N de troca
    admin_token = admin_login()
    criar_cupom_via_api(admin_token, CUPOM_PROMO, 15.0, promocional=True)
    for codigo in CUPONS_TROCA:
        criar_cupom_via_api(admin_token, codigo, 10.0, promocional=False)

    # cadastro e login
    registrar_usuario(page, USUARIO3)
    login_usuario(page, USUARIO3["email"], USUARIO3["senha"])

    # adiciona multiplos produtos pra garantir valor suficiente
    adicionar_n_produtos_carrinho(page, quantidade=3)

    ir_para_checkout(page)
    preencher_endereco(page)

    # cria N cartoes
    for i in range(NUM_CARTOES):
        bandeira = BANDEIRAS[i % len(BANDEIRAS)]
        if i == 0:
            preencher_cartao(page)
        else:
            preencher_cartao_extra(page, nome=f"CARTAO {i+1}", bandeira=bandeira)

    # aplica cupom promocional
    aplicar_cupom(page, CUPOM_PROMO)

    # aplica N cupons de troca
    for codigo in CUPONS_TROCA:
        aplicar_cupom(page, codigo)

    # pega o valor restante apos descontos dos cupons
    valor_total = obter_valor_total_checkout(page)

    # se ainda restar valor, divide entre N cartoes
    if valor_total > 0:
        for i in range(NUM_CARTOES):
            if i < NUM_CARTOES - 1:
                valor = max(round(valor_total / NUM_CARTOES, 2), 10.0)
            else:
                pago = max(round(valor_total / NUM_CARTOES, 2), 10.0) * (NUM_CARTOES - 1)
                valor = round(valor_total - pago, 2)

            adicionar_cartao_checkout(page, valor)

    # confirma compra
    page.click('button:has-text("Confirmar Compra")')
    expect(page.locator(".toast").last).to_contain_text("Compra realizada com sucesso", timeout=15000)

    page.wait_for_selector(".order-card", timeout=15000)
    expect(page.locator(".badge-status").first).to_contain_text("Em Processamento")

    # salva
    token = cliente_login(USUARIO3["email"], USUARIO3["senha"])
    compras = api_get(token, "/api/compras")
    compra_id = max(c["id"] for c in compras)
    salvar("compra_cupom_id", compra_id)
