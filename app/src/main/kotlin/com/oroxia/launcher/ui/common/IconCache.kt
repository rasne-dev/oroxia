package com.oroxia.launcher.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IconCache {
    private const val CACHE_SIZE = 250
    private val memoryCache = LruCache<String, Bitmap>(CACHE_SIZE)

    fun get(packageName: String, sizePx: Int): Bitmap? {
        val key = "${packageName}_$sizePx"
        return memoryCache.get(key)
    }

    suspend fun loadIcon(context: Context, packageName: String, sizePx: Int): Bitmap? {
        val key = "${packageName}_$sizePx"
        val existing = memoryCache.get(key)
        if (existing != null) return existing

        return withContext(Dispatchers.IO) {
            try {
                val pm = context.applicationContext.packageManager
                val drawable = pm.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(
                    width = sizePx.coerceAtLeast(48),
                    height = sizePx.coerceAtLeast(48),
                    config = Bitmap.Config.ARGB_8888
                )
                memoryCache.put(key, bitmap)
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    fun remove(packageName: String) {
        val keysToRemove = mutableListOf<String>()
        val snapshot = memoryCache.snapshot()
        for (k in snapshot.keys) {
            if (k.startsWith("${packageName}_")) {
                keysToRemove.add(k)
            }
        }
        for (k in keysToRemove) {
            memoryCache.remove(k)
        }
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
