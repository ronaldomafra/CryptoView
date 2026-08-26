# Plano de sincronização — histórico aprovado

Plano aprovado e implementado em 25/08/2026. Este arquivo preserva as decisões que orientaram o trabalho.

Refatoração posterior simplificou a implementação para um único coordenador e dois parâmetros genéricos: paralelismo de IO e paralelismo de banco. O comportamento de batch, checkpoint, cancelamento e retomada foi preservado.

## Tasks

1. Integrar a credencial segura ao startup sem expor plaintext.
2. Criar banco SQLDelight, drivers Android/iOS, WAL e pool limitado.
3. Criar um coordenador de execução única com preflight, passos, progresso, cancelamento e retomada.
4. Sincronizar corretoras, metadados, moedas e metadados de moedas em passos sequenciais.
5. Executar páginas em paralelo dentro de cada passo, com rate limit, buffer e retry limitado.
6. Persistir cada página imediatamente em transação e gravar checkpoint somente após commit.
7. Carregar histórico/mercados da moeda e ativos da corretora sob demanda.
8. Atualizar a cotação a cada 60 segundos apenas enquanto uma moeda estiver expandida.

## UI

- Banco local como fonte das listas de moedas e corretoras.
- Observação contínua do prefixo paginado para publicar cada batch confirmado durante a sincronização.
- Progresso calculado por itens persistidos, não apenas recebidos.
- Falhas de histórico, mercados e ativos isoladas da tela principal.
- Ajustes com estado da chave, uso da API, sincronização e limpeza de cache.
- Somente `Mercado` e `Ajustes` na navegação principal.

## Core

- Ordem dos passos: validação da credencial/cota → corretoras → metadados de corretoras → moedas → metadados de moedas → finalização.
- Corretoras essenciais podem ser atualizadas no startup conforme TTL; catálogo completo de moedas é inicial/manual.
- Downloads em ondas limitadas e persistência incremental, sem manter o catálogo inteiro em memória.
- Paralelismo genérico IO/banco de 20/2 no Android e 20/1 no iOS; SQLite continua com escritor físico único.
- Entidades-pai usam `INSERT OR IGNORE` + `UPDATE` na mesma transação para evitar o delete implícito de `REPLACE` e seus efeitos em cascata.
- Cotações, metadados, relações, ativos, histórico, estado de cache e checkpoints usam snapshots idempotentes.
- Reserva de cota preserva etapas essenciais; `403` de recurso não invalida automaticamente a chave.
- Nenhum segredo em DTO de domínio, estado de UI, banco de mercado, checkpoint ou log.

## Tests

- Parsing e mapeamento de respostas flexíveis.
- Reserva de cota e progresso limitado a 0–100%.
- Upsert/rollback/FK, múltiplas conexões e WAL.
- Paralelismo, backpressure, retry, cancelamento e retomada.
- Polling e troca rápida de moeda.
- Fluxos de onboarding, ajustes e restrição de plano.

## Decisões de escopo

- Não foi criado módulo iOS adicional nem adotado CocoaPods.
- A arquitetura do projeto de referência foi usada como inspiração, não copiada integralmente.
- Histórico não participa da sincronização global.
- Histórico é consultado sob demanda por moeda/período (`24H`, `7D`, `30D`, `1A`), com até 100 pontos por chamada e cache independente.
- Relações moeda–corretora são carregadas por demanda porque o endpoint é por moeda e pode depender do plano.
- A chave de desenvolvimento nunca deve ser salva em código, documentação, fixture, comando ou log.

## Validações posteriores — 26/08/2026

- Android executado em dispositivo real sem crash, `FATAL EXCEPTION` ou bloqueio do SQLite.
- Configuração WAL aplicada de forma consistente antes da abertura das conexões.
- Restrição `403` no catálogo de corretoras tratada como resultado parcial; métricas e páginas de moedas continuam sendo sincronizadas.
- API key confirmada como redigida nos logs Android.
- Aplicativo iOS testado manualmente em macOS/Xcode e aprovado.
- Permanecem pendentes a automação iOS em runner macOS, o roteiro visual completo Android e os benchmarks de concorrência/batches.
