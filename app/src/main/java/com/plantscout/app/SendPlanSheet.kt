package com.plantscout.app

import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import java.io.File

/** Bottom sheet for emailing the plan PDF through the CRM (or the phone's email app). */
class SendPlanSheet(
    private val activity: AppCompatActivity,
    private val plan: CustomerPlan,
    private val pdf: File,
    private val defaultSubject: String,
    private val defaultMessage: String,
    private val onPreview: () -> Unit,
    private val onSent: (List<String>) -> Unit,
    private val onUsePhone: (List<String>, String, String) -> Unit
) {
    private val dialog = BottomSheetDialog(activity)
    private val root: View = activity.layoutInflater.inflate(R.layout.sheet_send, null)

    private val form: View = root.findViewById(R.id.sendForm)
    private val done: View = root.findViewById(R.id.sendDone)
    private val doneText: TextView = root.findViewById(R.id.sendDoneText)
    private val chips: ChipGroup = root.findViewById(R.id.recipientGroup)
    private val noRecipients: TextView = root.findViewById(R.id.noRecipients)
    private val addCustomerChip: Chip = root.findViewById(R.id.chipAddCustomer)
    private val addLayout: TextInputLayout = root.findViewById(R.id.addEmailLayout)
    private val addInput: EditText = root.findViewById(R.id.addEmailInput)
    private val copySwitch: MaterialSwitch = root.findViewById(R.id.copySwitch)
    private val subject: EditText = root.findViewById(R.id.subjectInput)
    private val message: EditText = root.findViewById(R.id.messageInput)
    private val status: TextView = root.findViewById(R.id.sendStatus)
    private val progress: LinearProgressIndicator = root.findViewById(R.id.sendProgress)
    private val sendBtn: MaterialButton = root.findViewById(R.id.btnSend)
    private val phoneBtn: MaterialButton = root.findViewById(R.id.btnPhoneEmail)

    private val recipients = mutableListOf<String>()
    private val viaCrm = CrmPrefs.isConfigured(activity)
    private val branding = CrmPrefs.branding(activity)
    private val customerEmail = plan.customer.email.trim()
    private val businessEmail = branding?.email.orEmpty().trim()

    private fun valid(e: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()

    fun show() {
        // Fixed height: the header with the Send button stays on screen while the form scrolls.
        val height = (activity.resources.displayMetrics.heightPixels * 0.92).toInt()
        dialog.setContentView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height))
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        root.findViewById<TextView>(R.id.sendSubtitle).text =
            if (viaCrm) "Emailed from ${branding?.company?.ifBlank { null } ?: "your business"} through your CRM, and saved to the job's message history."
            else "You're not connected to your CRM, so this opens your phone's email app with the PDF attached."

        // Attachment card
        root.findViewById<TextView>(R.id.attachName).text = pdf.name
        root.findViewById<TextView>(R.id.attachInfo).text = attachmentInfo()
        root.findViewById<MaterialButton>(R.id.btnPreview).setOnClickListener { onPreview() }

        // Recipients
        if (valid(customerEmail)) addRecipient(customerEmail)
        addCustomerChip.setOnClickListener { addRecipient(customerEmail) }
        addLayout.setEndIconOnClickListener { addTyped() }
        addInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTyped()
                true
            } else false
        }
        refreshRecipients()

        if (valid(businessEmail)) {
            copySwitch.text = "Send a copy to the business ($businessEmail)"
        } else {
            copySwitch.visibility = View.GONE
        }

        subject.setText(defaultSubject)
        message.setText(defaultMessage)

        if (viaCrm) {
            sendBtn.text = "Send"
            phoneBtn.setOnClickListener {
                dialog.dismiss()
                onUsePhone(recipients.toList(), subject.text.toString(), message.text.toString())
            }
        } else {
            sendBtn.text = "Open email app"
            phoneBtn.visibility = View.GONE
        }
        sendBtn.setOnClickListener { send() }

        dialog.show()
    }

    private fun attachmentInfo(): String {
        val kb = (pdf.length() + 1023) / 1024
        val size = if (kb >= 1024) String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0) else "$kb KB"
        val pages = try {
            val renderer = PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY))
            val n = renderer.pageCount
            renderer.close() // also closes the file descriptor
            n
        } catch (e: Exception) {
            0
        }
        return listOfNotNull("PDF", if (pages > 0) (if (pages == 1) "1 page" else "$pages pages") else null, size).joinToString(" · ")
    }

    private fun addTyped() {
        val parts = addInput.text.toString().split(',', ';', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return
        val bad = parts.filterNot { valid(it) }
        parts.filter { valid(it) }.forEach { addRecipient(it) }
        if (bad.isEmpty()) {
            addInput.setText("")
            addLayout.error = null
        } else {
            addInput.setText(bad.joinToString(", "))
            addLayout.error = "Check this email address"
        }
    }

    private fun addRecipient(email: String) {
        if (recipients.any { it.equals(email, ignoreCase = true) }) return
        recipients += email
        val chip = activity.layoutInflater.inflate(R.layout.chip_email, chips, false) as Chip
        chip.text = email
        chip.setOnCloseIconClickListener {
            recipients.remove(email)
            chips.removeView(chip)
            refreshRecipients()
        }
        chips.addView(chip)
        refreshRecipients()
    }

    private fun refreshRecipients() {
        noRecipients.visibility = if (recipients.isEmpty()) View.VISIBLE else View.GONE
        val showAddCustomer = valid(customerEmail) && recipients.none { it.equals(customerEmail, ignoreCase = true) }
        addCustomerChip.visibility = if (showAddCustomer) View.VISIBLE else View.GONE
        if (showAddCustomer) addCustomerChip.text = "Add customer: $customerEmail"
    }

    private fun showError(msg: String) {
        status.text = msg
        status.visibility = View.VISIBLE
    }

    private fun send() {
        status.visibility = View.GONE
        // Include anything still typed in the "add email" box.
        if (addInput.text.toString().isNotBlank()) {
            addTyped()
            if (addLayout.error != null) return
        }
        val copy = copySwitch.visibility == View.VISIBLE && copySwitch.isChecked
        if (recipients.isEmpty() && !copy) {
            showError("Add at least one email address.")
            return
        }
        val subj = subject.text.toString().trim()
        val msg = message.text.toString().trim()
        if (!viaCrm) {
            dialog.dismiss()
            onUsePhone(recipients.toList(), subj, msg)
            return
        }

        setBusy(true)
        val to = recipients.toList()
        Thread {
            val r = runCatching { CrmClient.sendPlan(activity, plan.customer.jobId, to, copy, subj, msg, pdf) }
            activity.runOnUiThread {
                if (!dialog.isShowing) return@runOnUiThread
                setBusy(false)
                r.onSuccess { sentTo ->
                    form.visibility = View.GONE
                    sendBtn.visibility = View.GONE
                    done.visibility = View.VISIBLE
                    doneText.text = "Sent to ${sentTo.joinToString(", ")}." +
                        if (plan.customer.jobId > 0) "\nA copy is saved in the job's message history." else ""
                    onSent(sentTo)
                    Handler(Looper.getMainLooper()).postDelayed({ if (dialog.isShowing) dialog.dismiss() }, 2200)
                }.onFailure { e ->
                    showError((e.message ?: "Couldn't send.") + "\nTry again, or tap \"Use my email app\".")
                }
            }
        }.start()
    }

    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        sendBtn.isEnabled = !busy
        phoneBtn.isEnabled = !busy
        sendBtn.text = if (busy) "Sending…" else if (viaCrm) "Send" else "Open email app"
        dialog.setCancelable(!busy)
    }
}
