package com.oroxia.launcher.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.oroxia.launcher.R
import com.oroxia.launcher.data.local.OroxiaDatabase
import com.oroxia.launcher.data.pref.UserPreferencesRepository
import com.oroxia.launcher.domain.categorizer.AppScanner
import com.oroxia.launcher.domain.categorizer.SmartFolderManager
import com.oroxia.launcher.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ScanWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "oroxia_weekly_scan_worker"
        const val WORK_NAME_ONE_TIME = "oroxia_app_install_scan_worker"
        const val KEY_NEW_PACKAGE_NAME = "key_new_package_name"
        const val CHANNEL_ID = "oroxia_suggestions_channel"
        const val NOTIFICATION_ID = 1001

        fun scheduleWeeklyScan(context: Context) {
            val constraints = Constraints.Builder()
                .build() // Do not require network for local scan operations per AGENTS.md

            val periodicRequest = PeriodicWorkRequestBuilder<ScanWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun enqueueAppScan(context: Context, newPackageName: String? = null) {
            val data = workDataOf(KEY_NEW_PACKAGE_NAME to newPackageName)
            val request = OneTimeWorkRequestBuilder<ScanWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME_ONE_TIME}_${newPackageName ?: "all"}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val db = OroxiaDatabase.getInstance(appContext)
            val prefs = UserPreferencesRepository(appContext)
            val scanner = AppScanner(appContext, db.appDao(), db.folderDao())
            val folderManager = SmartFolderManager(db.appDao(), db.folderDao(), prefs)

            val apiKey = prefs.geminiApiKeyFlow.first()
            scanner.scanAndCategorizeInstalledApps(apiKey)
            prefs.updateLastScanTimestamp()

            val suggestions = folderManager.evaluateSuggestions()
            if (suggestions.isNotEmpty()) {
                val first = suggestions.first()
                showSuggestionNotification(
                    title = "Oroxia Akıllı Klasör Önerisi",
                    message = first.reason
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showSuggestionNotification(title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Oroxia Klasör Önerileri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Yeni uygulamalar için akıllı klasör öneri bildirimleri"
            }
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
