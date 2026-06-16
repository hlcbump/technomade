import { Header } from '../components/header.js';
import { Footer } from '../components/footer.js';
import { register } from '../auth.js';
import { validarSenhaForte, validarConfirmacaoSenha, validarCpf, mascaraCpf, showFieldError } from '../validators.js';
import { showToast } from '../utils.js';

// pagina de cadastro com validacao de senha em tempo real
export function RegisterPage() {
  return `
    ${Header()}
    <main>
      <section class="auth-section">
        <div class="auth-container">
          <div class="auth-card">
            <h1>Criar Conta</h1>
            <p class="text-muted">Junte-se à comunidade Technomade</p>

            <form id="register-form" onsubmit="handleRegister(event)">
              <div class="form-group">
                <label>Nome Completo</label>
                <input type="text" name="nome" required placeholder="João Silva" />
              </div>

              <div class="form-group">
                <label>Email</label>
                <input type="email" name="email" required placeholder="seu@email.com" />
              </div>

              <div class="form-group">
                <label>Senha</label>
                <input type="password" name="senha" required placeholder="••••••••"
                       oninput="validarSenhaTempoReal(this.value)" />
                <!-- indicadores visuais dos requisitos de senha -->
                <div class="password-requirements">
                  <div class="pwd-req invalid" data-req="length">
                    <i class="fas fa-check-circle"></i> Mínimo 8 caracteres
                  </div>
                  <div class="pwd-req invalid" data-req="uppercase">
                    <i class="fas fa-check-circle"></i> Uma letra maiúscula
                  </div>
                  <div class="pwd-req invalid" data-req="lowercase">
                    <i class="fas fa-check-circle"></i> Uma letra minúscula
                  </div>
                  <div class="pwd-req invalid" data-req="special">
                    <i class="fas fa-check-circle"></i> Um caractere especial
                  </div>
                </div>
              </div>

              <div class="form-group">
                <label>Confirmar Senha</label>
                <input type="password" name="confirmacaoSenha" required placeholder="••••••••" />
              </div>

              <div class="form-group">
                <label>CPF</label>
                <input type="text" name="cpf" placeholder="000.000.000-00" maxlength="14"
                       oninput="mascaraCpf(this)" />
              </div>

              <div class="form-group">
                <label>Telefone</label>
                <input type="tel" name="telefone" placeholder="(11) 99999-9999" />
              </div>

              <div class="form-group">
                <label>Gênero <span class="required">*</span></label>
                <select name="genero" required>
                  <option value="">Selecione...</option>
                  <option value="MASCULINO">Masculino</option>
                  <option value="FEMININO">Feminino</option>
                  <option value="NAO_INFORMAR">Não informar</option>
                </select>
              </div>

              <div class="form-group">
                <label>Data de Nascimento <span class="required">*</span></label>
                <input type="date" name="dataNascimento" required />
              </div>

              <input type="hidden" name="role" value="CLIENTE" />

              <button type="submit" class="btn btn-primary btn-block">
                Criar Conta
              </button>
            </form>

            <div class="auth-footer">
              <p>Já tem uma conta? <a href="#" onclick="navigate('login'); return false;">Entrar</a></p>
            </div>
          </div>
        </div>
      </section>
    </main>
    ${Footer()}
  `;
}

// handler do formulario de cadastro - valida senha e cria conta
export async function handleRegister(e, navigateFn) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
  const data = Object.fromEntries(formData);

  // RNF0031 - valida se a senha e forte
  const senhaError = validarSenhaForte(data.senha);
  if (senhaError) {
    showFieldError('senha', senhaError);
    showToast(senhaError, "error");
    return;
  }

  // RNF0032 - valida se as senhas coincidem
  const confirmError = validarConfirmacaoSenha(data.senha, data.confirmacaoSenha);
  if (confirmError) {
    showFieldError('confirmacaoSenha', confirmError);
    showToast(confirmError, "error");
    return;
  }

  // valida CPF se preenchido
  const cpfError = validarCpf(data.cpf);
  if (cpfError) {
    showFieldError('cpf', cpfError);
    showToast(cpfError, "error");
    return;
  }

  // remove campo de confirmacao que nao vai para o backend
  delete data.confirmacaoSenha;

  // remove campos vazios antes de enviar
  Object.keys(data).forEach((key) => {
    if (data[key] === "") delete data[key];
  });

  try {
    await register(data);
    showToast("Conta criada com sucesso! Faça login para continuar.");
    navigateFn("login");
  } catch (err) {
    showToast("Erro ao criar conta: " + err.message, "error");
  }
}
