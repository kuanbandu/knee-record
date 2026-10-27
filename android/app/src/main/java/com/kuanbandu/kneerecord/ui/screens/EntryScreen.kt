package com.kuanbandu.kneerecord.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.model.ExerciseEntry
import com.kuanbandu.kneerecord.model.Record
import kotlin.math.roundToInt

private data class ManualRow(val name: String = "", val sets: String = "")

/** 手动补记：不跟练也能记一次，或者直接把以前练过的补进来 */
@Composable
fun EntryScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf(listOf(ManualRow())) }
    var painBefore by remember { mutableStateOf(0f) }
    var painPeak by remember { mutableStateOf(0f) }
    var rpe by remember { mutableStateOf(3f) }
    var duration by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("补记一次训练", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // 动作
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("练了什么", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "自己填写，想写几个就加几行",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                )
                rows.forEachIndexed { i, row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = row.name,
                            onValueChange = { v ->
                                rows = rows.toMutableList().also { it[i] = row.copy(name = v) }
                            },
                            label = { Text("动作名称") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = row.sets,
                            onValueChange = { v ->
                                rows = rows.toMutableList().also {
                                    it[i] = row.copy(sets = v.filter { c -> c.isDigit() })
                                }
                            },
                            label = { Text("组数") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                            modifier = Modifier
                                .width(92.dp)
                                .padding(top = 8.dp),
                        )
                        if (rows.size > 1) {
                            TextButton(onClick = {
                                rows = rows.toMutableList().also { it.removeAt(i) }
                            }) { Text("×", fontSize = 20.sp, color = Color(0xFFFF3B30)) }
                        }
                    }
                }
                TextButton(onClick = { rows = rows + ManualRow() }) {
                    Text("＋ 再加一个动作")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 疼痛评分
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("疼痛评分", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

                Text("训练前：${painBefore.roundToInt()} / 10", fontSize = 15.sp)
                Slider(
                    value = painBefore,
                    onValueChange = {
                        painBefore = it
                        if (painPeak < it) painPeak = it
                    },
                    valueRange = 0f..10f,
                    steps = 9,
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "训练中峰值：${painPeak.roundToInt()} / 10",
                    fontSize = 15.sp,
                    color = if (painPeak.roundToInt() >= 4) Color(0xFFFF3B30) else Color.Unspecified,
                )
                Slider(
                    value = painPeak,
                    onValueChange = { painPeak = it },
                    valueRange = 0f..10f,
                    steps = 9,
                )
                Row {
                    Text("0 不痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text("10 剧痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // RPE 与时长
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("自觉疲劳（RPE）", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("${rpe.roundToInt()} / 10", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = rpe,
                    onValueChange = { rpe = it },
                    valueRange = 0f..10f,
                    steps = 9,
                )
                Row {
                    Text("0 完全不累", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text("10 力竭", fontSize = 12.sp, color = Color(0xFF6B7280))
                }

                Spacer(Modifier.height(12.dp))
                Text("训练时长", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = { Text("本次训练分钟数") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val minutes = if (duration.isBlank()) 0 else duration.toIntOrNull() ?: -1
                if (minutes < 0 || minutes > 600) {
                    Toast.makeText(context, "训练时长请填 0-600 之间的分钟数", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val filled = rows.filter { it.name.trim().isNotEmpty() }
                if (filled.isEmpty()) {
                    Toast.makeText(context, "请至少填写 1 个动作名称", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                LocalStore.addRecord(
                    Record(
                        id = "rec_${System.currentTimeMillis()}",
                        date = LocalStore.todayKey(),
                        timestamp = System.currentTimeMillis(),
                        painBefore = painBefore.roundToInt(),
                        painPeak = painPeak.roundToInt(),
                        rpe = rpe.roundToInt(),
                        durationMin = minutes,
                        note = note.trim(),
                        exercises = filled.map { ExerciseEntry(it.name.trim(), it.sets.trim()) },
                    ),
                )
                Toast.makeText(context, "已记录", Toast.LENGTH_SHORT).show()
                onSaved()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("保存记录", fontSize = 17.sp)
        }

        Spacer(Modifier.height(24.dp))
    }
}
