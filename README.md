# Sistema de Atendimento com IA

![CI](https://github.com/lucasmarchesii/Sistema-Atendimento-Integrado-com-IA/actions/workflows/ci.yml/badge.svg)

Backend em Java e Spring Boot que recebe mensagens de clientes, guarda cada uma e as classifica automaticamente (categoria, prioridade, assunto e resposta sugerida para um atendente revisar).

O foco do projeto não é "chamar uma IA", e sim **construir um software que continua funcionando mesmo quando a IA falha ou responde algo inesperado**:

- a mensagem do cliente é guardada **antes** de qualquer processamento, então nunca se perde;
- a resposta da IA é tratada como entrada externa e **validada em código**;
- se a classificação falhar, o atendimento fica pendente e é **reprocessado depois**, sem intervenção;
- a qualidade da classificação é **medida com dados**, e não apenas percebida.

## Como funciona

```
Cliente ── POST /atendimentos ──► Controller ──► Service ──► Repository ──► PostgreSQL
                                     │              │
                          validação  │              └──► Classificador (interface)
                          de entrada                      ├─ ClassificadorSimulado   (palavras-chave)
                                                          └─ ClassificadorIA ──► ClaudeClient ──► API da Anthropic
```

Estados de um atendimento:

```
RECEBIDO ──► CLASSIFICADO
    │
    └──► PENDENTE_CLASSIFICACAO ──(reprocessamento manual ou automático)──► CLASSIFICADO
```

1. O `Service` grava a mensagem com status `RECEBIDO`.
2. Em seguida, pede ao classificador a categoria, a prioridade, o assunto e uma resposta sugerida.
3. Se der certo, o atendimento vira `CLASSIFICADO`.
4. Se a classificação falhar (API fora do ar, timeout, JSON inválido, categoria inventada), o atendimento vira `PENDENTE_CLASSIFICACAO`, e o cliente recebe uma resposta normal.
5. Um reprocessamento (manual, por endpoint, ou automático, a cada minuto) tenta classificar os pendentes de novo. Ele é **idempotente**: quem já foi classificado não é processado outra vez.

### Decisões de projeto

| Decisão | Motivo |
|---|---|
| Salvar a mensagem antes de chamar a IA | Nenhuma mensagem se perde se a IA falhar |
| Validar a resposta da IA em código (JSON, enums, tamanhos) | A saída de uma IA é uma entrada externa e não pode ser considerada confiável |
| Timeouts de 5 s para conectar e 30 s para ler | Uma API lenta não prende o sistema |
| Falha vira `PENDENTE_CLASSIFICACAO` e é reprocessada | O sistema se recupera sozinho quando a IA volta |
| `Classificador` como interface | Troca de provedor por configuração; testes sem rede e sem custo |
| Migrations com Flyway e `ddl-auto=validate` | Schema versionado e reproduzível; restrições `CHECK` e índice no banco |
| Configuração por variáveis de ambiente | Segredos fora do Git; o mesmo código roda em qualquer ambiente |
| Conjunto de teste cego separado | Mede se o classificador generaliza, e não se decorou os exemplos |

## Tecnologias

Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL 16, Flyway, springdoc-openapi (Swagger UI), Maven, Docker e Docker Compose, GitHub Actions, JUnit 5, Mockito e AssertJ.

## Como executar

### Com Docker (recomendado)

Requisitos: Docker com Docker Compose.

```bash
git clone https://github.com/lucasmarchesii/Sistema-Atendimento-Integrado-com-IA.git
cd Sistema-Atendimento-Integrado-com-IA

cp .env.example .env        # abra o .env e troque POSTGRES_PASSWORD
docker compose up -d --build
```

A primeira execução demora alguns minutos (baixa as imagens e as dependências). Depois:

- API: http://localhost:8080
- Documentação interativa: http://localhost:8080/swagger-ui/index.html
- Verificação rápida: `curl http://localhost:8080/health`

Para parar: `docker compose down` (mantém os dados) ou `docker compose down -v` (apaga os dados).

### Para desenvolver

Requisitos: Docker e JDK 21.

```bash
docker compose up -d db      # só o banco
./mvnw spring-boot:run       # a aplicação lê as variáveis do arquivo .env
```

Não rode a aplicação pelo Maven e pelo Docker ao mesmo tempo: as duas usam a porta 8080.

## API

![Swagger UI](docs/swagger.png)

A documentação completa e interativa fica no Swagger UI (`/swagger-ui/index.html`). A especificação OpenAPI em JSON está em `/v3/api-docs`.

| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/atendimentos` | Registra um atendimento e tenta classificá-lo. Responde `201`, ou `400` se os dados forem inválidos |
| `GET` | `/atendimentos` | Lista os atendimentos |
| `POST` | `/atendimentos/reprocessar` | Tenta classificar de novo até 20 pendentes. Devolve um resumo com total, classificados e falhas |
| `GET` | `/health` | Verifica que a aplicação está de pé |

Exemplo:

```bash
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"nomeCliente":"Maria","mensagem":"Fui cobrada duas vezes no cartão e preciso do estorno urgente"}'
```

```json
{
  "id": 1,
  "nomeCliente": "Maria",
  "mensagem": "Fui cobrada duas vezes no cartão e preciso do estorno urgente",
  "status": "CLASSIFICADO",
  "categoria": "FINANCEIRO",
  "prioridade": "ALTA",
  "assunto": "Questão de cobrança ou pagamento",
  "respostaSugerida": "Olá! Vamos verificar a sua cobrança e retornamos em breve com a solução.",
  "criadoEm": "2026-11-20T21:22:20.553841Z"
}
```

Categorias: `FINANCEIRO`, `ACESSO`, `CANCELAMENTO`, `TECNICO`, `OUTROS`. Prioridades: `BAIXA`, `MEDIA`, `ALTA`.

## Configuração

A configuração vem de variáveis de ambiente. O arquivo `.env` (ignorado pelo Git) é lido tanto pelo Docker Compose quanto pela aplicação. O `.env.example` mostra o modelo.

| Variável | Padrão | Descrição |
|---|---|---|
| `POSTGRES_DB` | obrigatória | Nome do banco |
| `POSTGRES_USER` | obrigatória | Usuário do banco |
| `POSTGRES_PASSWORD` | obrigatória | Senha do banco |
| `DB_HOST` | `localhost` | Endereço do banco (no Compose, vale `db`) |
| `DB_PORT` | `5432` | Porta do banco |
| `CLASSIFICADOR_PROVEDOR` | `simulado` | `simulado` ou `claude` |
| `ANTHROPIC_API_KEY` | vazio | Chave da API, usada só com o provedor `claude` |
| `REPROCESSAMENTO_AUTOMATICO` | `true` | Liga o reprocessamento periódico |
| `REPROCESSAMENTO_INTERVALO_MS` | `60000` | Intervalo do reprocessamento automático |
| `SWAGGER_UI_ENABLED` | `true` | Liga ou desliga a documentação interativa |

No Docker Compose, as variáveis repassadas ao contêiner da aplicação são `POSTGRES_*`, `DB_HOST`, `CLASSIFICADOR_PROVEDOR` e `ANTHROPIC_API_KEY`. As demais valem ao rodar pelo Maven ou se forem acrescentadas ao bloco `environment` do `docker-compose.yml`.

## Classificadores

**`simulado` (padrão).** Classifica por palavras-chave. **Não é IA.** Serve como linha de base (*baseline*) e permite desenvolver e testar sem custo e sem internet.

**`claude`.** Envia a mensagem à API de Mensagens da Anthropic, pedindo um JSON com categoria, prioridade, assunto e resposta sugerida, e valida a resposta antes de usá-la. O prompt trata o texto do cliente como dado, e não como instrução. Para ativar:

```
CLASSIFICADOR_PROVEDOR=claude
ANTHROPIC_API_KEY=sua-chave
```

> **Estado atual:** o provedor `claude` está implementado e o seu comportamento de falha foi testado (sem chave e com erro da API), mas **ainda não foi validado contra a API real**, pois isso exige chave e créditos. Quando for, o mesmo teste de avaliação abaixo permite compará-lo com o classificador de palavras-chave.
>
> **Privacidade:** com o provedor `claude`, o texto das mensagens dos clientes é enviado a um serviço externo. Em um uso real, isso exige avaliar a LGPD e a política de retenção de dados do provedor.

## Avaliação da qualidade

A pergunta "o classificador funciona?" é respondida com dados. O teste `AvaliacaoClassificadorTest` roda o classificador sobre mensagens com a classificação correta (o *gabarito*, definido manualmente) e relata a acurácia de categoria e de prioridade, além de uma matriz de confusão.

- **Conjunto de desenvolvimento** (`src/test/resources/avaliacao/mensagens.txt`, 62 mensagens): usado para encontrar erros e ajustar as regras.
- **Teste cego** (`src/test/resources/avaliacao/mensagens-teste.txt`, 20 mensagens, 5 categorias): usado só para conferir se a melhora se mantém. O relatório dele **não mostra os erros individuais**, de propósito.

Formato dos arquivos: `CATEGORIA|PRIORIDADE|mensagem`, uma por linha.

| Versão do classificador simulado | Desenvolvimento (categoria / prioridade) | Teste cego (categoria / prioridade) |
|---|---|---|
| Inicial | 74,2% / 62,9% | 85,0% / 55,0% |
| Vocabulário de problemas técnicos ampliado | 82,3% / 62,9% | 100% / 55,0% |
| Regras de prioridade | 82,3% / 82,3% | 100% / 65,0% |

O que a avaliação mostrou:

- O erro dominante não era confundir categorias, e sim **ficar em silêncio**: quando nenhuma palavra-chave aparece, a mensagem cai em `OUTROS`. A categoria técnica era a pior (recall de 3 em 9 no início).
- O classificador é **sensível ao vocabulário**: "vocês aceitam pagamento via Pix?" é classificada corretamente, e "vocês aceitam Pix?" não.
- Ele **não entende contexto**: "cobrado mesmo depois de cancelar o pedido" vai para cancelamento, e a categoria correta é financeira.
- Uma alteração nas regras chegou a derrubar o recall de cancelamentos de 7 para 4 acertos. Foi percebida na matriz de confusão e corrigida, e os pisos mínimos de qualidade do teste agora protegem contra essa regressão.
- O primeiro teste cego foi **descartado por vazamento de dados** (as mensagens eram versões reescritas das do conjunto de desenvolvimento e todas de uma única categoria). Ele foi incorporado ao desenvolvimento e substituído por um novo.

## Testes e CI

```bash
docker compose up -d db
./mvnw test
```

- **Unitários** (sem banco): `ClassificadorIATest` valida a leitura e a validação da resposta da IA (JSON válido, embrulhado em texto, categoria inventada, campos ausentes, tamanho excedido, falha de rede). `AtendimentoServiceTest` garante, por exemplo, que a mensagem é salva **antes** de chamar a IA e que o reprocessamento conta sucessos e falhas.
- **Integração:** `AtendimentoApiIntegrationTest` sobe a aplicação em uma porta aleatória, com o banco real, e testa o fluxo HTTP completo, a validação (`400`) e a publicação da documentação OpenAPI. Exige o banco ligado.
- **Avaliação:** `AvaliacaoClassificadorTest`, descrito acima. Para ver o relatório: `./mvnw test -Dtest=AvaliacaoClassificadorTest`.

Só os unitários: `./mvnw test -Dtest='ClassificadorIATest,AtendimentoServiceTest'`.

A cada `push`, o GitHub Actions compila o projeto e roda todos os testes, com um PostgreSQL como serviço, e uma segunda tarefa confere que a imagem Docker constrói.

## Estrutura do projeto

```
src/main/java/com/lucasmarchesi/atendimento/
├── config/        agendamento e documentação OpenAPI
├── controller/    endpoints HTTP
├── dto/           dados de entrada e resumo do reprocessamento
├── ia/            interface Classificador, cliente da API, validação da resposta
├── model/         entidade Atendimento e enums
├── repository/    acesso ao banco (Spring Data JPA)
└── service/       regras de negócio e reprocessamento agendado
src/main/resources/db/migration/   migrations do Flyway
src/test/                          testes e conjuntos de avaliação
```

## Limitações conhecidas

**Arquitetura e operação**

- A classificação acontece **dentro da requisição**, então o cliente espera por ela (até o timeout de 30 s). O ideal seria uma fila dedicada.
- O reprocessamento não tem **limite de tentativas** nem espera crescente entre elas, e processa lotes de 20.
- A trava que evita reprocessamentos simultâneos vale para **uma única instância** da aplicação. Com várias instâncias, seria preciso uma trava no banco.
- Um atendimento que ficasse em `RECEBIDO` (por exemplo, se a aplicação caísse entre os dois salvamentos) não é recuperado automaticamente.
- Não há **autenticação**, e a documentação interativa fica aberta por padrão. Nenhuma das duas deve ir assim para produção.
- A listagem não tem filtros nem paginação, não há painel, e a entidade é devolvida diretamente nas respostas, sem um DTO de resposta.

**Classificação e avaliação**

- O provedor `claude` **não foi validado com a API real**.
- Os conjuntos de avaliação são **pequenos e fictícios**, escritos por poucas pessoas: cada mensagem vale vários pontos percentuais e o resultado pode não refletir mensagens reais.
- O gabarito é subjetivo em casos ambíguos. Três mensagens com cancelamento e cobrança juntos têm rótulos inconsistentes.
- O teste cego foi consultado várias vezes e perde independência a cada consulta.
- O classificador simulado é limitado por natureza: não entende contexto, depende do vocabulário e cai em `OUTROS` por silêncio.
- Contra *prompt injection*, o provedor `claude` conta com a instrução do prompt e com a validação da saída, o que reduz o risco, mas não o elimina.

## Próximos passos

- Validar o provedor `claude` com a API real e comparar com o classificador de palavras-chave usando o mesmo teste de avaliação.
- Classificação assíncrona, com fila, limite de tentativas e espera crescente.
- Filtros, paginação e um painel simples para os atendentes.
- Autenticação, métricas e DTO de resposta.
- Ampliar o conjunto de avaliação com mensagens de outras pessoas.

## Autor

Lucas Marchesi Gomes — [github.com/lucasmarchesii](https://github.com/lucasmarchesii)
