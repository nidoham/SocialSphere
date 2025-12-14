package com.nidoham.socialsphere

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.nidoham.socialsphere.util.NewPipeExtractorInstance

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        NewPipeExtractorInstance.init()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // মোট RAM এর 25%
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB ডিস্ক ক্যাশ
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger()) // Release build এ remove করবেন
            .build()
    }
}
