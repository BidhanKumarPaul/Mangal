package com.bkpit.mangal

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.bkpit.mangal.lifecycle.ModelLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MangalApplication : Application() {

    @Inject
    lateinit var modelLifecycleObserver: ModelLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        // Phase 5: unload the LLM from RAM when the app is backgrounded, and
        // reload lazily on the next request. Prevents Mangal being the reason
        // a low-RAM phone starts killing other apps.
        ProcessLifecycleOwner.get().lifecycle.addObserver(modelLifecycleObserver)
    }
}
