package me.semoro.flameworks

import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.*

@JupyterLibrary
internal class Integration : JupyterIntegration() {
    
    override fun Builder.onLoaded() {
        import("me.semoro.flameworks.FlameWorks")
    }
}