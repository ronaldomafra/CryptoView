# CryptoView — checklist de desenvolvimento

Checkpoint atualizado em 26/08/2026. A marcação indica código implementado; validações manuais continuam separadas.

## Tasks

- [x] Integrar o armazenamento seguro ao estado raiz, onboarding e ajustes
- [x] Remover o destino temporário `Testes` da navegação
- [x] Manter somente `Mercado` e `Ajustes` como destinos principais
- [x] Manter somente paralelismo genérico de IO e banco na configuração de processamento
- [x] Criar sincronização em etapas com validação, cancelamento e retomada
- [x] Persistir checkpoints por página confirmada
- [x] Respeitar a reserva de cota nas etapas não essenciais
- [x] Salvar moedas e corretoras incrementalmente durante o download
- [ ] Validar o fluxo completo com uma chave e plano reais nas duas plataformas

## UI

- [x] Onboarding valida e salva a chave antes de liberar o app
- [x] Mercado observa moedas e corretoras do SQLDelight
- [x] Busca local sobre o banco
- [x] Expansão exclusiva de moeda com cotação, gráfico e mercados
- [x] Gráfico histórico sob demanda com períodos `24H`, `7D`, `30D` e `1A`
- [x] Estados de carregamento, cache e restrição de plano isolados no histórico
- [x] Modal exibe o progresso real confirmado pelo banco
- [x] Modal de sincronização usa progresso horizontal por etapas e por itens persistidos
- [x] Fechar o modal mantém o processamento em segundo plano sem reabertura automática na mesma execução
- [x] Indicador de sincronização nas top bars permite reabrir o acompanhamento
- [x] Detalhe da corretora carrega ativos por demanda
- [x] Ajustes exibe uso da API, sincronização, substituição, remoção e limpeza de cache
- [x] Logos remotos possuem fallback local discreto
- [x] Layout adaptável com barra inferior ou rail
- [x] Aplicar a identidade dos mockups ao onboarding, mercado, filtros, expansão, corretoras, sincronização e navegação
- [x] Filtros locais de capitalização, preço, variação e corretoras já relacionadas no cache
- [x] Valores monetários, percentuais e idade do cache formatados para a apresentação planejada
- [ ] Revisão visual e acessibilidade em tamanhos Android/iOS reais

## Core

- [x] Android Keystore + AES-256-GCM + DataStore
- [x] iOS Swift CryptoKit + Keychain, sem CocoaPods
- [x] API key descriptografada somente no limite da requisição autenticada
- [x] Endpoints de chave, métricas, moedas, metadados, cotações, histórico, mercados, corretoras e ativos
- [x] SQLDelight como fonte de verdade local
- [x] Índices, WAL, `busy_timeout` e pool controlado por `Semaphore`/`Mutex`
- [x] Upsert seguro de entidades-pai e `INSERT OR REPLACE` para snapshots
- [x] Pipeline genérico de páginas com paralelismo IO/banco, backpressure e batch transacional
- [x] Retry limitado para erros transitórios e suporte a `Retry-After`
- [x] Sincronização inicial completa até a última página disponível
- [x] Startup atualiza somente corretoras essenciais quando o cache expirou
- [x] Metadados de moedas/corretoras em lote
- [x] Histórico, mercados e ativos fora da sincronização global
- [x] Histórico V3 com parsing flexível e no máximo 100 pontos/um crédito por consulta
- [x] Cache histórico separado por moeda/período com migração dos dados existentes para `24H`
- [x] Polling de 60 segundos somente para a moeda expandida
- [ ] Instrumentar métricas e executar benchmark pool 1×2 e paralelismo 1×5×10
- [ ] Avaliar um tratamento explícito de lifecycle/background além da saída da composição

## Tests e validação

- [x] Testes de armazenamento seguro e consulta de chave
- [x] Testes de parsing das duas formas de cotação USD
- [x] Testes de reserva de cota e percentual de progresso
- [x] Teste Android host: 33 testes aprovados em 26/08/2026
- [x] Verificação da migração SQLDelight aprovada
- [x] Compilação `:shared:compileAndroidMain` aprovada
- [x] Compilação cruzada `:shared:compileKotlinIosSimulatorArm64` aprovada no host Linux
- [x] Validar inicialização e sincronização Android no Logcat, sem crash nem bloqueio do SQLite
- [x] Confirmar que a API key permanece redigida nos logs Android
- [x] Confirmar que o `403` de corretoras não bloqueia métricas e sincronização de moedas
- [x] Validar manualmente o aplicativo iOS em macOS/Xcode após a integração (26/08/2026)
- [ ] Revalidar no iOS o seletor e as consultas históricas por período
- [ ] Testes do banco: upsert, rollback, FK, retomada e múltiplas conexões
- [ ] Testes do coordenador: paralelismo, backpressure, retry, cancelamento e checkpoint
- [ ] Testes de ViewModel: startup, polling, troca rápida e erros independentes
- [ ] Testes Compose das telas e snapshots
- [ ] Automatizar build e testes iOS em runner macOS
- [x] Executar build completo `:androidApp:assembleDebug`
- [ ] Executar roteiro manual Android com dados reais

## Próximo recorte recomendado

1. Testes determinísticos do banco e do sincronizador com drivers/fakes.
2. Roteiro manual de sincronização real, incluindo plano Basic/403 e retomada.
3. Roteiro visual nos tamanhos Android/iOS reais e estados offline/desatualizado.
4. Benchmark, acessibilidade, CI e documentação final de entrega.
