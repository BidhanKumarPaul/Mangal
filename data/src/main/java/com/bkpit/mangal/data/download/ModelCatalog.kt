package com.bkpit.mangal.data.download

/**
 * Hand-maintained list shown in the Model Manager screen. URLs/checksums are
 * placeholders — fill in real Hugging Face (or wherever you host mirrors)
 * URLs and the SHA-256 of the exact file before shipping. I'm not fetching
 * or verifying these from my sandbox (no network access there), and I'd
 * rather leave an obvious placeholder than invent a checksum that silently
 * fails verification for you.
 */
data class ModelCatalogEntry(
    val id: String,
    val displayName: String,
    val kind: ModelKind,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minRamMb: Int
)

enum class ModelKind { LLM, WHISPER }

object ModelCatalog {
    val entries = listOf(
        ModelCatalogEntry(
            id = "gemma-3-2b-q4km",
            displayName = "Gemma 3 2B Instruct (Q4_K_M)",
            kind = ModelKind.LLM,
            downloadUrl = "https://REPLACE_ME/gemma-3-2b-it-Q4_K_M.gguf",
            sha256 = "REPLACE_WITH_REAL_SHA256",
            sizeBytes = 1_600_000_000L,
            minRamMb = 3000
        ),
        ModelCatalogEntry(
            id = "qwen-3-5-1-7b-q4km",
            displayName = "Qwen 3.5 1.7B Instruct (Q4_K_M)",
            kind = ModelKind.LLM,
            downloadUrl = "https://REPLACE_ME/qwen3.5-1.7b-instruct-Q4_K_M.gguf",
            sha256 = "REPLACE_WITH_REAL_SHA256",
            sizeBytes = 1_100_000_000L,
            minRamMb = 2500
        ),
        ModelCatalogEntry(
            id = "whisper-base-int8",
            displayName = "Whisper Base (INT8)",
            kind = ModelKind.WHISPER,
            downloadUrl = "https://REPLACE_ME/ggml-base-q8_0.bin",
            sha256 = "REPLACE_WITH_REAL_SHA256",
            sizeBytes = 90_000_000L,
            minRamMb = 500
        ),
    )
}
