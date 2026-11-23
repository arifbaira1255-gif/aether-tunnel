package com.aether.vpn.core

/**
 * One step of the connection pipeline, as the diagnostics screen reports it.
 *
 * The point of this type is that a failed connect is never a mystery. Every stage the
 * app goes through - registration, tunnel up, handshake, traffic - records a pass or a
 * fail with a short reason, and the user can copy the whole thing and send it on.
 */
data class DiagnosticStep(
    val name: String,
    val status: Status,
    val detail: String = "",
    val durationMs: Long = 0
) {
    enum class Status { PASS, FAIL, SKIP, RUNNING }

    val symbol: String
        get() = when (status) {
            Status.PASS -> "PASS"
            Status.FAIL -> "FAIL"
            Status.SKIP -> "SKIP"
            Status.RUNNING -> "..."
        }
}

/**
 * The full record of one connect attempt.
 *
 * [toText] is what the "Copy diagnostics" button puts on the clipboard: plain text, no
 * JSON, readable in any chat app.
 */
data class DiagnosticReport(
    val steps: List<DiagnosticStep> = emptyList(),
    val failureKind: String = "",
    val userMessage: String = "",
    val appVersion: String = "",
    val backendVersion: String = "",
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L,
    /** How many (endpoint x port x MTU) combinations were tried. */
    val combinationsTried: Int = 0,
    /** The MTU that worked, or 0. */
    val workingMtu: Int = 0,
    /** The port that worked, or 0. */
    val workingPort: Int = 0
) {
    val ok: Boolean get() = steps.isNotEmpty() && steps.none { it.status == DiagnosticStep.Status.FAIL }

    fun toText(): String = buildString {
        appendLine("Aether Tunnel - connection diagnostics")
        appendLine("App version: $appVersion")
        appendLine("Tunnel engine: $backendVersion")
        appendLine("Result: ${if (ok) "CONNECTED" else "FAILED"}")
        if (failureKind.isNotBlank()) appendLine("Failure: $failureKind")
        if (userMessage.isNotBlank()) appendLine("Message: $userMessage")
        appendLine("Combinations tried: $combinationsTried")
        if (workingMtu > 0) appendLine("Working MTU: $workingMtu")
        if (workingPort > 0) appendLine("Working port: $workingPort")
        if (startedAt > 0 && finishedAt > 0) {
            appendLine("Duration: ${finishedAt - startedAt} ms")
        }
        appendLine()
        appendLine("Steps:")
        steps.forEach { s ->
            val ms = if (s.durationMs > 0) " (${s.durationMs} ms)" else ""
            appendLine("  [${s.symbol}] ${s.name}$ms")
            if (s.detail.isNotBlank()) appendLine("         ${s.detail}")
        }
    }
}
