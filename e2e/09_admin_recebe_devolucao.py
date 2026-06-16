# 09 - administrador confirma recebimento do produto devolvido
# admin loga no painel, vai na aba trocas e clica em "Recebido"

from playwright.sync_api import Page, expect
from helpers import *
from estado import salvar, carregar


def test_admin_confirma_recebimento_devolucao(page: Page):
    """admin loga, abre painel de trocas e confirma recebimento."""
    troca_id = carregar("troca_id")

    # login como admin na UI
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

    # aceita os confirm() automaticamente (tem 2: "Confirmar recebimento?" e "Retornar ao estoque?")
    page.on("dialog", lambda dialog: dialog.accept())

    # clica no botao "Recebido" da troca especifica
    page.locator(f'button[onclick="confirmTrocaReceived({troca_id})"]').click()
    expect(page.locator(".toast").last).to_contain_text("Troca confirmada", timeout=10000)

    # verifica que o status mudou para Trocada
    page.wait_for_timeout(2000)
    expect(page.locator(".badge-status.status-exchanged").first).to_be_visible()

    salvar("troca_status", "TROCADA")
