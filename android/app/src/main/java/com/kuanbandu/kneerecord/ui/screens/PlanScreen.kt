package com.kuanbandu.kneerecord.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.engine.ConsentRequiredError
import com.kuanbandu.kneerecord.engine.ExerciseBlockedError
import com.kuanbandu.kneerecord.engine.KneeRehabRules
import com.kuanbandu.kneerecord.engine.SessionHolder
import com.kuanbandu.kneerecord.model.Exercise
import com.kuanbandu.kneerecord.model.Stage

private const val CONSENT_TEXT = "我已知晓训练中如有不适立即停止"

@Composable
fun PlanScreen(onStart: () -> Unit, onGoAssess: () -> Unit) {
    val context = LocalContext.current
    val version = LocalStore.assessmentVersion.value
    val recordsVersion = LocalStore.recordsVersion.value

    val assessment = remember(version) { LocalStore.getAssessment() }
    val settings = remember(version) { LocalStore.getSettings() }
    val todayDone = remember(recordsVersion) { LocalStore.hasToday() }

    var consentAt by remember(version) { mutableStateOf(settings.consentAt) }
    val consentValid = remember(consentAt) { KneeRehabRules.isConsentValid(consentAt) }
    var detail by remember { mutableStateOf<Exercise?>(null) }

    if (assessment == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text("康复训练", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("先填写我的情况", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "伤情阶段、疼痛评分和 ROM 活动度决定本次能练什么。",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onGoAssess, modifier = Modifier.fillMaxWidth()) {
                        Text("去填写")
                    }
                }
            }
        }
        return
    }

    val plan = remember(version) { KneeRehabRules.previewSession(assessment) }
    val stage = remember(version) { Stage.of(assessment.stage) ?: Stage.EARLY }
    val canStart = consentValid && plan.exercises.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("康复训练", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // 本次推荐条件
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("本次推荐条件", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    Text("阶段", fontSize = 14.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text(stage.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text("允许最高等级", fontSize = 14.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text("L${plan.gate.maxLevel}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text("禁止类别", fontSize = 14.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text(
                        plan.gate.forbiddenLabels.takeIf { it.isNotEmpty() }?.joinToString("/") ?: "无",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (plan.gate.forbiddenLabels.isEmpty()) Color.Unspecified else Color(0xFFFF3B30),
                    )
                }
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text("被拦下的动作", fontSize = 14.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.weight(1f))
                    Text(
                        "共 ${plan.gate.blockedCount} 个" +
                            if (plan.gate.romBlockedCount > 0) "（活动度不足 ${plan.gate.romBlockedCount} 个）" else "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (todayDone) {
                    Text(
                        "今天已经练过了，再练一次请留意膝盖反应。",
                        fontSize = 13.sp,
                        color = Color(0xFFFF9F0A),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (plan.exercises.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前条件下没有可做的动作", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "疼痛评分较高时清单可能为空。重新评估或疼痛缓解后再来。",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        plan.slots.forEach { (slot, list) ->
            Text(
                slot.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280),
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach { e ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { detail = e },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(e.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "L${e.level}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                )
                            }
                            Text(
                                "${e.category.label} · ${e.dosage.describe()}",
                                fontSize = 13.sp,
                                color = Color(0xFF0A84FF),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                "口令：${e.kneeCue}",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = consentValid,
                onCheckedChange = { checked ->
                    val value = if (checked) System.currentTimeMillis() else 0L
                    consentAt = value
                    LocalStore.updateSettings(consentAt = value)
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(CONSENT_TEXT, fontSize = 14.sp)
                Text(
                    "勾选记录 12 小时后自动失效",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                try {
                    SessionHolder.session = KneeRehabRules.startSession(assessment, consentAt)
                    onStart()
                } catch (err: ConsentRequiredError) {
                    Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                } catch (err: Exception) {
                    Toast.makeText(context, err.message ?: "无法开始训练", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                if (!consentValid) {
                    "请先勾选训练前须知"
                } else {
                    "开始今日训练（${plan.exercises.size} 个动作）"
                },
                fontSize = 17.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    detail?.let { e ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(e.name) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "${e.category.label} · L${e.level} · ${e.dosage.describe()}",
                        fontSize = 13.sp,
                        color = Color(0xFF0A84FF),
                    )
                    Text(
                        "作用：${e.target}",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        "膝盖口令：${e.kneeCue}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text("安全要点", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    e.safety.forEach {
                        Text("· $it", fontSize = 13.sp, color = Color(0xFF6B7280))
                    }
                    Text("常见错误", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    e.mistakes.forEach {
                        Text("· $it", fontSize = 13.sp, color = Color(0xFF6B7280))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        SessionHolder.session = KneeRehabRules.startSingleExerciseSession(
                            assessment,
                            e.id,
                            consentAt,
                        )
                        detail = null
                        onStart()
                    } catch (err: ExerciseBlockedError) {
                        detail = null
                        Toast.makeText(context, err.message, Toast.LENGTH_LONG).show()
                    } catch (err: ConsentRequiredError) {
                        detail = null
                        Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                    }
                }) { Text("单独跟练") }
            },
            dismissButton = {
                TextButton(onClick = { detail = null }) { Text("关闭") }
            },
        )
    }
}
