package com.bkpit.mangal.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bkpit.mangal.llm.LlamaEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registered against ProcessLifecycleOwner in MangalApplication. A multi-GB
 * model sitting loaded in RAM while the user is in another app is exactly
 * the "why is my phone slow/hot" complaint this exists to prevent — we
 * unload on background and reload lazily (LlamaEngine.load is called again
 * from ChatViewModel/ModelManagerViewModel on the next request; this class
 * only tracks *which* file to reload).
 *
 * NOTE: this only unloads the LLM. The path back in re-reads the active
 * model file from Settings via ModelManagerViewModel/ChatViewModel — this
 * class deliberately does not hold a reference to which file was active, to
 * avoid two sources of truth for "what's the active model" drifting apart.
 */
@Singleton
class ModelLifecycleObserver @Inject constructor(
    private val llamaEngine: LlamaEngine
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onStop(owner: LifecycleOwner) {
        // App went to background (not just screen off — ProcessLifecycleOwner
        // tracks the whole process, all activities).
        scope.launch {
            if (llamaEngine.isLoaded) llamaEngine.unload()
        }
    }
}
