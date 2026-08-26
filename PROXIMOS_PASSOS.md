# CryptoView — próximos passos

Checkpoint de implementação em 26/08/2026.

## Estado atual

O marco de API key, banco local-first e sincronização foi implementado. O app inicia pelo estado real da credencial segura, executa uma sincronização em etapas e apresenta dados do SQLDelight. Moedas, corretoras e metadados entram no banco em batches à medida que as páginas retornam, e as listas observam cada commit para exibir esses dados progressivamente sem aguardar o encerramento da sincronização. Histórico, mercados de uma moeda e ativos da corretora permanecem sob demanda.

As informações descritivas da moeda também permanecem sob demanda. O ID CoinPaprika é pré-resolvido com correspondência estrita durante a expansão do card, enquanto a tela de informações executa no máximo uma chamada de detalhe e reutiliza cache local por 24 horas.

O desenho adotado é uma versão simplificada do projeto de referência: guarda de execução única, preflight de credencial/cota, passos sequenciais, páginas paralelas dentro de cada passo, metadata em lotes concorrentes de até 250 IDs, pool limitado, transações por página, checkpoints e retomada. O rate limiter libera os requests concorrentes dentro da cota do plano e aguarda a próxima janela ao atingir o total por minuto. A API key não é propagada pelo pipeline; ela é obtida somente pelo executor autenticado no instante da requisição.

A execução Android em dispositivo real está estável, sem crash ou bloqueio do SQLite, e continua a sincronização das moedas quando o catálogo de corretoras retorna `403` por restrição do plano. O aplicativo iOS foi validado manualmente e aprovado em macOS/Xcode em 26/08/2026.

## Próximo marco — robustez e evidências

### Tasks

- Criar fakes determinísticos para o coordenador de sincronização.
- Testar retomada a partir de páginas confirmadas, cancelamento e retry transitório.
- Registrar contadores/tempos sem incluir headers ou credenciais.
- Criar CI Android e runner macOS para o target iOS.

### UI

- Restaurar filtros locais de preço, variação e corretora sem chamadas por caractere.
- Exibir idade do cache e aviso offline não bloqueante.
- Refinar estados vazio, loading e restrição de plano por seção.
- Executar revisão visual/acessibilidade nos dispositivos de referência.

### Core

- Testar batches, rollback, chaves estrangeiras, pool e leituras durante escrita.
- Validar a paginação real conforme a quantidade acessível ao plano da chave usada.
- Medir paralelismo 1/5/10 e pool Android 1/2 antes de alterar os valores iniciais.
- Avaliar integração explícita com lifecycle para pausar polling quando o app entra em background.

### Tests

- Banco em memória/temporário com dados de moedas, corretoras e relações.
- `MockEngine` para 401, 403, 429, 5xx, timeout e resposta parcial.
- Coordenador: execução única, cota reservada, progresso pós-commit e resume.
- ViewModels: startup, remoção da chave, expansão exclusiva e troca rápida.
- Compose: onboarding, mercado, detalhe, ajustes e modal.
- Automatização de build/testes iOS em runner macOS; validação manual já aprovada.
- Roteiro manual Android completo, incluindo navegação e dados reais.

## Critério para encerrar o próximo marco

Todos os testes de concorrência/cache devem ser determinísticos, os builds Android/iOS devem estar verdes em seus hosts, e o README deve conter resultados reais de benchmark e limitações observadas do plano CoinMarketCap.
