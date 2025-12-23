package com.nidoham.socialsphere.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/**
 * Optimized service for continuous onlineAt status updates.
 * Never removes users/{uid}/onlineAt - status persists across all states.
 */
class StatusService : Service() {

    private var statusRef: DatabaseReference? = null
    private var uid: String? = null
    private var updateJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "StatusService"

        // Corrected intervals with accurate comments
        private const val INTERVAL_FOREGROUND_MS = 150_000L   // 2.5 minutes
        private const val INTERVAL_BACKGROUND_MS = 300_000L   // 5 minutes
        private const val INTERVAL_DOZE_MS = 600_000L         // 10 minutes
        private const val RETRY_DELAY_MS = 30_000L            // 30 seconds on error

        const val ACTION_START = "com.nidoham.socialsphere.START_STATUS_UPDATE"
        const val ACTION_STOP = "com.nidoham.socialsphere.STOP_STATUS_UPDATE"

        @JvmStatic
        fun start(context: Context) {
            context.startService(Intent(context, StatusService::class.java).apply {
                action = ACTION_START
            })
        }

        @JvmStatic
        fun stop(context: Context) {
            context.startService(Intent(context, StatusService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        acquireWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (updateJob?.isActive != true) startStatusUpdate()
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return

        (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.let { pm ->
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StatusService::WakeLock"
            ).apply {
                acquire() // No timeout - release manually
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun startStatusUpdate() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Log.e(TAG, "No authenticated user")
            stopSelf()
            return
        }

        uid = currentUser.uid
        statusRef = FirebaseDatabase.getInstance().getReference("users/${uid}/onlineAt")


        updateJob = serviceScope.launch {
            Log.d(TAG, "Starting onlineAt updates for $uid")
            while (isActive) {
                try {
                    if (updateOnlineAt()) {
                        delay(getAdaptiveInterval())
                    } else {
                        delay(RETRY_DELAY_MS)
                    }
                } catch (e: CancellationException) {
                    throw e // Propagate cancellation
                } catch (e: Exception) {
                    Log.e(TAG, "Update error: ${e.message}")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
    }

    private suspend fun updateOnlineAt(): Boolean = suspendCancellableCoroutine { cont ->
        val ref = statusRef ?: run {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }

        val timestamp = System.currentTimeMillis()
        ref.setValue(timestamp)
            .addOnSuccessListener {
                Log.v(TAG, "onlineAt updated: $timestamp")
                if (cont.isActive) cont.resume(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "onlineAt update failed: ${e.message}")
                if (cont.isActive) cont.resume(false)
            }
    }

    private fun getAdaptiveInterval(): Long {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        return when {
            pm?.isDeviceIdleMode == true -> INTERVAL_DOZE_MS
            isAppInForeground() -> INTERVAL_FOREGROUND_MS
            else -> INTERVAL_BACKGROUND_MS
        }
    }

    private fun isAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        return am.runningAppProcesses?.any { process ->
            process.processName == packageName &&
                    process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } == true
    }

    private fun stopService() {
        Log.d(TAG, "Stopping status service (status preserved)")
        updateJob?.cancel()
        updateJob = null
        statusRef = null
        uid = null
        releaseWakeLock()
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed (status preserved)")
        updateJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onTaskRemoved(intent: Intent?) {
        Log.d(TAG, "Task removed - restarting service")
        startStatusUpdate()
        super.onTaskRemoved(intent)
    }
}
