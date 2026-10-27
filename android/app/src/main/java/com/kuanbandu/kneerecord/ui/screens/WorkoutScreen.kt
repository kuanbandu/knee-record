package com.kuanbandu.kneerecord.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kuanbandu.kneerecord.audio.Cue
import com.kuanbandu.kneerecord.audio.Metronome
import com.kuanbandu.kneerecord.audio.Speech
import com.kuanbandu.kneerecord.model.MetronomeSound
import com.kuanbandu.kneerecord.ui.components.ExerciseAnimation
import com.kuanbandu.kneerecord.ui.components.framesFor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.engine.KneeRehabRules
import com.kuanbandu.kneerecord.engine.PainAdvice
import com.kuanbandu.kneerecord.engine.SessionHolder
import com.kuanbandu.kneerecord.model.DoseMode
import com.kuanbandu.kneerecord.model.ExerciseEntry
import com.kuanbandu.kneerecord.model.Record
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class Phase { RUNNING, REST, FEEDBACK, DONE }

@Composable
fun WorkoutScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val session = SessionHolder.session
    if (session == null) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    var index by remember { mutableIntStateOf(0) }
    var setIndex by remember { mutableIntStateOf(0) }
    var repIndex by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.RUNNING) }
    var remaining by remember { mutableIntStateOf(0) }

    var peakPain by remember { mutableIntStateOf(session.painBefore) }
    var lastRpe by remember { mutableIntStateOf(0) }
    var doneList by remember { mutableStateOf(listOf<ExerciseEntry>()) }

    var feedbackPain by remember { mutableFloatStateOf(session.painBefore.toFloat()) }
    var feedbackRpe by remember { mutableFloatStateOf(3f) }
    var reportPain by remember { mutableFloatStateOf(0f) }
    var showReport by remember { mutableStateOf(false) }
    var advice by remember { mutableStateOf<PainAdvice?>(null) }

    val total = session.exercises.size
    val exercise = session.exercises[index]
    val sets = exercise.dosage.sets.coerceAtLeast(1)
    val reps = exercise.dosage.reps.coerceAtLeast(1)

    // 节拍与语音：跟着设置走，页面退出时释放
    val settings = remember { LocalStore.getSettings() }
    val volume = settings.metronomeVolume / 100f
    val appContext = context.applicationContext
    val metronome = remember { Metronome(appContext) }
    val speech = remember { Speech(appContext) }
    val sound = remember(settings.metronomeSound) { MetronomeSound.of(settings.metronomeSound) }

    DisposableEffect(Unit) {
        onDispose {
            metronome.release()
            speech.shutdown()
        }
    }

    LaunchedEffect(settings.customSoundUri) {
        metronome.setCustom(settings.customSoundUri)
    }

    var animPlaying by remember { mutableStateOf(true) }
    var animSpeed by remember { mutableFloatStateOf(1f) }

    /** 本动作全部组数完成后的去向 */
    fun afterSet() {
        if (setIndex + 1 >= sets) {
            feedbackPain = peakPain.toFloat()
            phase = Phase.FEEDBACK
        } else {
            phase = Phase.REST
        }
    }

    fun finishExercise(doneSets: String) {
        doneList = doneList + ExerciseEntry(exercise.name, doneSets)
        if (index + 1 >= total) {
            phase = Phase.DONE
        } else {
            index++
            setIndex = 0
            repIndex = 0
            phase = Phase.RUNNING
        }
    }

    // 计时：休息倒计时 / 保持倒计时 / 正计时自动完成一组
    LaunchedEffect(index, setIndex, repIndex, phase) {
        val seconds = when {
            phase == Phase.REST -> exercise.dosage.restSec
            phase == Phase.RUNNING && exercise.dosage.mode == DoseMode.HOLD -> exercise.dosage.seconds
            phase == Phase.RUNNING && exercise.dosage.mode == DoseMode.COUNT_UP -> exercise.dosage.seconds
            else -> 0
        }
        if (seconds <= 0) return@LaunchedEffect

        remaining = seconds
        while (remaining > 0) {
            delay(1000L)
            remaining--
            // 休息最后 3 秒滴一下，不用盯着屏幕
            if (settings.restCue && phase == Phase.REST && remaining in 1..3) {
                metronome.cue(Cue.COUNTDOWN, volume)
            }
        }

        if (phase == Phase.REST) {
            if (settings.restCue) metronome.cue(Cue.FINISH, volume)
            setIndex++
            phase = Phase.RUNNING
        } else if (exercise.dosage.mode == DoseMode.HOLD) {
            if (repIndex + 1 >= reps) {
                repIndex = 0
                afterSet()
            } else {
                repIndex++
            }
        } else {
            afterSet()
        }
    }

    // 节拍：按设置的速度打拍；计数类动作还能语音报数
    LaunchedEffect(index, setIndex, repIndex, phase, settings.metronomeEnabled, settings.metronomeBpm) {
        if (!settings.metronomeEnabled) return@LaunchedEffect
        if (phase != Phase.RUNNING && phase != Phase.REST) return@LaunchedEffect
        val interval = (60_000L / settings.metronomeBpm).coerceAtLeast(200L)
        var beat = 0
        while (true) {
            val accent = settings.metronomeAccent
            metronome.click(sound, strong = accent > 0 && beat % accent == 0, volume)
            if (settings.countAloud && phase == Phase.RUNNING && exercise.dosage.mode == DoseMode.REPS) {
                speech.say((beat % reps + 1).toString())
            }
            beat++
            delay(interval)
        }
    }

    // 全部动作完成：落库成一条训练记录
    LaunchedEffect(phase) {
        if (phase == Phase.DONE) {
            val minutes = ((System.currentTimeMillis() - session.startedAt) / 60000L)
                .toInt()
                .coerceAtMost(600)
            LocalStore.addRecord(
                Record(
                    id = "rec_${System.currentTimeMillis()}",
                    date = LocalStore.todayKey(),
                    timestamp = System.currentTimeMillis(),
                    painBefore = session.painBefore,
                    painPeak = peakPain,
                    rpe = lastRpe,
                    durationMin = minutes,
                    note = "",
                    exercises = doneList,
                ),
            )
            Toast.makeText(context, "已记录本次训练", Toast.LENGTH_SHORT).show()
            SessionHolder.session = null
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "第 ${index + 1} / $total 个动作",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                SessionHolder.session = null
                onFinish()
            }) { Text("退出") }
        }

        Spacer(Modifier.height(6.dp))
        Text(exercise.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "${exercise.category.label} · L${exercise.level} · ${exercise.dosage.describe()}",
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
        )

        Spacer(Modifier.height(12.dp))

        // 动作示范动画
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ExerciseAnimation(
                    frames = framesFor(exercise.id),
                    playing = animPlaying,
                    speed = animSpeed,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { animPlaying = !animPlaying }) {
                        Text(if (animPlaying) "暂停示范" else "播放示范")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        animSpeed = when (animSpeed) {
                            1f -> 0.5f
                            0.5f -> 1.5f
                            else -> 1f
                        }
                    }) {
                        Text(
                            when (animSpeed) {
                                0.5f -> "慢放 0.5×"
                                1.5f -> "快放 1.5×"
                                else -> "常速 1×"
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (phase == Phase.REST) {
                    Text("组间休息", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("$remaining", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "秒后开始第 ${setIndex + 2} 组",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                    )
                    TextButton(onClick = {
                        setIndex++
                        phase = Phase.RUNNING
                    }) { Text("跳过休息") }
                } else {
                    val title = when (exercise.dosage.mode) {
                        DoseMode.HOLD -> "保持中"
                        DoseMode.COUNT_UP -> "进行中"
                        DoseMode.REPS -> "第 ${setIndex + 1} / $sets 组"
                    }
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (exercise.dosage.mode == DoseMode.REPS) "$reps 次" else "$remaining 秒",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (exercise.dosage.mode == DoseMode.HOLD && reps > 1) {
                        Text(
                            "本组第 ${repIndex + 1} / $reps 次",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                        )
                    }
                    if (exercise.dosage.mode == DoseMode.REPS) {
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = { afterSet() }) { Text("完成本组") }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("膝盖口令", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    exercise.kneeCue,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Text(
                    "安全要点",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                exercise.safety.forEach {
                    Text("· $it", fontSize = 13.sp, color = Color(0xFF6B7280))
                }

                Text(
                    "常见错误",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                exercise.mistakes.forEach {
                    Text("· $it", fontSize = 13.sp, color = Color(0xFF6B7280))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showReport = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("膝盖疼痛上报")
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("膝盖疼痛上报") },
            text = {
                Column {
                    Text(
                        "现在的疼痛评分：${reportPain.roundToInt()} / 10",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = reportPain,
                        onValueChange = { reportPain = it },
                        valueRange = 0f..10f,
                        steps = 9,
                    )
                    Row {
                        Text("0 不痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(Modifier.width(8.dp))
                        Spacer(Modifier.weight(1f))
                        Text("10 剧痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pain = reportPain.roundToInt()
                    peakPain = maxOf(peakPain, pain)
                    val result = KneeRehabRules.handlePainReport(session, pain)
                    showReport = false
                    if (result.stop) {
                        val finished = "第 ${setIndex + 1} 组后停止"
                        doneList = doneList + ExerciseEntry(exercise.name, finished)
                        if (index + 1 >= total) {
                            phase = Phase.DONE
                        } else {
                            index++
                            setIndex = 0
                            repIndex = 0
                            phase = Phase.RUNNING
                        }
                    }
                    advice = result
                }) { Text("提交") }
            },
            dismissButton = {
                TextButton(onClick = { showReport = false }) { Text("取消") }
            },
        )
    }

    if (phase == Phase.FEEDBACK) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("完成 ${exercise.name}") },
            text = {
                Column {
                    Text(
                        "疼痛评分：${feedbackPain.roundToInt()} / 10",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = feedbackPain,
                        onValueChange = { feedbackPain = it },
                        valueRange = 0f..10f,
                        steps = 9,
                    )
                    Row {
                        Text("0 不痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(Modifier.weight(1f))
                        Text("10 剧痛", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "自觉疲劳（RPE）：${feedbackRpe.roundToInt()} / 10",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = feedbackRpe,
                        onValueChange = { feedbackRpe = it },
                        valueRange = 0f..10f,
                        steps = 9,
                    )
                    Row {
                        Text("0 完全不累", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(Modifier.weight(1f))
                        Text("10 力竭", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    peakPain = maxOf(peakPain, feedbackPain.roundToInt())
                    lastRpe = feedbackRpe.roundToInt()
                    finishExercise(sets.toString())
                }) {
                    Text(if (index + 1 >= total) "完成训练" else "下一个动作")
                }
            },
            dismissButton = null,
        )
    }

    advice?.let { result ->
        AlertDialog(
            onDismissRequest = { advice = null },
            title = { Text(if (result.seekHelp) "立即停止训练" else "处置建议") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(result.message, fontSize = 14.sp)
                    result.alternative?.let { alt ->
                        Text(
                            "建议改做：${alt.name}（${alt.dosage.describe()}）",
                            fontSize = 13.sp,
                            color = Color(0xFF0A84FF),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { advice = null }) { Text("知道了") }
            },
            dismissButton = if (result.seekHelp) {
                {
                    TextButton(onClick = {
                        advice = null
                        SessionHolder.session = null
                        onFinish()
                    }) { Text("结束训练", color = Color(0xFFFF3B30)) }
                }
            } else {
                null
            },
        )
    }
}
