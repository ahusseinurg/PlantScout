package com.plantscout.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlanEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
        private const val MENU_SAVE = 1
        private const val MENU_PREVIEW = 2
        private const val MENU_SHARE = 3
        private const val MENU_EMAIL = 4
        private const val MENU_DELETE = 5
        private const val MENU_SEND = 6
    }

    private lateinit var plan: CustomerPlan
    private lateinit var sectionsBox: LinearLayout

    private lateinit var titleIn: EditText
    private lateinit var nameIn: EditText
    private lateinit var companyIn: EditText
    private lateinit var addressIn: EditText
    private lateinit var phoneIn: EditText
    private lateinit var emailIn: EditText
    private lateinit var jobIn: EditText

    /** One editable card per section. */
    private class SectionViews(val card: View, val title: EditText, val body: EditText)
    private val sectionViews = mutableListOf<SectionViews>()
    private var deleted = false
    private lateinit var sentInfo: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_PLAN_ID)
        val loaded = id?.let { CustomerPlanStore.load(this, it) }
        if (loaded == null) {
            Toast.makeText(this, "Plan not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        plan = loaded
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Edit plan"
        buildUi()
    }

    override fun onPause() {
        super.onPause()
        if (::plan.isInitialized && !deleted) collectAndSave()
    }

    // ---------------- UI ----------------

    private fun field(parent: LinearLayout, hint: String, value: String, multiline: Boolean = false, type: Int = InputType.TYPE_CLASS_TEXT): EditText {
        val til = TextInputLayout(this)
        til.hint = hint
        val et = TextInputEditText(til.context)
        et.setText(value)
        et.inputType = if (multiline) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        } else type
        til.addView(et)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(6)
        parent.addView(til, lp)
        return et
    }

    private fun heading(parent: LinearLayout, text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 13f
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setPadding(0, dp(18), 0, dp(6))
        parent.addView(tv)
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(14), dp(12), dp(14), dp(40))
        scroll.addView(root)

        // Letterhead preview
        val b = CrmPrefs.branding(this)
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val logoPath = b?.logoPath
        if (logoPath != null && File(logoPath).exists()) {
            val iv = ImageView(this)
            iv.setImageBitmap(BitmapFactory.decodeFile(logoPath))
            iv.adjustViewBounds = true
            head.addView(iv, LinearLayout.LayoutParams(dp(64), dp(64)).apply { marginEnd = dp(12) })
        }
        val headText = TextView(this)
        headText.text = if (b != null) {
            listOf(b.company, b.address, listOf(b.phone, b.email).filter { it.isNotBlank() }.joinToString(" · "), b.website)
                .filter { it.isNotBlank() }.joinToString("\n")
        } else {
            "No company letterhead yet.\nConnect to your CRM (menu → Company connection) to add your logo and details."
        }
        headText.textSize = 12.5f
        head.addView(headText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(head)

        sentInfo = TextView(this)
        sentInfo.setPadding(0, dp(10), 0, 0)
        sentInfo.textSize = 13f
        root.addView(sentInfo)
        refreshSentInfo()

        heading(root, "PLAN")
        titleIn = field(root, "Plan title", plan.title)

        heading(root, "CUSTOMER")
        val c = plan.customer
        nameIn = field(root, "Customer name", c.name, type = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        companyIn = field(root, "Company (optional)", c.company, type = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        addressIn = field(root, "Property address", c.address, multiline = true)
        phoneIn = field(root, "Phone", c.phone, type = InputType.TYPE_CLASS_PHONE)
        emailIn = field(root, "Email", c.email, type = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        jobIn = field(root, "Job number", c.jobNumber)

        heading(root, "SECTIONS — tap any text to edit")
        sectionsBox = LinearLayout(this)
        sectionsBox.orientation = LinearLayout.VERTICAL
        root.addView(sectionsBox)
        plan.sections.forEach { addSectionCard(it) }

        val add = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        add.text = "Add section"
        add.setOnClickListener {
            val s = PlanSection("New section", "")
            addSectionCard(s)
            sectionViews.lastOrNull()?.title?.requestFocus()
        }
        root.addView(add, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        val send = MaterialButton(this)
        send.text = "Send to customer or email…"
        send.setOnClickListener { showSendDialog() }
        root.addView(send, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })

        val export = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        export.text = "Create PDF & share"
        export.setOnClickListener { exportPdf(action = MENU_SHARE) }
        root.addView(export, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        setContentView(scroll)
    }

    private fun addSectionCard(s: PlanSection) {
        val card = MaterialCardView(this)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10)
        val inner = LinearLayout(this)
        inner.orientation = LinearLayout.VERTICAL
        inner.setPadding(dp(12), dp(8), dp(12), dp(4))
        card.addView(inner)

        val title = EditText(this)
        title.setText(s.title)
        title.setTypeface(title.typeface, Typeface.BOLD)
        title.textSize = 16f
        title.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        inner.addView(title)

        val body = EditText(this)
        body.setText(s.body)
        body.textSize = 14.5f
        body.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        body.setSingleLine(false)
        body.minLines = 2
        body.gravity = Gravity.TOP or Gravity.START
        body.hint = "Section text. A short line ending in \":\" becomes a bold sub-heading; lines starting with \"• \" become bullets."
        inner.addView(body)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.END
        val up = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        up.text = "Move up"
        val remove = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        remove.text = "Remove"
        row.addView(up)
        row.addView(remove)
        inner.addView(row)

        val views = SectionViews(card, title, body)
        sectionViews += views
        sectionsBox.addView(card, lp)

        up.setOnClickListener {
            val i = sectionViews.indexOf(views)
            if (i > 0) {
                sectionViews.removeAt(i)
                sectionViews.add(i - 1, views)
                sectionsBox.removeView(card)
                sectionsBox.addView(card, i - 1, lp)
            }
        }
        remove.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Remove \"${title.text.toString().ifBlank { "this section" }}\"?")
                .setPositiveButton("Remove") { _, _ ->
                    sectionViews.remove(views)
                    sectionsBox.removeView(card)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun collectAndSave() {
        plan.title = titleIn.text.toString().trim().ifBlank { "Weed Eradication Plan" }
        plan.customer.name = nameIn.text.toString().trim()
        plan.customer.company = companyIn.text.toString().trim()
        plan.customer.address = addressIn.text.toString().trim()
        plan.customer.phone = phoneIn.text.toString().trim()
        plan.customer.email = emailIn.text.toString().trim()
        plan.customer.jobNumber = jobIn.text.toString().trim()
        plan.sections.clear()
        sectionViews.forEach {
            val t = it.title.text.toString().trim()
            val b = it.body.text.toString().trim()
            if (t.isNotEmpty() || b.isNotEmpty()) plan.sections += PlanSection(t, b)
        }
        CustomerPlanStore.save(this, plan)
    }

    // ---------------- Export ----------------

    private fun buildPdf(): File? {
        collectAndSave()
        return try {
            PlanPdf.export(this, plan, CrmPrefs.branding(this))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't create the PDF: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun refreshSentInfo() {
        sentInfo.text = if (plan.sentLog.isEmpty()) "Not sent yet."
        else "Sent:\n" + plan.sentLog.takeLast(5).joinToString("\n") { "• $it" }
    }

    private fun defaultSubject(): String {
        val company = CrmPrefs.branding(this)?.company.orEmpty()
        val what = if (plan.mode == CustomerPlan.MODE_FOLLOWUP) "Your follow-up and regrowth prevention plan" else "Your weed eradication plan"
        return what + if (company.isNotBlank()) " from $company" else ""
    }

    private fun defaultMessage(): String {
        val company = CrmPrefs.branding(this)?.company.orEmpty()
        val first = plan.customer.name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "there" }
        val body = if (plan.mode == CustomerPlan.MODE_FOLLOWUP)
            "Thank you for choosing us. Now that your service is complete, we've put together a follow-up plan to help keep the weeds from coming back. It's attached as a PDF (${plan.planNumber})."
        else
            "Please find your weed eradication plan attached as a PDF (${plan.planNumber}). It explains what we found on your property and how we recommend dealing with it."
        return "Hello $first,\n\n$body\n\nIf you have any questions or would like to schedule a visit, just reply to this email.\n\nThank you,\n$company"
    }

    private fun validEmail(e: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()

    /** Send through the CRM (from the business) or fall back to the phone's email app. */
    private fun showSendDialog() {
        collectAndSave()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(8), dp(20), 0)

        val custEmail = plan.customer.email.trim()
        val toCustomer = CheckBox(this)
        if (custEmail.isNotBlank()) {
            toCustomer.text = "Customer: $custEmail"
            toCustomer.isChecked = true
        } else {
            toCustomer.text = "Customer has no email on file"
            toCustomer.isEnabled = false
        }
        box.addView(toCustomer)

        val others = EditText(this)
        others.hint = "Other email addresses (separate with commas)"
        others.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        box.addView(others)

        val businessEmail = CrmPrefs.branding(this)?.email.orEmpty()
        val copyMe = CheckBox(this)
        copyMe.text = "Send a copy to the business" + if (businessEmail.isNotBlank()) " ($businessEmail)" else ""
        if (businessEmail.isNotBlank()) box.addView(copyMe)

        val subject = EditText(this)
        subject.hint = "Subject"
        subject.setText(defaultSubject())
        box.addView(subject)

        val message = EditText(this)
        message.hint = "Message"
        message.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        message.minLines = 5
        message.gravity = Gravity.TOP or Gravity.START
        message.setText(defaultMessage())
        box.addView(message)

        val status = TextView(this)
        status.setPadding(0, dp(6), 0, 0)
        box.addView(status)

        val scroll = ScrollView(this)
        scroll.addView(box)
        val viaCrm = CrmPrefs.isConfigured(this)

        fun recipients(): List<String> {
            val list = mutableListOf<String>()
            if (toCustomer.isEnabled && toCustomer.isChecked) list += custEmail
            others.text.toString().split(',', ';', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }.forEach { list += it }
            return list.distinctBy { it.lowercase() }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Send plan")
            .setMessage(
                if (viaCrm) "The PDF is emailed from your business through the CRM and logged on the job."
                else "Not connected to the CRM, so this will open your phone's email app."
            )
            .setView(scroll)
            .setPositiveButton(if (viaCrm) "Send" else "Open email app", null)
            .setNeutralButton(if (viaCrm) "Use my email app" else "", null)
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
            val to = recipients()
            dialog.dismiss()
            emailFromPhone(to, subject.text.toString(), message.text.toString())
        }
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener { btn ->
            val to = recipients()
            val bad = to.filterNot { validEmail(it) }
            when {
                bad.isNotEmpty() -> { status.text = "Check these addresses: ${bad.joinToString()}"; return@setOnClickListener }
                to.isEmpty() && !(copyMe.isChecked && businessEmail.isNotBlank()) -> {
                    status.text = "Choose the customer or add at least one email address."
                    return@setOnClickListener
                }
            }
            if (!viaCrm) {
                dialog.dismiss()
                emailFromPhone(to, subject.text.toString(), message.text.toString())
                return@setOnClickListener
            }
            val pdf = buildPdf() ?: return@setOnClickListener
            btn.isEnabled = false
            status.text = "Sending…"
            val subj = subject.text.toString().trim()
            val msg = message.text.toString().trim()
            val copy = copyMe.isChecked && businessEmail.isNotBlank()
            Thread {
                val r = runCatching { CrmClient.sendPlan(this, plan.customer.jobId, to, copy, subj, msg, pdf) }
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    btn.isEnabled = true
                    r.onSuccess { sentTo ->
                        val stamp = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(Date())
                        plan.sentLog += "$stamp — ${sentTo.joinToString()}"
                        CustomerPlanStore.save(this, plan)
                        refreshSentInfo()
                        dialog.dismiss()
                        Toast.makeText(this, "Sent to ${sentTo.joinToString()}", Toast.LENGTH_LONG).show()
                    }.onFailure { e ->
                        status.text = (e.message ?: "Couldn't send.") + "\nYou can try again, or tap \"Use my email app\"."
                    }
                }
            }.start()
        }
    }

    private fun emailFromPhone(to: List<String>, subject: String, message: String) {
        val pdf = buildPdf() ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", pdf)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (to.isNotEmpty()) putExtra(Intent.EXTRA_EMAIL, to.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, "Send plan"))
            val stamp = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(Date())
            plan.sentLog += "$stamp — opened email app" + if (to.isNotEmpty()) " for ${to.joinToString()}" else ""
            CustomerPlanStore.save(this, plan)
            refreshSentInfo()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No email app found.", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportPdf(action: Int) {
        val file = buildPdf() ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = when (action) {
            MENU_PREVIEW -> Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            MENU_EMAIL -> Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (plan.customer.email.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(plan.customer.email))
                val company = CrmPrefs.branding(this@PlanEditorActivity)?.company.orEmpty()
                putExtra(Intent.EXTRA_SUBJECT, "Your weed eradication plan" + if (company.isNotBlank()) " from $company" else "")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hello ${plan.customer.name.ifBlank { "there" }},\n\nPlease find attached your weed eradication plan (${plan.planNumber}). " +
                        "Let us know if you have any questions.\n\nThank you,\n$company"
                )
            }
            else -> Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, plan.title)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            if (action == MENU_PREVIEW) startActivity(intent)
            else startActivity(Intent.createChooser(intent, if (action == MENU_EMAIL) "Email plan" else "Share plan"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open PDFs. Try Share instead.", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------- Menu ----------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_SEND, 0, "Send to customer or email…")
        menu.add(Menu.NONE, MENU_SAVE, 1, "Save")
        menu.add(Menu.NONE, MENU_PREVIEW, 2, "Preview PDF")
        menu.add(Menu.NONE, MENU_SHARE, 3, "Share PDF")
        menu.add(Menu.NONE, MENU_EMAIL, 4, "Email from my phone")
        menu.add(Menu.NONE, MENU_DELETE, 5, "Delete plan")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { finish(); return true }
            MENU_SAVE -> {
                collectAndSave()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                return true
            }
            MENU_SEND -> { showSendDialog(); return true }
            MENU_EMAIL -> { collectAndSave(); emailFromPhone(listOfNotNull(plan.customer.email.takeIf { it.isNotBlank() }), defaultSubject(), defaultMessage()); return true }
            MENU_PREVIEW, MENU_SHARE -> { exportPdf(item.itemId); return true }
            MENU_DELETE -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete this plan?")
                    .setMessage("This can't be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        deleted = true
                        CustomerPlanStore.delete(this, plan.id)
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
