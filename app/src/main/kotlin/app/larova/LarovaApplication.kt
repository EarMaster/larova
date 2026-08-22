package app.larova

import android.app.Application
import app.larova.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point.
 *
 * The Koin logger is at ERROR rather than DEBUG: this app has no analytics and no crash reporter,
 * and a graph that logs its every resolution to logcat is the sort of thing that ends up
 * mentioning what a family called their tiles.
 */
class LarovaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@LarovaApplication)
            modules(appModule)
        }
    }
}
