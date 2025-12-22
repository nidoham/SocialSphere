package com.nidoham.socialsphere

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nidoham.socialsphere.service.StatusService
import com.nidoham.socialsphere.util.NewPipeExtractorInstance

class App : Application(), ImageLoaderFactory {

    private lateinit var auth: FirebaseAuth
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var isServiceStarted = false

    override fun onCreate() {
        super.onCreate()

        FirebaseDatabase.getInstance().apply {
            setPersistenceEnabled(true)
        }

        auth = FirebaseAuth.getInstance()
        NewPipeExtractorInstance.init()

        setupAuthListener()
        checkAndStartService()
    }

    private fun setupAuthListener() {
        authStateListener = FirebaseAuth.AuthStateListener {
            checkAndStartService()
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    private fun checkAndStartService() {
        val user = auth.currentUser
        if (user != null && !isServiceStarted) {
            StatusService.start(this)
            isServiceStarted = true
        }
        // Note: We intentionally don't stop the service to preserve online status
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }

}
