# 05 - administrador define que o produto esta EM TRANSPORTE
# admin loga no painel, vai na aba pedidos e clica em "Despachar"

from playwright.sync_api import Page, expect
from helpers import *
from estado import salvar, carregar


def test_admin_despacha_em_transito(page: Page):
    """admin loga, abre painel de pedidos e despacha o pedido."""
    compra_id = carregar("compra_id")

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

    # clica na aba "Pedidos"
    page.click('button:has-text("Pedidos")')
    page.wait_for_timeout(3000)

    # aceita o confirm() automaticamente
    page.on("dialog", lambda dialog: dialog.accept())

    # clica no botao "Despachar" do pedido especifico
    page.locator(f'button[onclick*="updateOrderStatus({compra_id}"]:has-text("Despachar")').click()
    expect(page.locator(".toast").last).to_contain_text("despachar", timeout=10000)

    # verifica que o status mudou
    page.wait_for_timeout(2000)
    row = page.locator(f"tr.admin-order-row", has=page.locator(f"td:text-is('#{compra_id}')"))
    expect(row.locator(".badge-status.status-transit")).to_be_visible()

    salvar("compra_status", "EM_TRANSITO")
