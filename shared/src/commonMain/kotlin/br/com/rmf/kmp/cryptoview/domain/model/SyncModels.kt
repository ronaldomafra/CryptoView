package br.com.rmf.kmp.cryptoview.domain.model

enum class SyncTrigger {
    FIRST_RUN,
    STARTUP_ESSENTIAL,
    MANUAL_FULL,
    RESUME,
}

enum class SyncPhase {
    PREPARING,
    VALIDATING_CREDENTIAL,
    RESTORING_CHECKPOINT,
    EXCHANGES,
    EXCHANGE_METADATA,
    COINS,
    COIN_METADATA,
    FINALIZING,
    COMPLETED,
}

enum class SyncStatus {
    IDLE,
    RUNNING,
    PAUSED,
    PARTIAL,
    COMPLETED,
    FAILED,
}

data class SyncProgress(
    val runId: String? = null,
    val trigger: SyncTrigger? = null,
    val phase: SyncPhase = SyncPhase.PREPARING,
    val status: SyncStatus = SyncStatus.IDLE,
    val plannedItems: Long? = null,
    val persistedItems: Long = 0,
    val requestedPages: Int = 0,
    val committedPages: Int = 0,
    val failedPages: Int = 0,
    val error: CryptoError? = null,
    val message: String? = null,
) {
    val percentage: Float?
        get() = plannedItems?.takeIf { it > 0 }?.let {
            (persistedItems.toFloat() / it.toFloat()).coerceIn(0f, 1f)
        }
}

enum class SyncExecutionResult {
    ALREADY_RUNNING,
    NOT_STARTED,
    STARTED,
}

data class SyncResumeData(
    val runId: String,
    val trigger: SyncTrigger,
    val phase: SyncPhase,
    val persistedItems: Long,
    val requestedPages: Int,
    val committedPages: Int,
    val failedPages: Int,
)
