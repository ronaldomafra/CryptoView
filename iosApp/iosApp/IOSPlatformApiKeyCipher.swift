import CryptoKit
import Foundation
import Security
import Shared

final class IOSPlatformApiKeyCipher: PlatformApiKeyCipher {
    func encrypt(plainText: String) -> EncryptedApiKeyEnvelope? {
        do {
            let key = try getOrCreateKey()
            let sealedBox = try AES.GCM.seal(Data(plainText.utf8), using: key)
            let cipherTextAndTag = sealedBox.ciphertext + sealedBox.tag

            return EncryptedApiKeyEnvelope(
                version: 1,
                nonceBase64: Data(sealedBox.nonce).base64EncodedString(),
                cipherTextAndTagBase64: cipherTextAndTag.base64EncodedString()
            )
        } catch {
            return nil
        }
    }

    func decrypt(envelope: EncryptedApiKeyEnvelope) -> String? {
        do {
            guard envelope.version == 1,
                  let nonceData = Data(base64Encoded: envelope.nonceBase64),
                  let cipherTextAndTag = Data(base64Encoded: envelope.cipherTextAndTagBase64),
                  nonceData.count == Self.nonceSize,
                  cipherTextAndTag.count >= Self.tagSize else {
                return nil
            }

            let key = try readKey()
            let tagStart = cipherTextAndTag.count - Self.tagSize
            let sealedBox = try AES.GCM.SealedBox(
                nonce: AES.GCM.Nonce(data: nonceData),
                ciphertext: Data(cipherTextAndTag.prefix(tagStart)),
                tag: Data(cipherTextAndTag.suffix(Self.tagSize))
            )
            let plainText = try AES.GCM.open(sealedBox, using: key)
            return String(data: plainText, encoding: .utf8)
        } catch {
            return nil
        }
    }

    func deleteKey() -> Bool {
        let status = SecItemDelete(baseQuery() as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    private func getOrCreateKey() throws -> SymmetricKey {
        do {
            return try readKey()
        } catch KeychainError.notFound {
            let key = SymmetricKey(size: .bits256)
            let keyData = key.withUnsafeBytes { Data($0) }
            var query = baseQuery()
            query[kSecValueData as String] = keyData
            query[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly

            let status = SecItemAdd(query as CFDictionary, nil)
            if status == errSecDuplicateItem {
                return try readKey()
            }
            guard status == errSecSuccess else {
                throw KeychainError.status(status)
            }
            return key
        }
    }

    private func readKey() throws -> SymmetricKey {
        var query = baseQuery()
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound {
            throw KeychainError.notFound
        }
        guard status == errSecSuccess, let keyData = result as? Data else {
            throw KeychainError.status(status)
        }
        return SymmetricKey(data: keyData)
    }

    private func baseQuery() -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.service,
            kSecAttrAccount as String: Self.account,
            kSecUseDataProtectionKeychain as String: true,
        ]
    }

    private enum KeychainError: Error {
        case notFound
        case status(OSStatus)
    }

    private static let service = "br.com.rmf.kmp.cryptoview.secure-storage"
    private static let account = "coinmarketcap.api-key.aes.v1"
    private static let nonceSize = 12
    private static let tagSize = 16
}
