package com.kuanbandu.kneerecord.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.material3.Switch
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
import androidx.core.content.ContextCompat
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.reminder.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 使用须知原文（本应用不提供任何锻炼建议，此处仅为提示性声明） */
private val NOTICE_TEXT = "我已知晓运动中如有不适立即停止"

private val NOTICE_DETAIL = listOf(
    "本应用仅用于个人锻炼记录，不提供任何动作推荐或锻炼建议。",
    "本应用不替代专业人员的判断。",
    "身体持续不适时应停止锻炼，是否寻求专业帮助由你自己决定。",
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            ReminderScheduler.schedule(context, time)
            Toast.makeText(context, "已开启，每天 $time 会提醒你记录", Toast.LENGTH_SHORT).show()
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
        ReminderScheduler.sync(context) // 备份里的提醒设置也一并恢复了
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
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("每日提醒", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开启记录提醒", fontSize = 16.sp)
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
                                Toast.makeText(context, "已开启，每天 $time 会提醒你记录", Toast.LENGTH_SHORT).show()
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

        // 使用须知
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("使用须知", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    NOTICE_TEXT,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                NOTICE_DETAIL.forEach { line ->
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
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("我的数据", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已保存锻炼记录", fontSize = 16.sp, modifier = Modifier.weight(1f))
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
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("备份与恢复", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("数据只存在本机，换手机或重装前先导出", fontSize = 12.sp, color = Color(0xFF6B7280))

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
                        exportJsonLauncher.launch("knee-record-${fileStamp()}.json")
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
                        Text("一条记录一行，Excel 可打开", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    TextButton(onClick = {
                        exportCsvLauncher.launch("knee-record-${fileStamp()}.csv")
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
            "膝盖锻炼记录 · 本地数据版\n只负责记录与展示，不提供任何动作推荐或锻炼建议。",
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
            text = { Text("将删除全部锻炼记录与个人资料，且无法恢复。") },
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
