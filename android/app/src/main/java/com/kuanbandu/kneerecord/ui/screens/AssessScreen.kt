package com.kuanbandu.kneerecord.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.engine.PAIN_GATE
import com.kuanbandu.kneerecord.engine.PAIN_SEEK_HELP
import com.kuanbandu.kneerecord.model.Assessment
import com.kuanbandu.kneerecord.model.STAGE_OPTIONS
import kotlin.math.roundToInt

@Composable
fun AssessScreen(onSaved: () -> Unit = {}) {
    val context = LocalContext.current
    val saved = remember { LocalStore.getAssessment() }

    var age by remember { mutableStateOf(saved?.age?.takeIf { it > 0 }?.toString() ?: "") }
    var stage by remember { mutableStateOf(saved?.stage ?: "") }
    var pain by remember { mutableStateOf((saved?.painScore ?: 0).toFloat()) }
    var rom by remember { mutableStateOf((saved?.romFlexion ?: 90).toFloat()) }
    var limited by remember { mutableStateOf(saved?.extensionLimited ?: false) }
    var savedAt by remember { mutableStateOf(saved?.updatedAt ?: 0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("我的情况", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (savedAt > 0L) {
            Spacer(Modifier.height(8.dp))
            Text(
                "上次更新：${LocalStore.formatDateTime(savedAt)}",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
            )
        }

        Spacer(Modifier.height(12.dp))

        // 年龄
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("年龄", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { c -> c.isDigit() } },
                    label = { Text("请输入年龄") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 伤情阶段
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("伤情阶段", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "决定本次允许的最高动作等级",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                )
                STAGE_OPTIONS.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stage = opt.key }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = stage == opt.key, onClick = { stage = opt.key })
                        Column {
                            Text(opt.label, fontSize = 16.sp)
                            Text(opt.desc, fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                    }
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
                Text("此刻膝盖的疼痛程度", fontSize = 13.sp, color = Color(0xFF6B7280))
                Text(
                    "${pain.roundToInt()} / 10",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Slider(
                    value = pain,
                    onValueChange = { pain = it },
                    valueRange = 0f..10f,
                    steps = 9,
                )
                Row {
                    Text("0 不痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text("10 剧痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
                if (pain.roundToInt() >= PAIN_GATE) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "评分 ≥ $PAIN_GATE：深蹲、弓步、跳跃类动作将被禁止。",
                        fontSize = 13.sp,
                        color = Color(0xFFFF9F0A),
                    )
                }
                if (pain.roundToInt() >= PAIN_SEEK_HELP) {
                    Text(
                        "评分 ≥ $PAIN_SEEK_HELP：本次训练限制到最低强度，并建议先寻求专业帮助。",
                        fontSize = 13.sp,
                        color = Color(0xFFFF3B30),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ROM 活动度
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ROM 活动度", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "仰卧，脚跟往臀部方向滑，估计能弯到多少度",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                )
                Text(
                    "${rom.roundToInt()}°",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Slider(
                    value = rom,
                    onValueChange = { rom = it },
                    valueRange = 30f..135f,
                    steps = 20,
                )
                Row {
                    Text("30° 明显受限", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text("135° 接近全范围", fontSize = 12.sp, color = Color(0xFF6B7280))
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("膝盖无法完全伸直", fontSize = 16.sp)
                        Text(
                            "伸直受限时，需要主动伸膝的动作要谨慎",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                        )
                    }
                    Switch(checked = limited, onCheckedChange = { limited = it })
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val ageValue = age.toIntOrNull()
                if (ageValue == null || ageValue < 1 || ageValue > 120) {
                    Toast.makeText(context, "请填写 1-120 之间的年龄", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (stage.isBlank()) {
                    Toast.makeText(context, "请选择伤情阶段", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                LocalStore.saveAssessment(
                    Assessment(
                        age = ageValue,
                        stage = stage,
                        painScore = pain.roundToInt(),
                        romFlexion = rom.roundToInt(),
                        extensionLimited = limited,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                savedAt = System.currentTimeMillis()
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                onSaved()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("保存", fontSize = 17.sp)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "这里的评分和活动度会决定接下来能练什么。每天开始前重新确认一遍更准。",
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}
