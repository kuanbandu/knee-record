package com.kuanbandu.kneerecord.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kuanbandu.kneerecord.data.LocalStore
import java.util.Calendar

/**
 * 每日提醒：本机闹钟 + 本地通知，不需要服务端（这是相对小程序版的升级点）。
 * 应用启动时会调用 sync() 重新排期；重启手机后需先打开一次应用才会恢复提醒。
 */
object ReminderScheduler {

    private const val REQ_CODE = 1001

    fun sync(context: Context) {
        val settings = LocalStore.getSettings()
        cancel(context)
        if (settings.reminderEnabled) schedule(context, settings.reminderTime)
    }

    fun schedule(context: Context, time: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val pi = pendingIntent(context)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
