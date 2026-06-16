// RNF0031 - valida se a senha atende os requisitos de seguranca
export function validarSenhaForte(senha) {
  if (!senha || senha.length < 8) {
    return "A senha deve ter no mínimo 8 caracteres";
  }
  if (!/[A-Z]/.test(senha)) {
    return "A senha deve conter pelo menos uma letra maiúscula";
  }
  if (!/[a-z]/.test(senha)) {
    return "A senha deve conter pelo menos uma letra minúscula";
  }
  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(senha)) {
    return "A senha deve conter pelo menos um caractere especial (!@#$%^&*...)";
  }
  return null; // senha valida
}

// RNF0032 - valida se a confirmacao de senha e igual a senha
export function validarConfirmacaoSenha(senha, confirmacao) {
  if (senha !== confirmacao) {
    return "As senhas não coincidem";
  }
  return null;
}

// exibe ou remove mensagem de erro em um campo de formulario
export function showFieldError(inputName, message) {
  const input = document.querySelector(`[name="${inputName}"]`);
  if (!input) return;

  // remove erro anterior se existir
  const existingError = input.parentElement.querySelector('.field-error');
  if (existingError) existingError.remove();
  input.classList.remove('input-error');

  // adiciona novo erro se houver mensagem
  if (message) {
    input.classList.add('input-error');
    const errorDiv = document.createElement('div');
    errorDiv.className = 'field-error';
    errorDiv.textContent = message;
    input.parentElement.appendChild(errorDiv);
  }
}

// aplica mascara de CPF (000.000.000-00) no input
export function mascaraCpf(input) {
  let v = input.value.replace(/\D/g, '').slice(0, 11);
  v = v.replace(/(\d{3})(\d)/, '$1.$2');
  v = v.replace(/(\d{3})(\d)/, '$1.$2');
  v = v.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  input.value = v;
}

// valida CPF - apenas formato (11 digitos)
export function validarCpf(cpf) {
  if (!cpf) return null; // campo opcional

  const nums = cpf.replace(/\D/g, '');
  if (nums.length !== 11) return "CPF deve ter 11 dígitos";

  return null;
}

// valida senha em tempo real e atualiza os indicadores visuais
export function validarSenhaTempoReal(senha) {
  const requirements = {
    length: senha.length >= 8,
    uppercase: /[A-Z]/.test(senha),
    lowercase: /[a-z]/.test(senha),
    special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(senha),
  };

  // atualiza as classes CSS dos indicadores (verde = valido, vermelho = invalido)
  document.querySelectorAll('.pwd-req').forEach(el => {
    const req = el.dataset.req;
    if (requirements[req]) {
      el.classList.add('valid');
      el.classList.remove('invalid');
    } else {
      el.classList.remove('valid');
      el.classList.add('invalid');
    }
  });

  return Object.values(requirements).every(v => v);
}
