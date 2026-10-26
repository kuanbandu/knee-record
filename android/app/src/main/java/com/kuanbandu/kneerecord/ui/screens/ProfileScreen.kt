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
import com.kuanbandu.kneerecord.model.Assessment
import com.kuanbandu.kneerecord.model.STAGE_OPTIONS
import kotlin.math.roundToInt

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val saved = remember { LocalStore.getAssessment() }

    var age by remember { mutableStateOf(saved?.age?.takeIf { it > 0 }?.toString() ?: "") }
    var stage by remember { mutableStateOf(saved?.stage ?: "") }
    var bend by remember { mutableStateOf((saved?.bendAngle ?: 90).toFloat()) }
    var straighten by remember { mutableStateOf(saved?.straightenOk ?: false) }
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
            Text("上次更新：${LocalStore.formatDateTime(savedAt)}", fontSize = 13.sp, color = Color(0xFF6B7280))
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("当前阶段", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                STAGE_OPTIONS.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stage = opt.value }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = stage == opt.value, onClick = { stage = opt.value })
                        Column {
                            Text(opt.label, fontSize = 16.sp)
                            Text(opt.desc, fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("膝盖弯曲角度（自评）", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("脚跟往臀部方向滑，估计能弯到多少度", fontSize = 13.sp, color = Color(0xFF6B7280))
                Text("${bend.roundToInt()}°", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = bend,
                    onValueChange = { bend = it },
                    valueRange = 30f..135f,
                    steps = 20,
                )
                Row {
                    Text("30°（弯得少）", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text("135°（弯得多）", fontSize = 12.sp, color = Color(0xFF6B7280))
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("膝盖能完全伸直吗", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Switch(checked = straighten, onCheckedChange = { straighten = it })
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
                    Toast.makeText(context, "请选择当前阶段", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                LocalStore.saveAssessment(
                    Assessment(
                        age = ageValue,
                        stage = stage,
                        bendAngle = bend.roundToInt(),
                        straightenOk = straighten,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                savedAt = System.currentTimeMillis()
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("保存", fontSize = 17.sp)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "以上仅作为个人资料保存，不会参与任何推荐。",
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}
