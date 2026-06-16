import { state } from '../state.js';
import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { login } from '../auth.js';
import { loadCartFromBackend } from '../cart.js';
import { showToast } from '../utils.js';

// pagina de login com formulario de email e senha
export function LoginPage() {
  return `
    ${Header()}
    <main>
      <section class="auth-section">
        <div class="auth-container">
          <div class="auth-card">
            <h1>Entrar</h1>
            <p class="text-muted">Acesse sua conta Technomade</p>

            <form id="login-form" onsubmit="handleLogin(event)">
              <div class="form-group">
                <label>Email</label>
                <input type="email" name="email" required placeholder="seu@email.com" />
              </div>

              <div class="form-group">
                <label>Senha</label>
                <input type="password" name="senha" required placeholder="••••••••" />
              </div>

              <button type="submit" class="btn btn-primary btn-block">
                Entrar
              </button>
            </form>

            <div class="auth-footer">
              <p>Não tem uma conta? <a href="#" onclick="navigate('register'); return false;">Cadastre-se</a></p>
            </div>
          </div>
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// handler do formulario de login - envia credenciais e redireciona
export async function handleLogin(e, navigateFn) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData);

  try {
    await login(data.email, data.senha);
    showToast("Login realizado com sucesso!");
    if (state.user?.role !== 'ADMIN') {
      await loadCartFromBackend();
    }
    navigateFn("home");
  } catch (err) {
    showToast("Erro ao fazer login: " + err.message, "error");
  }
}
