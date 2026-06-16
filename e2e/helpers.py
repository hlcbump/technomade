# helpers compartilhados entre os testes e2e

import time
import requests
from playwright.sync_api import Page, expect

BASE_API = "http://localhost:8080"
TIMESTAMP = int(time.time() * 1000)

ADMIN_EMAIL = "admin@admin.com"
ADMIN_SENHA = "123456"


def gerar_cpf() -> str:
    import random
    n = [random.randint(0, 9) for _ in range(9)]
    d1 = sum(v * (10 - i) for i, v in enumerate(n)) % 11
    d1 = 0 if d1 < 2 else 11 - d1
    n.append(d1)
    d2 = sum(v * (11 - i) for i, v in enumerate(n)) % 11
    d2 = 0 if d2 < 2 else 11 - d2
    n.append(d2)
    return "".join(str(d) for d in n)


def gerar_numero_cartao() -> str:
    import random
    digitos = [random.randint(0, 9) for _ in range(15)]
    total = 0
    for i, d in enumerate(reversed(digitos)):
        if i % 2 == 0:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    digitos.append((10 - (total % 10)) % 10)
    n = "".join(str(d) for d in digitos)
    return f"{n[:4]} {n[4:8]} {n[8:12]} {n[12:16]}"


# --- API helpers ---

def admin_login() -> str:
    resp = requests.post(f"{BASE_API}/api/auth/login", json={
        "email": ADMIN_EMAIL, "senha": ADMIN_SENHA,
    })
    assert resp.status_code == 200, f"Falha no login admin: {resp.text}"
    return resp.json()["token"]


def cliente_login(email: str, senha: str) -> str:
    resp = requests.post(f"{BASE_API}/api/auth/login", json={
        "email": email, "senha": senha,
    })
    assert resp.status_code == 200, f"Falha no login cliente: {resp.text}"
    return resp.json()["token"]


def api_get(token: str, path: str):
    resp = requests.get(f"{BASE_API}{path}", headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200, f"GET {path} falhou: {resp.text}"
    return resp.json()


def api_put(token: str, path: str, body: dict = None):
    resp = requests.put(
        f"{BASE_API}{path}",
        json=body or {},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200, f"PUT {path} falhou: {resp.text}"
    return resp.json()


def api_post(token: str, path: str, body: dict):
    resp = requests.post(
        f"{BASE_API}{path}",
        json=body,
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200, f"POST {path} falhou: {resp.text}"
    return resp.json()


def criar_cupom_via_api(admin_token: str, codigo: str, valor: float, promocional: bool = True):
    """cria cupom via API do admin. promocional=False cria cupom de troca."""
    return api_post(admin_token, "/api/cupons", {
        "codigo": codigo, "valor": valor, "validadeDias": 30, "promocional": promocional,
    })


# --- UI helpers ---

def registrar_usuario(page: Page, usuario: dict):
    page.goto("/#register")
    page.wait_for_selector("#register-form")
    page.fill('input[name="nome"]', usuario["nome"])
    page.fill('input[name="email"]', usuario["email"])
    page.fill('input[name="senha"]', usuario["senha"])
    page.fill('input[name="confirmacaoSenha"]', usuario["senha"])
    page.fill('input[name="cpf"]', gerar_cpf())
    page.select_option('select[name="genero"]', usuario["genero"])
    page.fill('input[name="dataNascimento"]', usuario["data_nascimento"])
    page.click('#register-form button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Conta criada com sucesso", timeout=10000)


def login_usuario(page: Page, email: str, senha: str):
    page.wait_for_selector("#login-form")
    page.fill('#login-form input[name="email"]', email)
    page.fill('#login-form input[name="senha"]', senha)
    page.click('#login-form button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Login realizado com sucesso", timeout=10000)


def adicionar_produto_carrinho(page: Page, indice: int = 0):
    page.goto("/#products")
    page.wait_for_selector(".product-card", timeout=10000)
    page.locator(".product-card .btn").nth(indice).click()
    expect(page.locator(".toast").last).to_contain_text("adicionado ao carrinho", timeout=10000)


def adicionar_n_produtos_carrinho(page: Page, quantidade: int = 2):
    """adiciona N produtos diferentes ao carrinho (um de cada)."""
    page.goto("/#products")
    page.wait_for_selector(".product-card", timeout=10000)
    total_disponiveis = page.locator(".product-card .btn").count()
    n = min(quantidade, total_disponiveis)
    for i in range(n):
        page.locator(".product-card .btn").nth(i).click()
        expect(page.locator(".toast").last).to_contain_text("adicionado ao carrinho", timeout=10000)
        page.wait_for_timeout(500)


def ir_para_checkout(page: Page):
    page.goto("/#cart")
    page.wait_for_selector(".cart-item", timeout=10000)
    page.click('button:has-text("Finalizar Compra")')
    page.wait_for_selector(".checkout-grid", timeout=15000)


def preencher_endereco(page: Page, estado: str = "SP"):
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
    page.select_option('#address-form-container select[name="estado"]', estado)
    page.select_option('#address-form-container select[name="pais"]', "Brasil")
    page.click('#address-form-container button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Endereco salvo", timeout=10000)
    page.wait_for_timeout(2000)


def preencher_cartao(page: Page):
    page.click('button:has-text("Adicionar Cartao")')
    page.wait_for_selector("#card-form-container form")
    page.fill('#card-form-container input[name="numero"]', gerar_numero_cartao())
    page.fill('#card-form-container input[name="nomeImpresso"]', "TESTE E2E")
    page.select_option('#card-form-container select[name="bandeira"]', "VISA")
    page.fill('#card-form-container input[name="codigoSeguranca"]', "123")
    page.click('#card-form-container button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Cartao salvo", timeout=10000)
    page.wait_for_timeout(2000)


def preencher_cartao_extra(page: Page, nome: str = "CARTAO EXTRA", bandeira: str = "MASTERCARD"):
    """adiciona mais um cartao no checkout (alem do primeiro)."""
    page.click('button:has-text("Adicionar Cartao")')
    page.wait_for_selector("#card-form-container form")
    page.fill('#card-form-container input[name="numero"]', gerar_numero_cartao())
    page.fill('#card-form-container input[name="nomeImpresso"]', nome)
    page.select_option('#card-form-container select[name="bandeira"]', bandeira)
    page.fill('#card-form-container input[name="codigoSeguranca"]', "456")
    page.click('#card-form-container button[type="submit"]')
    expect(page.locator(".toast").last).to_contain_text("Cartao salvo", timeout=10000)
    page.wait_for_timeout(2000)


def aplicar_cupom(page: Page, codigo: str):
    """aplica um cupom no checkout digitando o codigo."""
    page.fill("#coupon-input", codigo)
    page.click('button:has-text("Aplicar")')
    expect(page.locator(".toast").last).to_contain_text("Cupom aplicado com sucesso", timeout=10000)
    page.wait_for_timeout(1000)


def obter_valor_total_checkout(page: Page) -> float:
    """le o valor total exibido no resumo do checkout."""
    total_text = page.locator(".summary-total span").last.text_content()
    return float(total_text.strip().replace("R$ ", "").replace(",", "."))


def adicionar_cartao_checkout(page: Page, valor: float):
    """adiciona o cartao selecionado no select ao pagamento com o valor dado."""
    page.fill("#checkout-card-valor", str(round(valor, 2)))
    page.locator('button.btn-primary.btn-sm:has-text("Adicionar")').first.click()
    page.wait_for_timeout(500)


def finalizar_compra_com_cartao(page: Page):
    total_text = page.locator(".summary-total span").last.text_content()
    valor_total = total_text.strip().replace("R$ ", "")
    page.fill("#checkout-card-valor", valor_total)
    page.locator('button.btn-primary.btn-sm:has-text("Adicionar")').first.click()
    page.wait_for_timeout(1000)
    page.click('button:has-text("Confirmar Compra")')
    expect(page.locator(".toast").last).to_contain_text("Compra realizada com sucesso", timeout=15000)
