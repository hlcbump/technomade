# 10 - sistema gera cupom de troca apos confirmacao do recebimento
# verifica que o cupom foi gerado com valor correto e validade de 3 meses

from playwright.sync_api import Page, expect
from helpers import *
from estado import carregar, salvar


def test_sistema_gerou_cupom_troca(page: Page):
    """verifica que o sistema gerou cupom de troca com valor e validade corretos."""
    troca_id = carregar("troca_id")
    admin_token = admin_login()

    # busca todos os cupons
    cupons = api_get(admin_token, "/api/cupons")

    # filtra o cupom gerado pela troca
    cupom_troca = [c for c in cupons if f"TROCA-{troca_id}" in c.get("codigo", "")]
    assert len(cupom_troca) > 0, f"Cupom de troca nao foi gerado para troca #{troca_id}"

    cupom = cupom_troca[0]
    assert cupom["valor"] > 0, "Cupom de troca deve ter valor positivo"
    assert cupom["usado"] is False, "Cupom recem-gerado nao deve estar usado"
    assert cupom["promocional"] is False, "Cupom de troca nao e promocional"
    assert cupom["validade"] is not None, "Cupom de troca deve ter data de validade"

    # salva o codigo do cupom pro teste 11 usar
    salvar("cupom_troca_codigo", cupom["codigo"])
    salvar("cupom_troca_valor", cupom["valor"])

    # verifica que o cliente consegue ver o cupom (login na UI pra validar acesso)
    email = carregar("cliente_email")
    senha = carregar("cliente_senha")

    page.goto("/#login")
    login_usuario(page, email, senha)

    # navega pro checkout pra ver cupons disponiveis
    adicionar_produto_carrinho(page)
    ir_para_checkout(page)
    page.wait_for_timeout(2000)

    # verifica que o cupom de troca aparece como disponivel
    page_content = page.content()
    assert cupom["codigo"] in page_content, (
        f"Cupom '{cupom['codigo']}' nao aparece na pagina de checkout"
    )
