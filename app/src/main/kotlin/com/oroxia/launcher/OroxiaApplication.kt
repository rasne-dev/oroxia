package com.oroxia.launcher

import android.app.Application
import com.oroxia.launcher.data.local.OroxiaDatabase
import com.oroxia.launcher.data.pref.UserPreferencesRepository
import com.oroxia.launcher.worker.ScanWorker

class OroxiaApplication : Application() {
    lateinit var database: OroxiaDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = OroxiaDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)

        // Schedule periodic weekly scan
        ScanWorker.scheduleWeeklyScan(this)
    }
}
