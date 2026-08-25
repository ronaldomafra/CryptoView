# CryptoView — plano original de desenvolvimento

> Documento histórico criado em 25/08/2026 a partir do `README.md`, do
> `PROMPT_DESENVOLVIMENTO.md`, dos mockups em `docs/mockups/` e do projeto de
> referência `C:\Projetos\Android\tbsales-multi`.
>
> Este arquivo registra o plano-base anterior à implementação. Os itens permanecem
> desmarcados propositalmente; o andamento real deve ser acompanhado em `TODO.md`.

## 1. Objetivo

Desenvolver o CryptoView como aplicativo Kotlin Multiplatform, com Android como
plataforma principal e interface Compose Multiplatform compartilhada com iOS.
O aplicativo deverá validar uma API key da CoinMarketCap, armazená-la com
segurança, sincronizar moedas e corretoras, persistir os dados localmente e
oferecer navegação local-first mesmo quando a rede estiver indisponível.

O escopo deve permanecer nos módulos existentes:

- `shared`: UI compartilhada, domínio, dados, rede, banco, segurança e contratos
  de plataforma.
- `androidApp`: host Android.
- `iosApp`: host iOS mínimo para iniciar a interface Compose.

Não criar módulos Desktop, Web ou backend. Não introduzir CocoaPods ou um módulo
iOS separado apenas para criptografia.

## 2. Decisões arquiteturais originais

- Kotlin Multiplatform e Compose Multiplatform para Android e iOS.
- MVVM com estado imutável, fluxo unidirecional e ViewModels compartilhados.
- Navigation 3 com dois destinos principais: `Mercado` e `Ajustes`.
- Koin com injeção por construtor e módulos separados por responsabilidade.
- Ktor/Ktorfit, Kotlin Serialization e engines específicas por plataforma.
- SQLDelight como fonte de verdade, com repositórios local-first.
- Coroutines e Flow para sincronização limitada, backpressure e polling.
- DataStore somente para preferências, metadados e envelopes não sensíveis.
- Android Keystore com AES-GCM para proteger a API key no Android.
- Keychain acessado diretamente por Kotlin/Native para proteger a API key no iOS.
- Sem WebSocket: atualização por REST e polling de 60 segundos apenas para a
  moeda expandida.
- Material 3, tema claro/escuro, baixa elevação, bordas discretas e layouts
  adaptáveis.

### Versões selecionadas no bootstrap

As versões abaixo representam a linha de base escolhida no projeto e devem ser
revalidadas em conjunto antes de mudanças de dependências:

| Tecnologia | Versão-base |
|---|---:|
| Kotlin | 2.4.10 |
| Android Gradle Plugin | 9.0.1 |
| Compose Multiplatform | 1.11.1 |
| Material 3 | 1.11.0-alpha07 |
| Navigation 3 | 1.1.1 |
| Koin | 4.2.2 |
| Coroutines | 1.11.0 |
| Ktor | 3.5.0 |
| Ktorfit | 2.7.5 |
| Kotlin Serialization JSON | 1.11.0 |
| Android SDK | compile/target 36; mínimo 24 |

## 3. Tasks — ordem de execução

### TASK-01 — Descoberta e documentação

- [ ] Revisar o desafio, a especificação e os seis mockups aprovados.
- [ ] Confirmar endpoints, campos, permissões e limitações do plano CoinMarketCap.
- [ ] Validar a compatibilidade das dependências escolhidas.
- [ ] Registrar riscos técnicos, decisões e critérios de aceite.
- [ ] Manter matriz requisito–endpoint–campo.

### TASK-02 — Bootstrap KMP

- [ ] Organizar source sets `commonMain`, `androidMain`, `iosMain`, `commonTest`,
  testes Android e testes iOS.
- [ ] Configurar Compose, Navigation 3, Serialization, Coroutines e Koin.
- [ ] Configurar hosts Android e iOS sem duplicar telas ou regras de negócio.
- [ ] Centralizar versões no version catalog.

### TASK-03 — UI navegável com dados mockados

- [ ] Implementar tema, componentes e telas conforme os mockups.
- [ ] Conectar toda a navegação antes da integração com rede e banco.
- [ ] Usar fixtures determinísticas somente na fase visual, previews e testes.
- [ ] Validar densidade visual, acessibilidade e diferentes dimensões.

### TASK-04 — Segurança da API key

- [ ] Criar o contrato compartilhado `SecureApiKeyStorage`.
- [ ] Implementar Android Keystore + AES-GCM dentro de `shared/androidMain`.
- [ ] Implementar Keychain diretamente em `shared/iosMain` com Kotlin/Native.
- [ ] Persistir somente após validação em `/v1/key/info`.
- [ ] Integrar salvar, consultar, substituir, revalidar e remover.

### TASK-05 — Rede, domínio e erros

- [ ] Configurar cliente Ktor/Ktorfit e autenticação por header.
- [ ] Criar DTOs, mapeadores, modelos de domínio e erros tipados.
- [ ] Redigir headers e dados sensíveis nos logs.
- [ ] Implementar tratamento específico para `401`, `403`, `429`, `5xx`,
  timeout e offline.

### TASK-06 — Banco e cache local-first

- [ ] Criar schema SQLDelight, índices, queries e upserts idempotentes.
- [ ] Configurar drivers Android/iOS, WAL, `busy_timeout` e pool controlado.
- [ ] Criar repositórios que emitam cache antes de atualizar a rede.
- [ ] Aplicar TTL por recurso sem apagar cache válido antes do novo commit.

### TASK-07 — Sincronização e polling

- [ ] Implementar paginação com paralelismo configurável e limitado.
- [ ] Persistir cada página em uma transação com backpressure.
- [ ] Calcular progresso somente após commits confirmados.
- [ ] Implementar retry transitório, cancelamento, checkpoint e retomada.
- [ ] Implementar polling de 60 segundos somente para a moeda expandida.

### TASK-08 — Integração da UI com dados reais

- [ ] Substituir fixtures pelos estados dos ViewModels e repositórios.
- [ ] Conectar onboarding, mercado, detalhes, sincronização e ajustes.
- [ ] Implementar loading, vazio, erro, restrição de plano e cache desatualizado.
- [ ] Preservar aba, busca, filtros, scroll e item expandido.

### TASK-09 — Qualidade e entrega

- [ ] Executar testes compartilhados, de plataforma, banco, UI e segurança.
- [ ] Medir benchmarks de rede, batches e pool de banco.
- [ ] Configurar CI Android e iOS sem API key real.
- [ ] Revisar acessibilidade, logs, segredos, documentação e critérios de aceite.

## 4. UI

### UI-01 — Design system

- [ ] Definir cores, tipografia, espaçamentos, formas e estados claro/escuro.
- [ ] Criar componentes reutilizáveis para cards, logos, variação, botões,
  abas, gráfico e mensagens de estado.
- [ ] Manter ícones e cards discretos, informação legível e áreas de toque
  acessíveis.

### UI-02 — Onboarding e API key

- [ ] Reproduzir `docs/mockups/01-onboarding-api-key.png`.
- [ ] Exibir marca, campo protegido, mostrar/ocultar chave e ação de validação.
- [ ] Não mostrar a chave em estado, logs, screenshots ou mensagens.
- [ ] Tratar validação, persistência, erro e carregamento separadamente.

### UI-03 — Navegação

- [ ] Criar somente os destinos principais `Mercado` e `Ajustes`.
- [ ] Preservar back stacks independentes.
- [ ] Usar bottom bar no telefone e navigation rail em larguras maiores.
- [ ] Implementar retorno nos detalhes de moeda e corretora.

### UI-04 — Mercado de moedas

- [ ] Reproduzir `02-mercado-moedas.png`.
- [ ] Exibir logo, nome, símbolo, preço, variação, corretoras e `+N`.
- [ ] Usar cards compactos, chaves estáveis e somente um item expandido.
- [ ] Exibir atualização discreta, sem “Ao vivo” ou “Tempo real”.

### UI-05 — Busca e filtros

- [ ] Reproduzir `03-mercado-busca-filtros.png`.
- [ ] Animar abertura da busca e aplicar debounce local.
- [ ] Filtrar por capitalização/preço, variação e corretora.
- [ ] Implementar ações `Limpar` e `Aplicar`.

### UI-06 — Moeda expandida

- [ ] Reproduzir `04-mercado-moeda-expandida.png`.
- [ ] Exibir preço, variação, última atualização, gráfico, mínimo, máximo e volume.
- [ ] Exibir corretoras e ação `Ver todas`.
- [ ] Manter estados independentes para cotação, histórico e mercados.

### UI-07 — Corretoras e detalhe

- [ ] Reproduzir `05-mercado-corretoras.png`.
- [ ] Listar logo, nome, ranking, volume, lançamento e quantidade de moedas.
- [ ] Criar detalhe com todos os campos obrigatórios e ativos da exchange.
- [ ] Exibir “Não informado” para campos ausentes, sem inventar valores.

### UI-08 — Sincronização

- [ ] Reproduzir `06-sincronizacao.png`.
- [ ] Exibir percentual confirmado, `X de Y`, barra, fase e ações.
- [ ] Permitir continuar navegando ou cancelar.
- [ ] Não expor detalhes internos de threads, WAL, pool ou batches.

### UI-09 — Ajustes e estados globais

- [ ] Exibir status e uso da API key sem revelar seu valor.
- [ ] Permitir revalidar, substituir e remover a credencial.
- [ ] Exibir última sincronização, sincronização manual e limpeza de cache.
- [ ] Cobrir loading, vazio, offline, stale cache, erro e restrição de plano.

## 5. Core

### CORE-01 — Organização e injeção

- [ ] Organizar `core`, `network`, `database`, `security`, `data`, `domain` e
  `presentation` dentro do módulo `shared`.
- [ ] Criar `platformModule`, `networkModule`, `databaseModule`,
  `securityModule`, `repositoryModule`, `useCaseModule` e `viewModelModule`.
- [ ] Criar composition roots Android e iOS sem service locator no domínio.

### CORE-02 — Configuração de processamento

- [ ] Criar `CryptoProcessConfig` e uma única fonte de parâmetros.
- [ ] Começar com HTTP paralelo 10, página 100, buffer 10 e polling 60 s.
- [ ] Começar com pool SQL 2 no Android e 1 no iOS.
- [ ] Permitir ajustes por plataforma sem condicionais espalhadas.

### CORE-03 — Armazenamento seguro Android

- [ ] Gerar chave AES-256 não exportável no `AndroidKeyStore`.
- [ ] Configurar `AES/GCM/NoPadding` para criptografia e descriptografia.
- [ ] Usar IV/nonce aleatório e exclusivo gerado pelo `Cipher` em cada operação.
- [ ] Persistir envelope versionado com ciphertext, IV e metadados mínimos.
- [ ] Usar a `SecretKey` diretamente; nunca tentar exportá-la para `ByteArray`.
- [ ] Invalidar o envelope e solicitar nova chave se Keystore ou autenticação
  falharem.

#### Uso do projeto de referência

Usar como referência conceitual:

- `AndroidPlatformCryptoKeyManager.kt`: alias, criação AES, modo GCM e tamanho
  de 256 bits.
- `AndroidPlatformAesEngine.kt`: `Cipher`, IV aleatório, Base64 e
  `GCMParameterSpec` na leitura.
- `EncryptedPayloadModel.kt`: separação entre IV e ciphertext.

Não copiar o contrato baseado em `getKeyBytes()`: chaves do Android Keystore são
não exportáveis e `SecretKey.encoded` pode retornar `null`. O CryptoView deve
manter a chave dentro do Keystore e entregar a própria `SecretKey` ao `Cipher`.

### CORE-04 — Armazenamento seguro iOS

- [ ] Implementar o `actual` do contrato em `shared/iosMain` usando o framework
  `Security` por interop Kotlin/Native.
- [ ] Salvar como `kSecClassGenericPassword`, com `service` e `account` estáveis.
- [ ] Usar acessibilidade `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` ou
  alternativa `ThisDeviceOnly` justificada pelo ciclo do aplicativo.
- [ ] Implementar create/update, read e delete tratando códigos `OSStatus`.
- [ ] Guardar no DataStore apenas metadados não sensíveis.

O módulo Swift `TbsIOSCryptoUtils` do projeto de referência não será criado nem
integrado. O Keychain já fornece proteção do sistema; não haverá módulo
CryptoKit, Pod ou framework iOS adicional apenas para cifrar a API key.

### CORE-05 — API CoinMarketCap

- [ ] Validar credencial em `/v1/key/info` antes de persistir.
- [ ] Enviar a chave somente no header `X-CMC_PRO_API_KEY`.
- [ ] Criar contratos Ktorfit e mapeadores nullable.
- [ ] Usar `MockEngine` em testes e nunca uma credencial real na CI.

### CORE-06 — Persistência e cache

- [ ] Persistir moedas, cotações, metadados, exchanges, ativos, market pairs,
  histórico e checkpoints.
- [ ] Implementar transações por página, índices e upsert idempotente.
- [ ] Configurar WAL, timeout e checkpoint periódico.
- [ ] Manter SQLite com um escritor efetivo, apesar do pool de conexões.

### CORE-07 — Repositórios local-first

- [ ] Emitir cache imediatamente por Flow.
- [ ] Verificar TTL e atualizar remotamente quando necessário.
- [ ] Persistir atomicamente e deixar a UI observar o banco.
- [ ] Manter dados anteriores quando a rede falhar e marcar stale cache.

### CORE-08 — Sincronização

- [ ] Planejar páginas com base em `total_cryptocurrencies` e no limite real do
  endpoint/plano.
- [ ] Usar `flatMapMerge` com concorrência limitada e `buffer` com backpressure.
- [ ] Persistir cada página assim que retornar, sem acumular tudo em memória.
- [ ] Atualizar `SyncState` somente após commit.
- [ ] Implementar retry limitado, `Retry-After`, cancelamento e retomada.

### CORE-09 — Polling e lifecycle

- [ ] Emitir cache e atualizar imediatamente ao expandir a moeda.
- [ ] Repetir `/v3/cryptocurrency/quotes/latest` a cada 60 s.
- [ ] Cancelar ao recolher, trocar de moeda, sair da tela ou ir ao background.
- [ ] Evitar chamadas duplicadas por recomposição.

### CORE-10 — Observabilidade e segurança transversal

- [ ] Criar erros de domínio e logs estruturados sem segredos.
- [ ] Medir duração de páginas, batches, espera de pool e retries.
- [ ] Não registrar API key, plaintext, ciphertext, nonce ou headers sensíveis.
- [ ] Descriptografar/ler o segredo no menor escopo necessário para a requisição.

## 6. Matriz requisito–endpoint–campo

| Requisito | Endpoint principal | Campos mínimos |
|---|---|---|
| Validar chave e uso | `/v1/key/info` | plano, uso, limites e status |
| Total para sincronização | `/v1/global-metrics/quotes/latest` | `total_cryptocurrencies` |
| Lista de moedas | `/v3/cryptocurrency/listings/latest` | id, name, symbol, quote/price, percent_change_24h, volume_24h |
| Metadados da moeda | `/v2/cryptocurrency/info` | logo, description e URLs |
| Cotação/polling | `/v3/cryptocurrency/quotes/latest` | price, percent_change_24h, volume_24h, last_updated |
| Histórico/gráfico | `/v3/cryptocurrency/quotes/historical` | timestamp e quote/price |
| Mercados da moeda | `/v2/cryptocurrency/market-pairs/latest` | exchange, pair, price e volume |
| Lista de exchanges | `/v1/exchange/listings/latest` | id, name, spot_volume_usd, date_launched |
| Metadados da exchange | `/v1/exchange/info` | logo, description, URLs, maker_fee e taker_fee |
| Ativos da exchange | `/v1/exchange/assets` | currency.name e currency.price_usd |
| Pares da exchange | `/v1/exchange/market-pairs/latest` | pares e métricas da exchange |

Todos os campos opcionais devem ser nullable. Restrições `403` de plano não
devem ser tratadas automaticamente como API key inválida.

## 7. Tests

### TEST-01 — Domínio e estado compartilhado

- [ ] Validação da API key, casos de uso, DTOs e mapeadores.
- [ ] Erros `401`, `403`, `429`, `5xx`, timeout e offline.
- [ ] ViewModels, ações, efeitos e transições de estado.
- [ ] Busca, filtros e preservação do item expandido.

### TEST-02 — Segurança

- [ ] Contrato fake: salvar, ler, substituir e remover.
- [ ] Android: round trip AES-GCM, IVs diferentes e falha após adulteração.
- [ ] Android: chave não exportável e envelope inválido/ausente.
- [ ] iOS: criar, atualizar, ler e remover item do Keychain.
- [ ] Verificar ausência do segredo em logs, estado de UI e armazenamento comum.

### TEST-03 — Rede e repositórios

- [ ] Respostas válidas, nullable, serialização inválida e códigos HTTP.
- [ ] Header de autenticação presente e redigido nos logs.
- [ ] Cache hit, stale cache, refresh, falha remota e persistência atômica.
- [ ] Garantir que testes usem Ktor `MockEngine`, nunca a API real.

### TEST-04 — Banco

- [ ] Upsert idempotente e rollback de batch incompleto.
- [ ] Índices e consultas de busca, filtros e relacionamentos.
- [ ] Leituras durante escrita, múltiplas conexões e timeout.
- [ ] WAL, checkpoint, retomada e fechamento seguro do pool.

### TEST-05 — Concorrência e sincronização

- [ ] Limite real de requisições simultâneas.
- [ ] Buffer/backpressure entre rede e banco.
- [ ] Progresso incrementado somente após commit.
- [ ] Falha parcial, retry, `Retry-After`, cancelamento e retomada.
- [ ] Ausência de coroutine ilimitada e de acúmulo de todas as páginas.

### TEST-06 — Polling

- [ ] Início somente para a moeda selecionada.
- [ ] Intervalo de 60 s controlado por scheduler de teste.
- [ ] Cancelamento ao recolher, trocar, navegar ou entrar em background.
- [ ] Troca rápida sem manter polling obsoleto.

### TEST-07 — Interface Android

- [ ] Onboarding, chave inválida e estado de validação.
- [ ] Abas, busca, filtros, lista e card expandido.
- [ ] Corretoras, detalhe e ativos obrigatórios.
- [ ] Modal de sincronização, ajustes, offline e cache desatualizado.
- [ ] Rotação, tamanhos diferentes, acessibilidade e navegação completa.

### TEST-08 — Snapshots e adaptação

- [ ] Criar previews/snapshots dos seis mockups aprovados.
- [ ] Cobrir telefone, tablet/foldable e escalas de fonte relevantes.
- [ ] Comparar alterações sem atualizar baselines automaticamente.

### TEST-09 — iOS, integração e CI

- [ ] Compilar framework e host iOS em runner macOS.
- [ ] Inicializar Koin e interface Compose no iOS.
- [ ] Executar testes compartilhados nos targets iOS aplicáveis.
- [ ] Configurar CI para build Android, testes, análise estática e build iOS.

### TEST-10 — Benchmark

- [ ] Comparar rede sequencial, paralelismo 5 e paralelismo 10.
- [ ] Comparar pool 1 e 2 no Android.
- [ ] Comparar tamanhos de batch.
- [ ] Registrar dispositivo, volume, metodologia e resultados no README.

## 8. Riscos e limitações conhecidos

- O plano Basic da CoinMarketCap não fornece WebSocket e possui atualização
  aproximada de 60 segundos.
- Histórico, market pairs e outros endpoints podem ser bloqueados pelo plano;
  a UI deve mostrar indisponibilidade sem fabricar dados.
- `429` exige respeito ao `Retry-After`; excesso de paralelismo pode piorar a
  sincronização.
- WAL permite leitura durante escrita, mas não transforma SQLite em banco com
  vários escritores físicos simultâneos.
- Chaves do Android Keystore podem ser invalidadas; o aplicativo deve limpar o
  envelope inconsistente e solicitar nova configuração.
- O Keychain precisa ser validado em macOS/iOS real ou em CI apropriada.
- Dependências KMP evoluem em ritmos diferentes; upgrades devem ser feitos como
  conjunto compatível e acompanhados de build/testes.
- Não deve haver afirmação de teste iOS manual sem evidência de macOS/Xcode.

## 9. Critérios de conclusão

- Android e target iOS compilam nas plataformas adequadas.
- API key é validada e nunca persistida em texto puro.
- Android usa Keystore/AES-GCM; iOS usa Keychain sem módulo Swift adicional.
- Listas e detalhes de moedas/exchanges usam o banco como fonte de verdade.
- Sincronização possui limite verificável, backpressure, batches e retomada.
- Progresso representa commits confirmados.
- Polling existe apenas para a moeda expandida e respeita lifecycle.
- Cache offline, busca, filtros, erros e restrições de plano funcionam.
- Testes, CI, benchmarks e README apresentam evidências reais.
- Não restam TODOs relacionados a requisitos obrigatórios.

## 10. Dependências entre frentes

```text
Bootstrap
   ├── UI mockada e navegável
   ├── Segurança da API key ──> autenticação da rede
   └── Banco + rede ──> repositórios local-first
                         └── sincronização ──> UI com dados reais
                                              └── polling e detalhes

Testes acompanham cada frente; qualidade, CI e benchmark encerram a entrega.
```

