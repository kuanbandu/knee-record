package com.kuanbandu.kneerecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kuanbandu.kneerecord.data.LocalStore
import com.kuanbandu.kneerecord.reminder.ReminderScheduler
import com.kuanbandu.kneerecord.ui.KneeRecordApp
import com.kuanbandu.kneerecord.ui.theme.KneeRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStore.init(applicationContext)
        ReminderScheduler.sync(applicationContext) // 按当前设置重新排期提醒
        setContent {
            KneeRecordTheme {
                KneeRecordApp()
            }
        }
    }
}
