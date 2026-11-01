package com.example.nexus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.model.AIProviderType
import com.example.nexus.model.NexusTask
import com.example.nexus.model.OpStatus
import com.example.nexus.model.Operation
import com.example.nexus.model.RiskLevel
import com.example.nexus.model.StepStatus
import com.example.nexus.ui.theme.NexusAmber
import com.example.nexus.ui.theme.NexusAmberDark
import com.example.nexus.ui.theme.NexusBg
import com.example.nexus.ui.theme.NexusBlue
import com.example.nexus.ui.theme.NexusBorder
import com.example.nexus.ui.theme.NexusCyan
import com.example.nexus.ui.theme.NexusGreen
import com.example.nexus.ui.theme.NexusGreenDark
import com.example.nexus.ui.theme.NexusMuted
import com.example.nexus.ui.theme.NexusPanel
import com.example.nexus.ui.theme.NexusPanel2
import com.example.nexus.ui.theme.NexusRed
import com.example.nexus.ui.theme.NexusRedDark
import com.example.nexus.ui.theme.NexusText

@Composable
fun NexusHeader(
    providerType: AIProviderType,
    onToggleProvider: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF101722),
        border = BorderStroke(1.dp, NexusBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "PROJECT NEXUS",
                    color = NexusBlue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "LOCAL-FIRST ENVIRONMENT",
                    color = NexusMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            // Mode Pill Badge
            val isOnline = providerType == AIProviderType.GEMINI
            val badgeColor = if (isOnline) NexusBlue else NexusGreen
            val badgeBg = if (isOnline) Color(0xFF122842) else Color(0xFF10281E)
            val badgeBorder = if (isOnline) Color(0xFF1E4670) else Color(0xFF1D4A35)
            val badgeText = if (isOnline) "● ONLINE · GEMINI" else "● OFFLINE · DETERMINISTIC"

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .border(1.dp, badgeBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = onToggleProvider)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("mode_toggle_badge")
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun RiskBadge(risk: RiskLevel) {
    val color: Color
    val bg: Color
    val border: Color
    when (risk) {
        RiskLevel.LOW -> {
            color = NexusGreen
            bg = NexusGreenDark.copy(alpha = 0.4f)
            border = Color(0xFF285A47)
        }
        RiskLevel.MEDIUM -> {
            color = NexusAmber
            bg = NexusAmberDark.copy(alpha = 0.4f)
            border = Color(0xFF6B5A2A)
        }
        RiskLevel.HIGH -> {
            color = NexusRed
            bg = NexusRedDark.copy(alpha = 0.4f)
            border = Color(0xFF6B2A2A)
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "${risk.name} RISK",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun OperationCard(
    op: Operation,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NexusPanel),
        border = BorderStroke(1.dp, NexusBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("operation_card_${op.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROPOSED: ${op.label}",
                    color = NexusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                RiskBadge(risk = op.risk)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parameter JSON block
            Surface(
                color = Color(0xFF0E1621),
                border = BorderStroke(1.dp, NexusBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Parameters: " + op.params.entries.joinToString(", ") { "${it.key}: \"${it.value.take(40)}\"" },
                        color = NexusCyan,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Policy: " + op.policyReason,
                color = NexusMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            when (op.status) {
                OpStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287AC9)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("approve_button_${op.id}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDeny,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusRed),
                            border = BorderStroke(1.dp, Color(0xFF6B2A2A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("deny_button_${op.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deny", fontSize = 12.sp)
                        }
                    }
                }
                OpStatus.APPROVED, OpStatus.COMPLETED -> {
                    Text(
                        text = "Status: Approved & Verified in Sandbox",
                        color = NexusGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (op.output.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = op.output,
                            color = NexusMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                OpStatus.DENIED -> {
                    Text(
                        text = "Status: Denied by User. Logged to Security Audit.",
                        color = NexusRed,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OpStatus.EXECUTING -> {
                    Text(
                        text = "Executing operation safely in sandbox...",
                        color = NexusAmber,
                        fontSize = 11.5.sp
                    )
                }
                OpStatus.FAILED -> {
                    Text(
                        text = "Operation execution failed: ${op.output}",
                        color = NexusRed,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: NexusTask,
    onRunNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NexusPanel),
        border = BorderStroke(1.dp, NexusBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUTONOMOUS TASK",
                        color = NexusBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = task.title,
                        color = NexusText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF13283E))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.status.name,
                        color = NexusBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress
            val doneCount = task.steps.count { it.status == StepStatus.DONE }
            val progress = if (task.steps.isNotEmpty()) doneCount.toFloat() / task.steps.size.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                color = NexusCyan,
                trackColor = Color(0xFF101A27),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Steps
            task.steps.forEachIndexed { idx, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon: String
                    val iconColor: Color
                    when (step.status) {
                        StepStatus.DONE -> { icon = "✓"; iconColor = NexusGreen }
                        StepStatus.RUNNING -> { icon = "●"; iconColor = NexusAmber }
                        StepStatus.FAILED -> { icon = "✗"; iconColor = NexusRed }
                        else -> { icon = "${idx + 1}"; iconColor = NexusMuted }
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0E1621))
                            .border(1.dp, NexusBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            color = if (step.status == StepStatus.DONE) NexusMuted else NexusText,
                            fontSize = 12.sp,
                            fontWeight = if (step.status == StepStatus.RUNNING) FontWeight.Bold else FontWeight.Normal
                        )
                        if (step.verificationNotes.isNotBlank()) {
                            Text(
                                text = step.verificationNotes,
                                color = NexusCyan,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (task.status != com.example.nexus.model.TaskStatus.COMPLETED &&
                task.status != com.example.nexus.model.TaskStatus.CANCELLED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRunNext,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287AC9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Execute Next Step", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusRed),
                        border = BorderStroke(1.dp, Color(0xFF6B2A2A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
