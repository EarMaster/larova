package app.larova

import android.app.Application

/**
 * Application entry point. Dependency injection is wired here once the graph exists; keeping the
 * class in place from the first commit means the manifest never has to change for it.
 */
class LarovaApplication : Application()
