# 11 - cliente usa o cupom de troca gerado para fazer uma nova compra
# pega cupom_troca_codigo do teste 10 e usa como pagamento

from playwright.sync_api import Page, expect
from helpers import *
from estado import carregar


def test_compra_com_cupom_troca(page: Page):
    """cliente usa cupom de troca (gerado no fluxo anterior) + cartao para nova compra."""
    email = carregar("cliente_email")
    senha = carregar("cliente_senha")
    cupom_codigo = carregar("cupom_troca_codigo")

    # login do cliente
    page.goto("/#login")
    login_usuario(page, email, senha)

    # adiciona produto ao carrinho (o teste 10 ja adicionou um, mas o carrinho pode ter sido limpo)
    adicionar_produto_carrinho(page)
    ir_para_checkout(page)

    # o endereco e cartao ja existem dos testes anteriores, espera carregar
    page.wait_for_timeout(2000)

    # aplica o cupom de troca
    page.fill("#coupon-input", cupom_codigo)
    page.click('button:has-text("Aplicar")')
    expect(page.locator(".toast").last).to_contain_text("Cupom aplicado com sucesso", timeout=10000)
    page.wait_for_timeout(1000)

    # pega o valor restante apos desconto do cupom
    total_text = page.locator(".summary-total span").last.text_content()
    valor_total = float(total_text.strip().replace("R$ ", "").replace(",", "."))

    # se ainda restar valor, paga com cartao
    if valor_total > 0:
        page.fill("#checkout-card-valor", str(round(valor_total, 2)))
        page.locator('button.btn-primary.btn-sm:has-text("Adicionar")').first.click()
        page.wait_for_timeout(1000)

    # confirma compra
    page.click('button:has-text("Confirmar Compra")')
    expect(page.locator(".toast").last).to_contain_text("Compra realizada com sucesso", timeout=15000)

    # verifica pedido criado
    page.wait_for_selector(".order-card", timeout=15000)
    expect(page.locator(".badge-status").first).to_contain_text("Em Processamento")
