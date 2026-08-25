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
- [x] Menu e tela temporária para testes da integração CoinMarketCap
- [ ] Testes de UI e snapshots

## Core

- [x] Koin e composition roots
- [x] ViewModel, Repository e UseCase registrados e compostos pelo Koin
- [x] Contrato `SecureApiKeyStorage`
- [x] Android Keystore + AES-GCM + DataStore
- [x] Swift CryptoKit + Keychain em `iosApp`, sem CocoaPods
- [x] Cliente Ktor/Ktorfit da CoinMarketCap
- [x] Clientes HTTP por plataforma com `expect/actual` — CIO no Android e Darwin no iOS
- [x] Service Ktorfit declarativo retornando `Flow<Response<...>>`
- [x] Tratamento comum de resposta, erros HTTP, erros da API e cancelamento de coroutine
- [x] Fluxo MVVM: UI → ViewModel → Repository → UseCase → Service
- [x] Validação de parâmetros no UseCase antes da operação de rede
- [x] Consulta `GET /v1/key/info` com conversão para modelos de domínio
- [x] Remover `TempUtils.API_KEY` fixa do código
- [x] Integrar salvar/ler/status/remover à tela temporária de testes
- [ ] Remover a tela temporária antes da distribuição
- [ ] SQLDelight, WAL e pool de conexões
- [ ] Repositórios local-first e políticas de cache
- [ ] Sincronização, backpressure, retry e checkpoint
- [ ] Polling real da moeda expandida

## Validação

- [x] Configurar Android SDK local
- [x] Compilar Android
- [x] Percorrer manualmente a navegação no dispositivo Android
- [x] Validar manualmente a consulta CoinMarketCap pela tela temporária
- [x] Compilar o código compartilhado para `iosSimulatorArm64`
- [ ] Compilar framework iOS em macOS
- [x] Executar testes automatizados
- [x] Testar contrato seguro: round-trip, falhas, recuperação e remoção
- [x] Validar singleton do Repository e factories de ViewModel/UseCase no grafo Koin
- [ ] Registrar benchmarks e documentação final
