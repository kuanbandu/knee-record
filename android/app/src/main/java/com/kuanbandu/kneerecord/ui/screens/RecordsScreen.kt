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
import com.kuanbandu.kneerecord.ui.components.FeelingChart

private const val CHART_POINTS = 12

@Composable
fun RecordsScreen(onAdd: () -> Unit) {
    // 数据版本号变化后重新读取，写入由 LocalStore 负责自增
    val version = LocalStore.recordsVersion.value
    val records = remember(version) { LocalStore.getRecords() }
    val totalMin = remember(records) { records.sumOf { it.durationMin } }
    val tip = remember(version) { LocalStore.reminderTip() }
    var pendingDelete by remember { mutableStateOf<Record?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("我的记录", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("我的锻炼记录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "共 ${records.size} 次 · 累计 $totalMin 分钟",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                    )
                }
                Button(onClick = onAdd) { Text("＋ 记一次") }
            }
        }

        if (tip.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            TipCard(tip)
        }

        Spacer(Modifier.height(12.dp))

        if (records.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有锻炼记录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "点上面「＋ 记一次」，把今天练的内容和身体感受记下来。",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                    )
                }
            }
        } else {
            val shown = records.take(CHART_POINTS).reversed()
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("身体感受变化趋势", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "最近 ${shown.size} 次记录的身体感受评分",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                    )
                    FeelingChart(
                        points = shown.map { it.feelingScore },
                        labels = shown.map {
                            if (it.date.length >= 10) it.date.substring(5) else it.date
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .padding(top = 8.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp, 3.dp)
                                .background(Color(0xFF0A84FF)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("身体感受评分（0-10）", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("历史记录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            records.take(50).forEach { rec ->
                RecordItem(rec = rec, onDelete = { pendingDelete = rec })
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    pendingDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    LocalStore.removeRecord(rec.id)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RecordItem(rec: Record, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    LocalStore.formatDateTime(rec.timestamp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text("${rec.durationMin} 分钟", fontSize = 13.sp, color = Color(0xFF6B7280))
                TextButton(onClick = onDelete) { Text("删除", color = Color(0xFFFF3B30)) }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Column {
                    Text("${rec.feelingScore}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("身体感受", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
                Column {
                    Text("${rec.exercises.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("动作数", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }

            if (rec.exercises.isNotEmpty()) {
                Text(
                    rec.exercises.joinToString(" · ") {
                        if (it.sets.isBlank()) it.name else "${it.name} ${it.sets} 组"
                    },
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (rec.note.isNotBlank()) {
                Text(
                    "备注：${rec.note}",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TipCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF4FF)),
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = Color(0xFF24508A),
            modifier = Modifier.padding(14.dp),
        )
    }
}
