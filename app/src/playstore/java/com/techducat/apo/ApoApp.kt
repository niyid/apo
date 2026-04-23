package com.techducat.apo

import timber.log.Timber

// Playstore flavor — no Firebase/Crashlytics.
// Falls back to base ApoApp behaviour; override hooks are no-ops.
class ApoAppFlavor : ApoApp() {
    override fun productionTree(): Timber.Tree = Timber.DebugTree()
    override fun onCreateFlavor() {}
}
