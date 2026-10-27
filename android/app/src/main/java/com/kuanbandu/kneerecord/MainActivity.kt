package com.kuanbandu.kneerecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.engine.ExerciseLibrary
import com.kuanbandu.kneerecord.reminder.ReminderScheduler
import com.kuanbandu.kneerecord.ui.KneeRecordApp
import com.kuanbandu.kneerecord.ui.components.animationIssues
import com.kuanbandu.kneerecord.ui.components.missingAnimations
import com.kuanbandu.kneerecord.ui.theme.KneeRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStore.init(applicationContext)

        // 启动自检：动作库里任何一个动作要素不全就拒绝启动，
        // 宁可崩溃也不把缺要素的动作推给用户。
        val problems = ExerciseLibrary.validate() +
            missingAnimations(ExerciseLibrary.ALL.map { it.id })
                .map { "缺少示范动画：$it" } +
            animationIssues().map { "示范动画有问题：$it" }
        if (problems.isNotEmpty()) {
            throw IllegalStateException("动作库自检未通过：\n" + problems.joinToString("\n"))
        }

        ReminderScheduler.sync(applicationContext)
        enableEdgeToEdge()
        setContent {
            KneeRecordTheme {
                KneeRecordApp()
            }
        }
    }
}
