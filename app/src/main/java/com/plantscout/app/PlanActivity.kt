package com.plantscout.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PlanActivity : AppCompatActivity() {

    private var plan = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        plan = PlanGenerator.generate(PlantStore.load(this))
        findViewById<TextView>(R.id.planText).text = render(plan)
    }

    private fun render(text: String): CharSequence {
        val sb = SpannableStringBuilder()
        for (line in text.lines()) {
            when {
                line.startsWith("### ") -> heading(sb, line.removePrefix("### "), 1.05f)
                line.startsWith("## ") -> heading(sb, line.removePrefix("## "), 1.25f)
                line.startsWith("# ") -> heading(sb, line.removePrefix("# "), 1.5f)
                else -> sb.append(line).append('\n')
            }
        }
        return sb
    }

    private fun heading(sb: SpannableStringBuilder, text: String, size: Float) {
        val start = sb.length
        sb.append(text)
        sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(RelativeSizeSpan(size), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append('\n')
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.plan_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val plain = PlanGenerator.toPlainText(plan)
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_share -> {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Plant eradication plan")
                    putExtra(Intent.EXTRA_TEXT, plain)
                }
                startActivity(Intent.createChooser(send, "Share plan"))
                true
            }
            R.id.action_copy -> {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Eradication plan", plain))
                Toast.makeText(this, "Plan copied", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
