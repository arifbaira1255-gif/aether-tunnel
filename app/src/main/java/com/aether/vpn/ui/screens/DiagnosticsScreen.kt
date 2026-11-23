package com.aether.vpn.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.vpn.core.DiagnosticStep
import com.aether.vpn.ui.AppState
import com.aether.vpn.ui.components.GlassCard
import com.aether.vpn.ui.components.HairlineDivider
import com.aether.vpn.ui.components.Pill
import com.aether.vpn.ui.components.SectionLabel
import com.aether.vpn.ui.components.clickableNoRipple
import com.aether.vpn.ui.theme.Amber
import com.aether.vpn.ui.theme.Cyan
import com.aether.vpn.ui.theme.Mint
import com.aether.vpn.ui.theme.Rose
import com.aether.vpn.ui.theme.TextFaint
import com.aether.vpn.ui.theme.TextPrimary
import com.aether.vpn.ui.theme.TextSecondary
import com.aether.vpn.ui.theme.TextTertiary
import com.aether.vpn.ui.theme.Violet

/**
 * The troubleshooting screen.
 *
 * It is deliberately NOT part of the normal navigation: the user only reaches it from the
 * "Troubleshoot" button that appears after a failed connect. It answers one question -
 * where exactly did the connection stop - and gives them a way to send that answer on.
 */
@Composable
fun DiagnosticsScreen(app: AppState, onBack: () -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
    val report = app.diagnostics

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .clickableNoRipple(onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Troubleshoot",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    "Where the connection stopped",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
            Pill(
                text = if (report.ok) "Working" else "Failed",
                color = if (report.ok) Mint else Rose,
                filled = true
            )
        }

        Spacer(Modifier.height(18.dp))

        // ---- The plain-language answer -------------------------------------
        GlassCard(Modifier.fillMaxWidth(), accent = if (report.ok) Mint else Rose) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (report.ok) "The tunnel is working"
                    else "What went wrong",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (report.ok) Mint else Rose
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (report.ok) {
                        "Every step passed: the server answered, the key exchange " +
                            "completed, and a real request came back through the tunnel."
                    } else {
                        report.userMessage.ifBlank {
                            "The connection did not complete. The steps below show where it stopped."
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Step by step ---------------------------------------------------
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionLabel("Connection steps", accent = Cyan)
                Spacer(Modifier.height(12.dp))

                if (report.steps.isEmpty()) {
                    Text(
                        "No attempt has been recorded yet. Tap Connect to run one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint
                    )
                } else {
                    report.steps.forEachIndexed { index, step ->
                        StepRow(step)
                        if (index != report.steps.lastIndex) {
                            Spacer(Modifier.height(10.dp))
                            HairlineDivider()
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Technical summary ---------------------------------------------
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionLabel("Details", accent = Violet)
                Spacer(Modifier.height(12.dp))
                DetailRow("App version", report.appVersion.ifBlank { "?" })
                DetailRow("Tunnel engine", report.backendVersion.ifBlank { "?" })
                DetailRow("Combinations tried", report.combinationsTried.toString())
                if (report.workingMtu > 0) DetailRow("Working MTU", report.workingMtu.toString())
                if (report.workingPort > 0) DetailRow("Working port", report.workingPort.toString())
                if (report.startedAt > 0 && report.finishedAt > 0) {
                    DetailRow("Duration", "${report.finishedAt - report.startedAt} ms")
                }
                if (report.failureKind.isNotBlank()) {
                    DetailRow("Failure code", report.failureKind)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- Actions --------------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                label = "Copy diagnostics",
                icon = Icons.Rounded.ContentCopy,
                accent = Cyan,
                modifier = Modifier.weight(1f)
            ) {
                copyToClipboard(context, report.toText())
                Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
            }
            ActionButton(
                label = "Try again",
                icon = Icons.Rounded.Refresh,
                accent = Mint,
                modifier = Modifier.weight(1f)
            ) { onRetry() }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Send the copied text to support if the problem continues. It contains no " +
                "personal data - only the connection steps and their results.",
            style = MaterialTheme.typography.bodySmall,
            color = TextFaint
        )

        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun StepRow(step: DiagnosticStep) {
    val color = when (step.status) {
        DiagnosticStep.Status.PASS -> Mint
        DiagnosticStep.Status.FAIL -> Rose
        DiagnosticStep.Status.SKIP -> TextFaint
        DiagnosticStep.Status.RUNNING -> Amber
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    step.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    step.symbol,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    letterSpacing = 0.8.sp
                )
            }
            if (step.detail.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    step.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            if (step.durationMs > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "${step.durationMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextFaint
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.07f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .clickableNoRipple(onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    manager?.setPrimaryClip(ClipData.newPlainText("Aether diagnostics", text))
}
