package com.kuanbandu.kneerecord.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kuanbandu.kneerecord.audio.Metronome
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.model.MetronomeSound
import com.kuanbandu.kneerecord.reminder.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** 免责声明原文 */
private val DISCLAIMER_TEXT = "我已知晓训练中如有不适立即停止"

private val DISCLAIMER_DETAIL = listOf(
    "本应用提供的训练方案为通用参考，不替代执业医师或康复治疗师的评估与处方。",
    "本应用不构成医疗诊断、治疗方案或医嘱。",
    "训练中疼痛加重、肿胀明显或出现关节卡锁，应及时就医。",
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { LocalStore.getSettings() }
    val version = LocalStore.recordsVersion.value
    val recordCount = remember(version) { LocalStore.getRecords().size }

    var enabled by remember { mutableStateOf(settings.reminderEnabled) }
    var time by remember { mutableStateOf(settings.reminderTime) }
    var showClear by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var pendingCount by remember { mutableStateOf(0) }

    // 节拍与提示音
    var metroEnabled by remember { mutableStateOf(settings.metronomeEnabled) }
    var bpm by remember { mutableFloatStateOf(settings.metronomeBpm.toFloat()) }
    var soundKey by remember { mutableStateOf(settings.metronomeSound) }
    var vol by remember { mutableFloatStateOf(settings.metronomeVolume.toFloat()) }
    var accent by remember { mutableIntStateOf(settings.metronomeAccent) }
    var countAloud by remember { mutableStateOf(settings.countAloud) }
    var restCueOn by remember { mutableStateOf(settings.restCue) }
    var customUri by remember { mutableStateOf(settings.customSoundUri) }

    val preview = remember { Metronome(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { preview.release() }
    }
    LaunchedEffect(Unit) {
        if (settings.customSoundUri.isNotBlank()) preview.setCustom(settings.customSoundUri)
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val value = uri.toString()
        customUri = value
        soundKey = MetronomeSound.CUSTOM.key
        LocalStore.updateSettings(customSoundUri = value)
        LocalStore.updateSettings(metronomeSound = MetronomeSound.CUSTOM.key)
        preview.setCustom(value)
        Toast.makeText(context, "已选好，点「试听」听一下", Toast.LENGTH_SHORT).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            ReminderScheduler.schedule(context, time)
            Toast.makeText(context, "已开启，每天 $time 会提醒你训练", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "未授予通知权限，提醒不会弹出", Toast.LENGTH_SHORT).show()
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching { writeToUri(context, uri, LocalStore.exportJson()) }
        Toast.makeText(
            context,
            if (ok.isSuccess) "备份已导出" else "导出失败：${ok.exceptionOrNull()?.message}",
            Toast.LENGTH_LONG,
        ).show()
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching { writeToUri(context, uri, LocalStore.exportCsv()) }
        Toast.makeText(
            context,
            if (ok.isSuccess) "表格已导出" else "导出失败：${ok.exceptionOrNull()?.message}",
            Toast.LENGTH_LONG,
        ).show()
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching { readFromUri(context, uri) }.getOrDefault("")
        if (text.isBlank()) {
            Toast.makeText(context, "读取文件失败", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val count = runCatching { LocalStore.parseImportCount(text) }.getOrElse {
            Toast.makeText(context, "这不是本应用的备份文件", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        if (count == 0) {
            Toast.makeText(context, "备份里没有记录", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        pendingCount = count
        pendingImport = text
    }

    fun runImport(merge: Boolean) {
        val text = pendingImport ?: return
        val count = runCatching { LocalStore.importFromJson(text, merge) }.getOrElse {
            Toast.makeText(context, "导入失败：${it.message}", Toast.LENGTH_LONG).show()
            pendingImport = null
            return
        }
        ReminderScheduler.sync(context)
        val restored = LocalStore.getSettings()
        enabled = restored.reminderEnabled
        time = restored.reminderTime
        pendingImport = null
        Toast.makeText(context, "已导入 $count 条记录", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // 每日提醒
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("每日提醒", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开启训练提醒", fontSize = 16.sp)
                        Text("到点后手机会收到通知", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { value ->
                            enabled = value
                            LocalStore.updateSettings(reminderEnabled = value)
                            if (!value) {
                                ReminderScheduler.cancel(context)
                                Toast.makeText(context, "已关闭提醒", Toast.LENGTH_SHORT).show()
                                return@Switch
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                ReminderScheduler.schedule(context, time)
                                Toast.makeText(context, "已开启，每天 $time 会提醒你训练", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("提醒时间", fontSize = 16.sp)
                        Text("本机闹钟通知，不需要联网", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Button(onClick = {
                        val parts = time.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 19
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                time = String.format("%02d:%02d", h, m)
                                LocalStore.updateSettings(reminderTime = time)
                                if (enabled) ReminderScheduler.schedule(context, time)
                            },
                            hour,
                            minute,
                            true,
                        ).show()
                    }) {
                        Text("$time ›")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 节拍与提示音
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("节拍与提示音", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "跟练时按拍响，节奏快慢和音色自己定",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("训练时打节拍", fontSize = 16.sp)
                        Text("关掉后只剩画面，不出声", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Switch(
                        checked = metroEnabled,
                        onCheckedChange = {
                            metroEnabled = it
                            LocalStore.updateSettings(metronomeEnabled = it)
                        },
                    )
                }

                Text(
                    "音色",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(MetronomeSound.WOOD, MetronomeSound.TICK).forEach { s ->
                        TextButton(onClick = {
                            soundKey = s.key
                            LocalStore.updateSettings(metronomeSound = s.key)
                            preview.click(s, strong = true, vol / 100f)
                        }) {
                            Text(
                                s.label,
                                color = if (soundKey == s.key) Color(0xFF0A84FF) else Color(0xFF6B7280),
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(MetronomeSound.BELL, MetronomeSound.CUSTOM).forEach { s ->
                        TextButton(onClick = {
                            soundKey = s.key
                            LocalStore.updateSettings(metronomeSound = s.key)
                            preview.click(s, strong = true, vol / 100f)
                        }) {
                            Text(
                                s.label,
                                color = if (soundKey == s.key) Color(0xFF0A84FF) else Color(0xFF6B7280),
                            )
                        }
                    }
                }

                if (soundKey == MetronomeSound.CUSTOM.key) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自选音频", fontSize = 14.sp)
                            Text(
                                if (customUri.isBlank()) "还没选，从手机里挑一个" else "已选，可试听或清除",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                            )
                        }
                        TextButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }) {
                            Text("选择")
                        }
                        if (customUri.isNotBlank()) {
                            TextButton(onClick = {
                                preview.click(MetronomeSound.CUSTOM, strong = true, vol / 100f)
                            }) { Text("试听") }
                            TextButton(onClick = {
                                customUri = ""
                                soundKey = MetronomeSound.WOOD.key
                                LocalStore.updateSettings(customSoundUri = "")
                                LocalStore.updateSettings(metronomeSound = MetronomeSound.WOOD.key)
                                preview.setCustom("")
                            }) { Text("清除", color = Color(0xFFFF3B30)) }
                        }
                    }
                }

                Text(
                    "快慢：每分钟 ${bpm.roundToInt()} 拍",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Slider(
                    value = bpm,
                    onValueChange = { bpm = it },
                    valueRange = 40f..180f,
                    steps = 27,
                    onValueChangeFinished = {
                        LocalStore.updateSettings(metronomeBpm = bpm.roundToInt())
                    },
                )

                Text(
                    "音量：${vol.roundToInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Slider(
                    value = vol,
                    onValueChange = { vol = it },
                    valueRange = 0f..100f,
                    steps = 20,
                    onValueChangeFinished = {
                        LocalStore.updateSettings(metronomeVolume = vol.roundToInt())
                    },
                )

                Text(
                    "重音",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(0 to "不强调", 2 to "每 2 拍").forEach { (v, label) ->
                        TextButton(onClick = {
                            accent = v
                            LocalStore.updateSettings(metronomeAccent = v)
                        }) {
                            Text(
                                label,
                                color = if (accent == v) Color(0xFF0A84FF) else Color(0xFF6B7280),
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(3 to "每 3 拍", 4 to "每 4 拍").forEach { (v, label) ->
                        TextButton(onClick = {
                            accent = v
                            LocalStore.updateSettings(metronomeAccent = v)
                        }) {
                            Text(
                                label,
                                color = if (accent == v) Color(0xFF0A84FF) else Color(0xFF6B7280),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("语音报数", fontSize = 16.sp)
                        Text("计数类动作按拍念数", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Switch(
                        checked = countAloud,
                        onCheckedChange = {
                            countAloud = it
                            LocalStore.updateSettings(countAloud = it)
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("休息倒计时提示", fontSize = 16.sp)
                        Text("最后 3 秒滴一下，结束一声长音", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Switch(
                        checked = restCueOn,
                        onCheckedChange = {
                            restCueOn = it
                            LocalStore.updateSettings(restCue = it)
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 免责声明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("免责声明", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    DISCLAIMER_TEXT,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                DISCLAIMER_DETAIL.forEach { line ->
                    Text(
                        "· $line",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 我的数据
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("我的数据", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已保存训练记录", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("$recordCount 条", fontSize = 16.sp, color = Color(0xFF6B7280))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("清除全部数据", fontSize = 16.sp, color = Color(0xFFFF3B30))
                        Text("数据全部保存在本机，不会上传", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    TextButton(onClick = { showClear = true }) {
                        Text("清除", color = Color(0xFFFF3B30))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 备份与恢复
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("备份与恢复", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "数据只存在本机，换手机或重装前先导出",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("导出备份（JSON）", fontSize = 16.sp)
                        Text("完整备份，可再导入回来", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    TextButton(onClick = {
                        exportJsonLauncher.launch("knee-rehab-${fileStamp()}.json")
                    }) { Text("导出") }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("导出表格（CSV）", fontSize = 16.sp)
                        Text("一次训练一行，Excel 可打开", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    TextButton(onClick = {
                        exportCsvLauncher.launch("knee-rehab-${fileStamp()}.csv")
                    }) { Text("导出") }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("从备份导入", fontSize = 16.sp)
                        Text("选择之前导出的 JSON 文件", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    TextButton(onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    }) { Text("导入") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "膝盖康复训练 · 本地数据版\n本应用不构成医疗建议，训练方案请与主治医生或康复治疗师确认。",
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清除全部数据") },
            text = { Text("将删除全部训练记录与个人资料，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    LocalStore.clearAll()
                    ReminderScheduler.cancel(context)
                    enabled = false
                    time = "19:00"
                    showClear = false
                    Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show()
                }) { Text("确认清除", color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("取消") }
            },
        )
    }

    if (pendingImport != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入备份") },
            text = {
                Text(
                    "文件里有 $pendingCount 条记录。\n\n" +
                        "合并：保留本机现有记录，只补充不重复的。\n" +
                        "替换：清空本机数据后再导入。\n\n" +
                        "点空白处取消。",
                )
            },
            confirmButton = {
                TextButton(onClick = { runImport(merge = true) }) { Text("合并") }
            },
            dismissButton = {
                TextButton(onClick = { runImport(merge = false) }) {
                    Text("替换", color = Color(0xFFFF3B30))
                }
            },
        )
    }
}

private fun fileStamp(): String =
    SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())

private fun writeToUri(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use {
        it.write(text.toByteArray(Charsets.UTF_8))
    }
}

private fun readFromUri(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)?.use {
        it.readBytes().toString(Charsets.UTF_8)
    } ?: ""
