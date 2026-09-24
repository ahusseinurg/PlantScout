package com.plantscout.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Entry point. Normally it opens the main screen straight away.
 * If the app crashed last time, it shows the error so it can be copied and reported.
 * Uses only plain Android views so it works even if the app's theme is the problem.
 */
class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = CrashLog.read(this)
        if (report == null) {
            openMain()
            return
        }
        showReport(report)
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(0, 0)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun showReport(report: String) {
        val pad = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "PlantScout crashed last time"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.BLACK)
        })
        root.addView(TextView(this).apply {
            text = "Tap Copy error and paste it to whoever is fixing the app. Then tap Try again."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, dp(8))
        })

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply {
            text = "Copy error"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("PlantScout crash", report))
                Toast.makeText(this@LauncherActivity, "Error copied", Toast.LENGTH_SHORT).show()
            }
        })
        buttons.addView(Button(this).apply {
            text = "Share"
            setOnClickListener {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                startActivity(Intent.createChooser(send, "Share crash report"))
            }
        })
        buttons.addView(Button(this).apply {
            text = "Try again"
            setOnClickListener {
                CrashLog.clear(this@LauncherActivity)
                openMain()
            }
        })
        root.addView(buttons)

        val scroll = ScrollView(this)
        scroll.addView(TextView(this).apply {
            text = report
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.BLACK)
            setTextIsSelectable(true)
            setPadding(0, dp(12), 0, dp(12))
        })
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
    }
}
