package com.kuanbandu.kneerecord.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.model.Record
import com.kuanbandu.kneerecord.ui.components.PainChart
import java.util.Calendar

private const val CHART_POINTS = 12

@Composable
fun RecordsScreen(onAdd: () -> Unit) {
    val version = LocalStore.recordsVersion.value
    val records = remember(version) { LocalStore.getRecords() }
    var pendingDelete by remember { mutableStateOf<Record?>(null) }
    var reminderTip by remember { mutableStateOf("") }

    LaunchedEffect(version) {
        val settings = LocalStore.getSettings()
        reminderTip = if (settings.reminderEnabled) {
            val parts = settings.reminderTime.split(":")
            val target = (parts.getOrNull(0)?.toIntOrNull() ?: 19) * 60 +
                (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            val cal = Calendar.getInstance()
            val current = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            if (current >= target && records.none { it.date == LocalStore.todayKey() }) {
                "提醒时间是 ${settings.reminderTime}，今天还没有训练记录。"
            } else {
                ""
            }
        } else {
            ""
        }
    }

    val shown = records.take(CHART_POINTS).reversed()
    val totalMin = records.sumOf { it.durationMin }
    val avgBefore = if (records.isEmpty()) 0.0 else records.map { it.painBefore }.average()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("训练记录", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        if (reminderTip.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(reminderTip, fontSize = 13.sp, color = Color(0xFFFF9F0A))
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("训练次数", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Text("${records.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("累计时长", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Text("$totalMin 分钟", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("平均训练前疼痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Text(
                        String.format("%.1f", avgBefore),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (shown.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("疼痛变化趋势", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "最近 ${shown.size} 次训练",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                    )
                    PainChart(
                        labels = shown.map {
                            if (it.date.length >= 10) it.date.substring(5) else it.date
                        },
                        before = shown.map { it.painBefore },
                        peak = shown.map { it.painPeak },
                    )
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(22.dp, 3.dp)
                                .background(Color(0xFF0A84FF)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("训练前疼痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp, 3.dp)
                                .background(Color(0xFFFF9F0A)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("训练中峰值", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp, 3.dp)
                                .background(Color(0xFFFF3B30)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("4 分警戒", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (records.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("还没有训练记录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "在训练页完成一次跟练会自动记录，也可以手动补记。",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        records.forEach { rec ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                LocalStore.formatDateTime(rec.timestamp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${rec.durationMin} 分钟",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                            )
                        }
                        TextButton(onClick = { pendingDelete = rec }) {
                            Text("删除", color = Color(0xFFFF3B30))
                        }
                    }

                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Metric("训练前", rec.painBefore.toString(), Color(0xFF0A84FF))
                        Spacer(Modifier.width(12.dp))
                        Metric(
                            "峰值",
                            rec.painPeak.toString(),
                            if (rec.painPeak >= 4) Color(0xFFFF3B30) else Color(0xFFFF9F0A),
                        )
                        Spacer(Modifier.width(12.dp))
                        Metric("RPE", rec.rpe.toString(), Color(0xFF6B7280))
                    }

                    if (rec.exercises.isNotEmpty()) {
                        Text(
                            rec.exercises.joinToString("、") {
                                if (it.sets.isBlank()) it.name else "${it.name} ${it.sets} 组"
                            },
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (rec.note.isNotBlank()) {
                        Text(
                            rec.note,
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("＋ 补记一次")
        }

        Spacer(Modifier.height(24.dp))
    }

    pendingDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录") },
            text = { Text("${LocalStore.formatDateTime(rec.timestamp)} 的训练记录将被删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    LocalStore.deleteRecord(rec.id)
                    pendingDelete = null
                }) { Text("删除", color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF6B7280))
    }
}
