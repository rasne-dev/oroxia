package com.oroxia.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oroxia.launcher.data.local.OroxiaDatabase
import com.oroxia.launcher.ui.common.IconCache
import com.oroxia.launcher.worker.ScanWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!replacing && packageName != null) {
                    ScanWorker.enqueueAppScan(context, packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!replacing && packageName != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = OroxiaDatabase.getInstance(context)
                            db.appDao().deleteApp(packageName)
                            IconCache.remove(packageName)
                        } catch (e: Exception) {
                            // Safe error handling to prevent any receiver crash
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                ScanWorker.scheduleWeeklyScan(context)
            }
        }
    }
}
