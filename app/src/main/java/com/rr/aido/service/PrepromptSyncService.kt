package com.rr.aido.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.Preprompt
import com.rr.aido.data.repository.MarketplaceRepository
import com.rr.aido.data.repository.MarketplaceRepositoryImpl
import com.rr.aido.data.repository.Result
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class PrepromptSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var marketplaceRepository: MarketplaceRepository

    // Sync interval in milliseconds (default: 1 hour)
    private val syncInterval = 60 * 60 * 1000L

    // Mock user ID (in production, get from authentication)
    private val currentUserId = "user_current_sync"

    override fun onCreate() {
        super.onCreate()
        dataStoreManager = DataStoreManager(applicationContext)
        marketplaceRepository = MarketplaceRepositoryImpl()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Initializing sync..."))

        Log.d(TAG, "PrepromptSyncService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SYNC_NOW -> {
                Log.d(TAG, "Manual sync requested")
                performSync()
            }
            ACTION_START_AUTO_SYNC -> {
                Log.d(TAG, "Auto-sync started")
                schedulePeriodicSync()
            }
            ACTION_STOP_AUTO_SYNC -> {
                Log.d(TAG, "Auto-sync stopped")
                stopSelf()
            }
            else -> {
                Log.d(TAG, "Starting default sync")
                performSync()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun schedulePeriodicSync() {
        serviceScope.launch {
            while (isActive) {
                performSync()
                delay(syncInterval)
            }
        }
    }

    private fun performSync() {
        serviceScope.launch {
            try {
                updateNotification("Syncing preprompts...")

                // Get local preprompts
                val localPreprompts = dataStoreManager.prepromptsFlow.first()
                Log.d(TAG, "Found ${localPreprompts.size} local preprompts")

                // Perform sync
                when (val result = marketplaceRepository.syncUserPreprompts(currentUserId, localPreprompts)) {
                    is Result.Success -> {
                        val syncStatus = result.data
                        val message = if (syncStatus.pendingUploads == 0 && syncStatus.pendingDownloads == 0) {
                            "Sync completed successfully"
                        } else {
                            "Sync in progress: ${syncStatus.pendingUploads} uploads, ${syncStatus.pendingDownloads} downloads"
                        }
                        updateNotification(message)
                        Log.d(TAG, message)

                        // Download any new preprompts from cloud
                        downloadCloudPreprompts()
                    }
                    is Result.Error -> {
                        val errorMessage = "Sync failed: ${result.message}"
                        updateNotification(errorMessage)
                        Log.e(TAG, errorMessage)
                    }
                    is Result.Loading -> {
                        updateNotification("Sync in progress...")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during sync", e)
                updateNotification("Sync error: ${e.message}")
            }
        }
    }

    private suspend fun downloadCloudPreprompts() {
        try {
            when (val cloudPreprompts = marketplaceRepository.downloadUserPreprompts(currentUserId)) {
                is Result.Success -> {
                    val remotePreprompts = cloudPreprompts.data
                    if (remotePreprompts.isNotEmpty()) {
                        val localPreprompts = dataStoreManager.prepromptsFlow.first()

                        // Merge: Add cloud preprompts that don't exist locally
                        val localTriggers = localPreprompts.map { it.trigger }.toSet()
                        val newPreprompts = remotePreprompts.filter { it.trigger !in localTriggers }

                        if (newPreprompts.isNotEmpty()) {
                            val mergedPreprompts = localPreprompts + newPreprompts
                            dataStoreManager.savePreprompts(mergedPreprompts)
                            Log.d(TAG, "Downloaded and merged ${newPreprompts.size} new preprompts")
                            updateNotification("Downloaded ${newPreprompts.size} new commands")
                        }
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Error downloading cloud preprompts: ${cloudPreprompts.message}")
                }
                is Result.Loading -> {
                    // Do nothing
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading cloud preprompts", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Preprompt Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background synchronization of preprompts"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aido Sync")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "PrepromptSyncService destroyed")
    }

    companion object {
        private const val TAG = "PrepromptSyncService"
        private const val CHANNEL_ID = "preprompt_sync_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_SYNC_NOW = "com.rr.aido.SYNC_NOW"
        const val ACTION_START_AUTO_SYNC = "com.rr.aido.START_AUTO_SYNC"
        const val ACTION_STOP_AUTO_SYNC = "com.rr.aido.STOP_AUTO_SYNC"


        fun startSync(context: Context) {
            val intent = Intent(context, PrepromptSyncService::class.java).apply {
                action = ACTION_SYNC_NOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }


        fun startAutoSync(context: Context) {
            val intent = Intent(context, PrepromptSyncService::class.java).apply {
                action = ACTION_START_AUTO_SYNC
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }


        fun stopSync(context: Context) {
            val intent = Intent(context, PrepromptSyncService::class.java).apply {
                action = ACTION_STOP_AUTO_SYNC
            }
            context.startService(intent)
        }
    }
}
