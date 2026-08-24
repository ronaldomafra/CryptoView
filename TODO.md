# CryptoView — checklist de desenvolvimento

Atualizar este arquivo ao concluir cada etapa. Itens marcados representam código implementado; validações por build permanecem separadas.

## UI mockada

- [x] Tema e tokens visuais do CryptoView
- [x] Modelos e fixtures para moedas, corretoras e ativos
- [x] Onboarding com campo protegido e entrada no app
- [x] Navegação principal Mercado/Ajustes
- [x] Navegação interna com retorno e back stacks independentes
- [x] Mercado com tabs Moedas/Corretoras
- [x] Busca local e painel de filtros interativos
- [x] Cards de moedas com expansão exclusiva
- [x] Gráfico mockado de 24 horas
- [x] Lista e detalhe de corretoras
- [x] Lista de mercados de uma moeda
- [x] Tela de Ajustes
- [x] Modal de sincronização com ações
- [x] Layout adaptável com bottom bar/rail
- [x] Refino de densidade: tipografia, ícones, cards e espaçamentos mais discretos
- [x] Comparação visual fina com screenshots dos mockups
- [ ] Testes de UI e snapshots

## Core

- [ ] Koin e composition roots
- [ ] Contrato `SecureApiKeyStorage`
- [ ] Android Keystore + AES-GCM + DataStore
- [ ] Swift Keychain em `iosApp`
- [ ] Cliente Ktor/Ktorfit da CoinMarketCap
- [ ] SQLDelight, WAL e pool de conexões
- [ ] Repositórios local-first e políticas de cache
- [ ] Sincronização, backpressure, retry e checkpoint
- [ ] Polling real da moeda expandida

## Validação

- [x] Configurar Android SDK local
- [x] Compilar Android
- [x] Percorrer manualmente a navegação no dispositivo Android
- [ ] Compilar framework iOS em macOS
- [ ] Executar testes automatizados
- [ ] Registrar benchmarks e documentação final
