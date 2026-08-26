package br.com.rmf.kmp.cryptoview.utils

data class CryptoProcessConfig(
    val parallelIoValue: Int,
    val parallelDbValue: Int,
) {
    init {
        require(parallelIoValue > 0) { "parallelIoValue deve ser maior que zero" }
        require(parallelDbValue > 0) { "parallelDbValue deve ser maior que zero" }
    }
}

internal fun defaultCryptoProcessConfig(
    parallelIoValue: Int,
    parallelDbValue: Int,
): CryptoProcessConfig = CryptoProcessConfig(
    parallelIoValue = parallelIoValue,
    parallelDbValue = parallelDbValue,
)
