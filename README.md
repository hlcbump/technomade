# Technomade - E-commerce

Trabalho de LES (Laboratório de Engenharia de Software) — FATEC Mogi das Cruzes.

E-commerce de gadgets e acessórios para nômades digitais.

## Pré-requisitos

- Java 21
- PostgreSQL 16
- Node.js (para servir o frontend)
- Python 3 + Playwright (apenas para testes e2e)

## Configuração

1. Copie o arquivo de variáveis de ambiente e preencha:

```bash
cp .env.example .env
```

2. Edite o `.env` com seus dados:

```env
DB_USERNAME=technomade
DB_PASSWORD=sua_senha_do_postgres
JWT_SECRET=cole_uma_chave_aqui
```

Para gerar o JWT_SECRET, rode:

```bash
openssl rand -hex 32
```

3. Crie o banco de dados no PostgreSQL:

```sql
CREATE DATABASE technomade;
```

## Como rodar

### Backend

```bash
./mvnw spring-boot:run
```

O backend sobe em `http://localhost:8080`.

Na primeira execução, o Hibernate cria as tabelas automaticamente e o `data.sql` popula o banco com dados de demonstração (produtos, usuários, compras).

### Frontend

O frontend é HTML/CSS/JS puro (sem framework). Para servir os arquivos:

```bash
npx serve frontend/ -l 8000
```

Acesse em `http://localhost:8000`.

## Usuários de teste

O `data.sql` já cria esses usuários automaticamente:

| Tipo | Email | Senha |
|------|-------|-------|
| Admin | `admin@technomade.com` | `Admin@123` |
| Cliente | `carlos@teste.com` | `Admin@123` |

## Testes

### Testes unitários (Java)

```bash
./mvnw test
```

### Testes e2e (Playwright + Python)

Os testes e2e simulam o fluxo completo no navegador. Precisam do backend e frontend rodando.

```bash
# 1. Instale as dependências (primeira vez)
pip install pytest playwright
playwright install

# 2. Suba o backend e o frontend (em terminais separados)
./mvnw spring-boot:run
npx serve frontend/ -l 8000

# 3. Rode os testes
cd e2e
pytest
```

Os arquivos de teste seguem a ordem do fluxo de compra:

```
01_cliente_realiza_compra.py
02_pagamento_multiplos_cartoes.py
03_pagamento_cupom_e_cartao.py
04_admin_confirma_pagamento.py
05_admin_despacha_em_transito.py
06_admin_confirma_entrega.py
07_cliente_solicita_troca.py
08a_admin_autoriza_troca.py
09_admin_recebe_devolucao.py
10_sistema_gera_cupom_troca.py
11_compra_com_cupom_troca.py
```

## Estrutura do projeto

```
src/main/java/         → Código do backend (Spring Boot)
  controller/          → Endpoints da API REST
  service/             → Regras de negócio
  repository/          → Acesso ao banco de dados
  model/               → Entidades JPA
  dto/                 → Objetos de transferência (request/response)
  config/              → Segurança e JWT

frontend/              → Interface web (HTML/CSS/JS)
e2e/                   → Testes end-to-end com Playwright
postman/               → Collection para testar a API
```

## API

A documentação dos endpoints pode ser testada via Postman (pasta `postman/`).

Principais rotas:

| Rota | Descrição |
|------|-----------|
| `POST /api/auth/login` | Login (retorna token JWT) |
| `POST /api/usuarios` | Cadastro de usuário |
| `GET /api/produtos` | Listar produtos |
| `POST /api/compras` | Finalizar compra |
| `POST /api/carrinho` | Adicionar item ao carrinho |
| `POST /api/trocas` | Solicitar troca |
