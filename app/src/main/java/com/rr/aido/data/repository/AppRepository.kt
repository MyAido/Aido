package com.rr.aido.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.rr.aido.data.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        resolveInfos.map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            AppInfo(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = activityInfo.packageName,
                icon = resolveInfo.loadIcon(pm)
            )
        }.distinctBy { it.packageName } // Handle multiple launcher activities for same app
         .sortedBy { it.name.lowercase() }
    }
}
