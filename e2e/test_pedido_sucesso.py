# roda o caminho cadastro -> login -> produto -> carrinho -> checkout -> pedido

import re
import time
from playwright.sync_api import Page, expect


def gerar_cpf() -> str:
    """gera cpf fake mas que passa na validação (calcula os digitos verificadores certinho)."""
    import random
    n = [random.randint(0, 9) for _ in range(9)]
    # primeiro digito verificador
    d1 = sum(v * (10 - i) for i, v in enumerate(n)) % 11
    d1 = 0 if d1 < 2 else 11 - d1
    n.append(d1)
    # segundo digito verificador
    d2 = sum(v * (11 - i) for i, v in enumerate(n)) % 11
    d2 = 0 if d2 < 2 else 11 - d2
    n.append(d2)
    return "".join(str(d) for d in n)


def gerar_numero_cartao() -> str:
    """gera numero de cartao que passa no algoritmo de luhn (validação usada por visa, master, etc)."""
    import random
    digitos = [random.randint(0, 9) for _ in range(15)]
    # calcula o digito de checagem pra fechar o luhn
    total = 0
    for i, d in enumerate(reversed(digitos)):
        if i % 2 == 0:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    digitos.append((10 - (total % 10)) % 10)
    n = "".join(str(d) for d in digitos)
    # formata em blocos de 4 tipo cartão real
    return f"{n[:4]} {n[4:8]} {n[8:12]} {n[12:16]}"


# timestamp garante que cada execução cria um usuário novo (sem conflito de email)
TIMESTAMP = int(time.time() * 1000)
USUARIO = {
    "nome": f"Teste E2E {TIMESTAMP}",
    "email": f"teste.e2e.{TIMESTAMP}@test.com",
    "senha": "Teste@123",
    "genero": "MASCULINO",
    "data_nascimento": "1990-01-15",
}


def test_registro_pedido_com_sucesso(page: Page):
    """testa o fluxo completo de compra, do cadastro até a verificação do pedido."""

    # cadastro de usuário novo 
    page.goto("/#register")
    page.wait_for_selector("#register-form")

    page.fill('input[name="nome"]', USUARIO["nome"])
    page.fill('input[name="email"]', USUARIO["email"])
    page.fill('input[name="senha"]', USUARIO["senha"])
    page.fill('input[name="confirmacaoSenha"]', USUARIO["senha"])
    page.fill('input[name="cpf"]', gerar_cpf())
    page.select_option('select[name="genero"]', USUARIO["genero"])
    page.fill('input[name="dataNascimento"]', USUARIO["data_nascimento"])

    page.click('#register-form button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Conta criada com sucesso", timeout=10000)

    # login com o usuário que acabou de criar
    page.wait_for_selector("#login-form")
    page.fill('#login-form input[name="email"]', USUARIO["email"])
    page.fill('#login-form input[name="senha"]', USUARIO["senha"])
    page.click('#login-form button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Login realizado com sucesso", timeout=10000)

    # pega o primeiro produto disponível e joga no carrinho
    page.goto("/#products")
    page.wait_for_selector(".product-card", timeout=10000)

    page.locator(".product-card .btn").first.click()
    expect(page.locator(".toast").last).to_contain_text("adicionado ao carrinho", timeout=10000)

    #abre o carrinho e vai pro checkout
    page.goto("/#cart")
    page.wait_for_selector(".cart-item", timeout=10000)

    page.click('button:has-text("Finalizar Compra")')
    page.wait_for_selector(".checkout-grid", timeout=15000)

    # preenche endereço de entrega (dados fixos, não precisa ser único)
    page.click('button:has-text("Adicionar Endereco")')
    page.wait_for_selector("#address-form-container form")

    page.select_option('#address-form-container select[name="tipoEndereco"]', "ENTREGA")
    page.fill('#address-form-container input[name="nomeEndereco"]', "Casa Teste")
    page.select_option('#address-form-container select[name="tipoResidencia"]', "Casa")
    page.select_option('#address-form-container select[name="tipoLogradouro"]', "Rua")
    page.fill('#address-form-container input[name="logradouro"]', "Rua das Flores")
    page.fill('#address-form-container input[name="numero"]', "123")
    page.fill('#address-form-container input[name="bairro"]', "Centro")
    page.fill('#address-form-container input[name="cep"]', "01001000")
    page.fill('#address-form-container input[name="cidade"]', "Sao Paulo")
    page.select_option('#address-form-container select[name="estado"]', "SP")
    page.select_option('#address-form-container select[name="pais"]', "Brasil")

    page.click('#address-form-container button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Endereco salvo", timeout=10000)

    # espera o frontend recalcular o frete depois de salvar o endereço
    page.wait_for_timeout(2000)

    #cria um cartão de crédito pro pagamento
    page.click('button:has-text("Adicionar Cartao")')
    page.wait_for_selector("#card-form-container form")

    page.fill('#card-form-container input[name="numero"]', gerar_numero_cartao())
    page.fill('#card-form-container input[name="nomeImpresso"]', "TESTE E2E")
    page.select_option('#card-form-container select[name="bandeira"]', "VISA")
    page.fill('#card-form-container input[name="codigoSeguranca"]', "123")

    page.click('#card-form-container button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Cartao salvo", timeout=10000)

    # espera o re-render da lista de cartões
    page.wait_for_timeout(2000)

    # usa o cartão pra pagar o valor total 
    total_text = page.locator(".summary-total span").last.text_content()
    valor_total = total_text.strip().replace("R$ ", "")

    page.fill("#checkout-card-valor", valor_total)
    page.locator('button.btn-primary.btn-sm:has-text("Adicionar")').first.click()

    page.wait_for_timeout(1000)

    # confirma a compra e espera o popup de sucesso 
    page.click('button:has-text("Confirmar Compra")')
    expect(page.locator(".toast").last).to_contain_text(
        "Compra realizada com sucesso", timeout=15000
    )

    # -- verifica se o pedido aparece na lista com os dados certos --
    page.wait_for_selector(".order-card", timeout=15000)

    # status inicial de toda compra nova
    expect(page.locator(".badge-status").first).to_contain_text("Em Processamento")

    # checa se a data de entrega prevista tá visível e no formato dd/mm/aaaa
    delivery = page.locator(".order-delivery-date").first
    expect(delivery).to_be_visible()
    delivery_text = delivery.text_content()
    assert "Entrega prevista:" in delivery_text
    assert re.search(r"\d{2}/\d{2}/\d{4}", delivery_text), (
        f"Data de entrega nao encontrada no texto: {delivery_text}"
    )