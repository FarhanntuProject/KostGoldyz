package id.kosku.pengingat

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Dijalankan sekali sehari sekitar jam 09.00 untuk mengecek tagihan. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val s = Repo(applicationContext).load()
        val cfg = s.cfg ?: return Result.success()
        val t = hitung(cfg, s.bayar)
        if (t.status != Status.AMAN) {
            val (title, text) = pesanNotifikasi(cfg, t)
            Notif.show(applicationContext, title, text)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "pengingat-kos-harian"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(9, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delayMin = Duration.between(now, next).toMinutes()

            val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMin, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}

fun pesanNotifikasi(cfg: KosConfig, t: Tagihan): Pair<String, String> {
    val title = when {
        t.days < 0 -> "Sewa kos terlambat ${-t.days} hari"
        t.days == 0L -> "Hari ini jatuh tempo sewa kos"
        else -> "Sewa kos jatuh tempo ${t.days} hari lagi"
    }
    val tunggakan = if (t.overdue.size > 1) " Tunggakan ${t.overdue.size} bulan, total ${rp(t.overdue.size * cfg.sewa)}." else ""
    val rek = if (cfg.rek.isNotBlank()) " Transfer ke ${cfg.rek}." else ""
    val text = "Sewa ${bulan(t.target)} ${rp(cfg.sewa)}, jatuh tempo ${tanggal(t.due)}.$tunggakan$rek"
    return title to text
}

object Notif {
    private const val CHANNEL = "tagihan_kos"

    fun bolehNotif(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Pengingat sewa kos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pengingat sebelum dan sesudah jatuh tempo sewa kos"
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, title: String, text: String) {
        if (!bolehNotif(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1001, n)
    }
}
