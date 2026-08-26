# CryptoView — pacote de especificação e implementação

Aplicativo Kotlin Multiplatform/Compose Multiplatform para consultar moedas e corretoras da CoinMarketCap, com Android como plataforma principal e host iOS compartilhando UI e regras de negócio.

## Status atual — 26/08/2026

- API key integrada ao onboarding, startup e ajustes, sem rota temporária de testes.
- Android utiliza Keystore + AES-256-GCM; iOS utiliza Swift, CryptoKit e Keychain, sem CocoaPods.
- SQLDelight é a fonte de verdade para moedas, corretoras, cotações e cache por demanda.
- Sincronização coordenada com páginas paralelas, rate limit, transações por batch, pool, WAL, checkpoint, cancelamento e retomada.
- Histórico e mercados são carregados ao abrir uma moeda; o gráfico histórico consulta diretamente a API com opções `24H`, `7D`, `30D` e `1A`, mantendo cache independente por período.
- Polling de 60 segundos ocorre somente para a moeda expandida.
- Build Android, migração SQLDelight e 33 testes Android host aprovados.
- Sincronização apresentada com progresso horizontal por etapas; ao fechar o modal, a execução continua em segundo plano e pode ser reaberta pelo indicador da top bar.
- Inicialização e sincronização Android validadas em dispositivo real, sem crash nem bloqueio do SQLite; restrições `403` do plano são tratadas sem interromper a sincronização das moedas.
- Aplicativo iOS validado manualmente em macOS/Xcode em 26/08/2026.

## Arquitetura da sincronização

O coordenador executa o preflight de credencial/cota e, em seguida, corretoras, metadados de corretoras, moedas e metadados de moedas. As etapas são sequenciais; dentro delas, um pipeline genérico de `Flow` aplica `parallelIoValue` aos downloads e `parallelDbValue` às transações. Moedas e corretoras usam a mesma configuração, e o checkpoint só é confirmado depois do commit.

O processamento usa IO/banco `20/2` no Android e `20/1` no iOS. WAL melhora a convivência entre leitura e escrita, mas o SQLite continua serializando o escritor físico. Entidades-pai usam `INSERT OR IGNORE` seguido de `UPDATE`; snapshots usam `INSERT OR REPLACE`.

A API key não é mantida em estado de UI nem propagada nos passos. O executor autenticado lê o armazenamento seguro apenas no limite de cada requisição.

## Documentação

- [`TODO.md`](TODO.md): checklist e validações pendentes.
- [`PROXIMOS_PASSOS.md`](PROXIMOS_PASSOS.md): próximo marco recomendado.
- [`PLANO_SINCRONIZACAO.md`](PLANO_SINCRONIZACAO.md): plano aprovado e decisões do sincronizador.
- [`PLANO_CRIPTOGRAFIA_API_KEY.md`](PLANO_CRIPTOGRAFIA_API_KEY.md): decisões e histórico de segurança.
- [`PLANO_ORIGINAL.md`](PLANO_ORIGINAL.md): plano-base preservado para histórico.
- [`PROMPT_DESENVOLVIMENTO.md`](PROMPT_DESENVOLVIMENTO.md): especificação consolidada do desafio.

## Referências visuais

- `docs/mockups/01-onboarding-api-key.png`
- `docs/mockups/02-mercado-moedas.png`
- `docs/mockups/03-mercado-busca-filtros.png`
- `docs/mockups/04-mercado-moeda-expandida.png`
- `docs/mockups/05-mercado-corretoras.png`
- `docs/mockups/06-sincronizacao.png`

A UI compartilhada segue esse conjunto visual no onboarding, mercado, busca/filtros, moeda expandida, corretoras, sincronização e navegação. Os filtros operam sobre o SQLDelight; a opção de corretora usa somente relações de mercado já consultadas e armazenadas no dispositivo.

## Limitações atuais

- Market pairs, histórico e ativos podem retornar `403` conforme o plano; a UI mantém as demais seções disponíveis.
- A validação manual do iOS foi aprovada; a automação do build e dos testes em runner macOS ainda está pendente.
- O novo seletor de histórico ainda requer revalidação visual e funcional no iOS.
- Benchmarks de paralelismo, pool e batches ainda não foram executados.
- Estados visuais offline/desatualizado permanecem no próximo recorte.
