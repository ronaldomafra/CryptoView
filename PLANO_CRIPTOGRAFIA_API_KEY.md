# CryptoView — plano de criptografia da API key

> Documento histórico registrado em 25/08/2026.
>
> Escopo: armazenamento seguro da API key da CoinMarketCap no Android e iOS.
> A gestão do histórico Git e da credencial anteriormente utilizada será feita
> separadamente pelo responsável pelo projeto.

## 1. Decisão arquitetural

- Implementar AES-256-GCM nas duas plataformas, mantendo o formato conceitual do
  projeto de referência `C:\Projetos\Android\tbsales-multi`.
- Android: chave AES não exportável no Android Keystore.
- iOS: implementação Swift local no `iosApp`, usando CryptoKit e Keychain.
- Integrar a implementação Swift ao código compartilhado por reverse import:
  uma interface Kotlin será implementada pelo Swift e injetada no
  `MainViewController`.
- Não criar Pod, podspec, XCFramework auxiliar, módulo Swift separado ou
  integração CocoaPods.
- Persistir somente o envelope cifrado no DataStore KMP.
- Remover do aplicativo o uso de `TempUtils.API_KEY` e substituir o fluxo
  temporário pelo onboarding real.

O Keychain já protege itens sensíveis no iOS. A camada AES adicional será usada
para manter simetria com o projeto de referência e demonstrar conhecimento de
Swift/CryptoKit, sem ser apresentada como substituta do Keychain.

## 2. Contratos compartilhados

Criar em `commonMain`:

```kotlin
interface SecureApiKeyStorage {
    suspend fun status(): SecureApiKeyStatus
    suspend fun save(apiKey: String): SecureStorageResult<Unit>
    suspend fun read(): SecureStorageResult<String>
    suspend fun remove(): SecureStorageResult<Unit>
}
```

Contrato público implementado no Android e pelo Swift:

```kotlin
interface PlatformApiKeyCipher {
    fun encrypt(plainText: String): EncryptedApiKeyEnvelope?
    fun decrypt(envelope: EncryptedApiKeyEnvelope): String?
    fun deleteKey(): Boolean
}
```

Não propagar exceptions pela fronteira Swift–Kotlin. A implementação Swift
captura erros do CryptoKit, Keychain e Base64 e retorna `null` ou `false`; a
camada compartilhada converte o resultado em erros tipados.

Envelope persistido:

```kotlin
data class EncryptedApiKeyEnvelope(
    val version: Int,
    val nonceBase64: String,
    val cipherTextAndTagBase64: String,
)
```

## 3. Formato criptográfico

- Versão inicial do envelope: `1`.
- Algoritmo: AES-256-GCM.
- Nonce aleatório de 12 bytes para cada criptografia.
- Tag de autenticação de 16 bytes anexada ao ciphertext.
- Nonce e `ciphertext + tag` codificados em Base64 sem quebras de linha.
- Nenhuma chave, IV ou nonce fixos.
- Sem AAD na versão 1, mantendo o formato do projeto de referência.
- Envelope incompleto, versão desconhecida ou tag inválida será descartado.

## 4. Core compartilhado e DataStore

- Adicionar `datastore-core` e `datastore-preferences-core` 1.2.1.
- Criar DataStore exclusivo: `cryptoview_secure.preferences_pb`.
- Persistir atomicamente somente:

  - `envelope_version`
  - `nonce_base64`
  - `ciphertext_and_tag_base64`

- Implementar `DefaultSecureApiKeyStorage` com `Mutex` para serializar leitura,
  gravação, substituição e remoção.
- `save` criptografa antes e grava o novo envelope em uma única edição.
- `read` valida o envelope e entrega o plaintext somente ao caso de uso que fará
  a requisição.
- Falha de descriptografia limpa envelope e chave inconsistentes e solicita uma
  nova configuração.
- `remove` apaga o envelope antes de remover a chave nativa.
- Excluir o DataStore seguro dos backups Android e iOS.

## 5. Android Keystore

- Adaptar os padrões de `AndroidPlatformCryptoKeyManager` e
  `AndroidPlatformAesEngine` do projeto de referência.
- Alias: `br.com.rmf.kmp.cryptoview.api-key.aes.v1`.
- Gerar AES-256 no provider `AndroidKeyStore` com:

  - `PURPOSE_ENCRYPT`
  - `PURPOSE_DECRYPT`
  - `BLOCK_MODE_GCM`
  - `ENCRYPTION_PADDING_NONE`

- Usar `Cipher.getInstance("AES/GCM/NoPadding")`.
- Na descriptografia, usar `GCMParameterSpec(128, nonce)`.
- Entregar a `SecretKey` diretamente ao `Cipher`.
- Não copiar o contrato `getKeyBytes()` da referência: chaves do Keystore não
  devem ser exportadas e `SecretKey.encoded` pode retornar `null`.
- Não exigir StrongBox, biometria ou autenticação do usuário nesta fase.

## 6. iOS com Swift, CryptoKit e Keychain

- Criar uma classe Swift dentro do `iosApp` implementando
  `PlatformApiKeyCipher`.
- Importar somente:

  - `Foundation`
  - `Security`
  - `CryptoKit`

- Configuração do Keychain:

  - Service: `br.com.rmf.kmp.cryptoview.secure-storage`
  - Account: `coinmarketcap.api-key.aes.v1`
  - Classe: `kSecClassGenericPassword`
  - Acessibilidade: `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
  - Proteção: `kSecUseDataProtectionKeychain`

- Usar `WhenUnlockedThisDeviceOnly` porque o aplicativo não precisa acessar a
  API key enquanto o aparelho estiver bloqueado.
- Gerar chave aleatória de 256 bits e convertê-la em `SymmetricKey`.
- Usar `SecItemAdd` quando a chave estiver ausente e `SecItemUpdate` quando já
  existir.
- Usar `SecItemCopyMatching` para leitura e `SecItemDelete` para remoção.
- Usar `AES.GCM.seal` para criptografar.
- Persistir nonce separado e concatenar `ciphertext + tag`.
- Usar `AES.GCM.SealedBox` e `AES.GCM.open` para descriptografar.
- Validar Base64, nonce e tamanho mínimo da tag.
- Não registrar chave, plaintext, nonce ou ciphertext.

Injeção no host iOS:

```swift
let cryptoProvider = IOSPlatformApiKeyCipher()

MainViewControllerKt.MainViewController(
    platformApiKeyCipher: cryptoProvider
)
```

O `MainViewController` repassa o provider ao módulo de plataforma do Koin.

## 7. Bootstrap e Koin

- Refatorar `initKoin` para receber o módulo de plataforma construído pelo host.
- Android fornece `Context`, DataStore, cliente CIO e cipher Keystore.
- iOS fornece DataStore, cliente Darwin e provider Swift.
- Registrar no grafo:

  - `PlatformApiKeyCipher`
  - `SecureApiKeyStorage`
  - DataStore seguro
  - ViewModels de aplicação, onboarding e ajustes

## 8. Fluxo do aplicativo

Substituir o booleano local de `App()` por estado controlado por ViewModel:

- `LoadingCredential`
- `NeedsApiKey`
- `Ready`
- `CredentialRecoveryRequired`

### Onboarding

1. O campo começa vazio.
2. O usuário informa a chave.
3. O ViewModel valida em `/v1/key/info`.
4. Somente após sucesso chama `SecureApiKeyStorage.save`.
5. O valor digitado é removido do estado de UI.
6. O aplicativo abre o mercado.

Falha de rede ou chave inválida não persiste nada. Falha de criptografia mantém
o usuário no onboarding e apresenta uma mensagem genérica de segurança.

### Inicialização

- Credencial descriptografável abre o mercado sem validação remota automática.
- Credencial ausente ou corrompida abre o onboarding.
- O plaintext não será mantido pelo estado raiz do aplicativo.

### Ajustes

- `Validar novamente` lê a chave apenas dentro do ViewModel/caso de uso.
- `Substituir` mantém a chave anterior até a nova ser validada e persistida.
- `Remover` limpa DataStore e chave nativa e retorna ao onboarding.

### Remoção do fluxo temporário

- Excluir `TempUtils` e o uso da API key fixa.
- Remover `CoinMarketCapTestScreen`, seu ViewModel e seu modelo de UI temporário.
- Remover o destino inferior `Testes`.
- A navegação final volta a conter somente `Mercado` e `Ajustes`.

## 9. Regras para o segredo em memória

- Não colocar a API key em `StateFlow`, estado persistente Compose, singleton,
  banco comum, analytics ou logs.
- O plaintext existirá somente:

  - no campo durante a digitação;
  - durante a validação;
  - no menor escopo necessário para montar a requisição autenticada.

- Limpar o campo e o estado após salvar.
- Redigir o header `X-CMC_PRO_API_KEY` nos logs HTTP.
- Não registrar plaintext, ciphertext, nonce ou conteúdo do Keychain/Keystore.

## 10. Testes compartilhados

- Save, read e remove com cipher e envelope store fakes.
- Substituição atômica.
- Concorrência serializada pelo `Mutex`.
- Envelope ausente, incompleto, adulterado ou com versão desconhecida.
- Falha ao criptografar, descriptografar, persistir ou remover.
- Chave inválida nunca armazenada.
- Validação remota obrigatoriamente anterior ao save.
- Estados de startup, onboarding, mercado e recuperação.
- Ausência do segredo em logs e estados persistentes de UI.

## 11. Testes Android

- Chave criada no `AndroidKeyStore`.
- Round trip após recriar as instâncias, simulando reinício.
- Duas criptografias do mesmo texto geram nonces e ciphertexts diferentes.
- Alteração de nonce, ciphertext ou tag impede leitura.
- Remoção exclui envelope e alias.
- DataStore seguro não participa do backup.
- Nenhum código depende de `SecretKey.encoded`.

## 12. Testes Swift/iOS

- Criar testes Xcode específicos para o provider Swift.
- Round trip CryptoKit.
- Nonces diferentes para o mesmo plaintext.
- Tag ou ciphertext adulterado retorna falha.
- Criar, consultar, atualizar e remover a chave no Keychain.
- Remoção idempotente.
- Usar service/account exclusivos nos testes e limpar no `tearDown`.
- Validar a passagem Swift → Kotlin → Koin.
- Reabrir o aplicativo e recuperar a credencial.
- Compilar framework e aplicativo sem CocoaPods.

## 13. Critérios de aceite

- Builds e testes Android verdes.
- Framework, aplicativo e testes iOS verdes em macOS/Xcode.
- API key validada antes de ser armazenada.
- Nenhum plaintext persistido.
- Android usa Keystore/AES-GCM.
- iOS usa Swift/CryptoKit + Keychain.
- Aplicativo permanece configurado após reinício.
- Envelope adulterado não é descriptografado.
- Remover e substituir funcionam nas duas plataformas.
- Nenhum componente CocoaPods é introduzido.
- Navegação final contém somente `Mercado` e `Ajustes`.

## 14. Limitações reconhecidas

- Nenhuma solução protege completamente um processo comprometido ou aparelho com
  root/jailbreak.
- O plaintext precisa existir brevemente em memória para autenticar a requisição.
- A camada AES no iOS não torna o Keychain desnecessário nem oferece proteção
  absoluta caso processo, Keychain e armazenamento local sejam comprometidos ao
  mesmo tempo.
- Biometria e autenticação por usuário permanecem fora do escopo desta fase.

