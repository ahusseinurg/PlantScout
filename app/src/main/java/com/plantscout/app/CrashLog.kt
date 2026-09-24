package com.plantscout.app

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Application class: installs the crash catcher as early as possible. */
class PlantScoutApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}

/** Saves the details of any crash so it can be shown the next time the app opens. */
object CrashLog {

    private fun file(ctx: Context) = File(ctx.filesDir, "last_crash.txt")

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                val version = try {
                    val pi = app.packageManager.getPackageInfo(app.packageName, 0)
                    "${pi.versionName}"
                } catch (e: Exception) {
                    "?"
                }
                val header = buildString {
                    append("PlantScout version: ").append(version).append('\n')
                    append("Android: ").append(Build.VERSION.RELEASE)
                        .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
                    append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                    append("Time: ").append(
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    ).append('\n')
                    append("Thread: ").append(thread.name).append("\n\n")
                }
                file(app).writeText(header + sw.toString())
            } catch (ignored: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(ctx: Context): String? = file(ctx).takeIf { it.exists() }?.readText()

    fun clear(ctx: Context) {
        file(ctx).delete()
    }
}
