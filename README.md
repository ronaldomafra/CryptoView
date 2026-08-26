# CryptoView

Aplicativo Kotlin Multiplatform criado para o [desafio mobile do Mercado Bitcoin](https://github.com/mb-desafio/querosermb). O app consulta moedas e corretoras, mantém os dados localmente e compartilha interface, estado e regras de negócio entre Android e iOS. O foco técnico está na sincronização concorrente, na persistência reativa com SQLite e no tratamento seguro da API key.

## Principais recursos

- Onboarding com validação e armazenamento seguro da API key.
- Listas paginadas de moedas e corretoras, busca e filtros locais.
- Detalhe da moeda com cotação, gráfico por período, mercados e atualização a cada 60 segundos enquanto expandida.
- Detalhe da corretora com metadados e preços dos ativos.
- Informações complementares da moeda pela API pública da CoinPaprika, extensão adicionada além do escopo do desafio.
- Sincronização incremental com progresso, cancelamento, checkpoint e retomada.
- Cache local-first: a interface observa o banco e continua útil quando a rede falha.
- Tema claro/escuro e layout adaptável para telefone e telas maiores.

## Telas Android

<table>
  <tr>
    <td align="center"><strong>Splash</strong></td>
    <td align="center"><strong>Onboarding</strong></td>
    <td align="center"><strong>Mercado</strong></td>
    <td align="center"><strong>Detalhe da moeda</strong></td>
  </tr>
  <tr>
    <td><img src="docs/telas/android-splash.png" width="190" alt="Splash do CryptoView no Android" /></td>
    <td><img src="docs/telas/android-onboarding.png" width="190" alt="Onboarding Android" /></td>
    <td><img src="docs/telas/android-mercado.png" width="190" alt="Mercado de moedas no Android" /></td>
    <td><img src="docs/telas/android-detalhe-moeda.png" width="190" alt="Detalhe expandido da moeda no Android" /></td>
  </tr>
  <tr>
    <td align="center"><strong>Busca</strong></td>
    <td align="center"><strong>Filtros</strong></td>
    <td align="center"><strong>Sincronização</strong></td>
    <td align="center"><strong>Ajustes</strong></td>
  </tr>
  <tr>
    <td><img src="docs/telas/android-busca.png" width="190" alt="Busca de moedas no Android" /></td>
    <td><img src="docs/telas/android-filtros.png" width="190" alt="Filtros do mercado no Android" /></td>
    <td><img src="docs/telas/android-sincronizacao.png" width="190" alt="Progresso da sincronização no Android" /></td>
    <td><img src="docs/telas/android-ajustes.png" width="190" alt="Ajustes e estado dos dados no Android" /></td>
  </tr>
</table>

O aplicativo também foi validado manualmente no iOS. As capturas dessa plataforma serão adicionadas posteriormente.

## Arquitetura e estado

O projeto utiliza **MVVM**, fluxo unidirecional de dados e abordagem **local-first**. Essa foi uma decisão arquitetural específica para o desafio, com o objetivo de demonstrar sincronização, resiliência e uso reativo do SQLite; não pressupõe que o aplicativo real do Mercado Bitcoin siga a mesma abordagem.

Os ViewModels compartilhados expõem estados imutáveis por `StateFlow`; a UI envia eventos e observa o estado considerando o ciclo de vida. O SQLDelight é a fonte de verdade para as telas, portanto cada lote confirmado no banco atualiza os fluxos observados e a interface progressivamente.

```mermaid
flowchart LR
    UI[Compose Multiplatform UI] -->|Eventos| VM[ViewModels compartilhados]
    VM -->|StateFlow| UI
    VM --> REPO[Repositórios]
    REPO --> POOL[Pool SQLDelight]
    POOL --> DB[(SQLite com WAL)]
    DB -->|Flow reativo| REPO
    REPO --> CMC[CoinMarketCap]
    REPO --> CP[CoinPaprika]
    SYNC[Coordenador de sincronização] --> CMC
    SYNC -->|Lotes transacionais| POOL
```

Responsabilidades principais:

- **UI:** Compose Multiplatform, Navigation 3, componentes reutilizáveis e estados de loading/erro/cache.
- **Estado:** ViewModels compartilhados, `StateFlow`, eventos explícitos e estado imutável.
- **Domínio:** repositórios e modelos sem dependência das telas.
- **Dados:** Ktor/Ktorfit, SQLDelight, pool de conexões, cache por recurso e mapeamento de DTOs.
- **Injeção:** Koin com módulos de rede, banco, segurança, repositórios e ViewModels.

### Escolha da interface multiplataforma

Não foi criada uma segunda implementação das telas em Swift 5/SwiftUI porque o Compose Multiplatform atende integralmente ao escopo e mantém a experiência equivalente nas duas plataformas sem duplicar estado e regras de apresentação. O código Swift permanece onde agrega valor: inicialização do host iOS e integração nativa com CryptoKit e Keychain.

## Concorrência e desempenho

A sincronização foi estruturada como um pipeline de `Flow`, separando concorrência de rede e concorrência de banco. Isso permite aproveitar o paralelismo de I/O sem transferir a mesma pressão para o SQLite.

- Downloads de páginas e metadados são executados em paralelo, com limites configurados por plataforma.
- Um buffer aplica backpressure entre download e persistência; falhas cancelam os trabalhos relacionados de forma estruturada.
- Escritas são agrupadas em lotes e executadas dentro de transações, reduzindo commits e invalidações desnecessárias.
- Um pool reutilizável, limitado por semáforo e preparado para até quatro conexões, controla o acesso ao banco. A concorrência de escrita atual é conservadora para reduzir contenção entre writers.
- SQLite opera com WAL, `synchronous=NORMAL`, `busy_timeout` e checkpoints passivos periódicos.
- Após cada commit, versões por recurso são publicadas em `StateFlow`; somente consultas interessadas são refeitas, e as listas recebem os novos lotes sem reiniciar a paginação.

## Sincronização

A sincronização valida a credencial e a cota, restaura checkpoints e processa as páginas em paralelo. Cada lote só avança o checkpoint e o progresso depois do commit, permitindo cancelamento e retomada sem considerar como persistidos dados que ainda estavam apenas em memória.

```mermaid
flowchart TD
    A[Iniciar sincronização] --> B[Validar API key e cota]
    B --> C[Restaurar checkpoint]
    C --> D[Flow de páginas]
    D --> E[Downloads paralelos]
    E --> F[Buffer e backpressure]
    F --> G[Transações em lote]
    G --> H[Pool SQLDelight]
    H --> I[(SQLite WAL)]
    I --> J[Commit e checkpoint]
    J --> K[StateFlow por recurso]
    K --> L[UI atualizada progressivamente]
```

Histórico, mercados da moeda, ativos da corretora e informações CoinPaprika ficam fora da sincronização global e são carregados apenas quando necessários.

## Extensão com CoinPaprika

Como iniciativa além dos requisitos do desafio, integrei a API pública da CoinPaprika para enriquecer o detalhe das moedas com informações complementares. A associação com a CoinMarketCap é validada de forma estrita, o conteúdo é carregado sob demanda e fica em cache local, sem aumentar o custo da sincronização principal. Considerei essa informação útil para tornar a tela de detalhe mais completa e demonstrar a integração segura de fontes diferentes.

## Segurança da API key

A segurança da credencial foi tratada como requisito central: a API key nunca é armazenada em texto puro nem mantida no estado da interface.

- **Android:** chave AES-256 não exportável no Android Keystore e envelope AES-GCM persistido no DataStore.
- **iOS:** CryptoKit e chave protegida pelo Keychain.
- A API key não faz parte do estado da UI e é recuperada somente no limite da requisição autenticada.
- Headers e credenciais são redigidos dos logs.

## Tecnologias

| Área | Tecnologia |
|---|---|
| Plataforma | Kotlin Multiplatform, Android e iOS |
| Interface | Compose Multiplatform, Material 3, Navigation 3 |
| Estado | MVVM, StateFlow, Coroutines e Flow |
| Rede | Ktor, Ktorfit e Kotlin Serialization |
| Persistência | SQLDelight, SQLite/WAL, pool limitado e transações por lote |
| Injeção | Koin |
| Imagens | Coil |

## Executando o projeto

Pré-requisitos: JDK 17, Android Studio com Android SDK 36 e, para iOS, macOS com Xcode.

```bash
# Compilar o aplicativo Android
bash gradlew :androidApp:assembleDebug

# Executar os testes compartilhados no host Android
bash gradlew :shared:testAndroidHostTest
```

Para Android, abra o projeto no Android Studio e execute `androidApp`. Para iOS, abra `iosApp/iosApp.xcodeproj` no Xcode. A API key da CoinMarketCap é informada e validada no primeiro acesso; nenhuma credencial precisa ser adicionada ao código.

## Validação

- 53 testes automatizados de domínio, rede, segurança, sincronização, paginação e banco aprovados no host Android.
- Build Android e compilação compartilhada para iOS aprovados.
- Fluxos principais validados manualmente em dispositivos Android e iOS.

## Limitações conhecidas

- O plano gratuito da CoinMarketCap pode responder `403` para corretoras, ativos, market pairs ou histórico. A falha é isolada e não impede a sincronização das moedas disponíveis.
- Não existe fallback CoinPaprika para corretoras. A CoinPaprika é usada somente para informações complementares das moedas.
- A associação entre CoinMarketCap e CoinPaprika é estrita; quando não há correspondência segura, a ação de informações permanece indisponível.
- A galeria atual contém apenas capturas Android.

Dados de mercado: [CoinMarketCap](https://coinmarketcap.com/api/). Informações complementares: [CoinPaprika](https://coinpaprika.com/api/).
