package com.plantscout.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

class CustomerPlansActivity : AppCompatActivity() {

    companion object {
        private const val MENU_CONNECTION = 1
        private const val MENU_REFRESH_LETTERHEAD = 2

        /** Shared connection dialog (also used from the main screen). */
        fun showConnectionDialog(activity: AppCompatActivity, onConnected: (() -> Unit)? = null) {
            val pad = (20 * activity.resources.displayMetrics.density).toInt()
            val box = LinearLayout(activity)
            box.orientation = LinearLayout.VERTICAL
            box.setPadding(pad, pad / 2, pad, 0)
            val urlIn = EditText(activity)
            urlIn.hint = "Website, e.g. utahgoats.org"
            urlIn.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            urlIn.setText(CrmPrefs.url(activity))
            val tokenIn = EditText(activity)
            tokenIn.hint = "App token (starts with ps_)"
            tokenIn.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            tokenIn.setText(CrmPrefs.token(activity))
            val status = TextView(activity)
            status.setPadding(0, pad / 2, 0, 0)
            box.addView(urlIn)
            box.addView(tokenIn)
            box.addView(status)

            val builder = MaterialAlertDialogBuilder(activity)
                .setTitle("Company connection")
                .setMessage("Connects PlantScout to your CRM for customers, your letterhead and Gemini AI plans. In WordPress, install the PlantScout Connector plugin, then go to Settings → PlantScout App to create a token.")
                .setView(box)
                .setPositiveButton("Test & save", null)
                .setNegativeButton("Cancel", null)
            if (CrmPrefs.isConfigured(activity)) {
                builder.setNeutralButton("Disconnect") { _, _ ->
                    CrmPrefs.clear(activity)
                    Toast.makeText(activity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
            }
            val dialog = builder.show()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener { btn ->
                val url = CrmClient.normalizeUrl(urlIn.text.toString())
                val token = tokenIn.text.toString().trim()
                if (url.isBlank() || token.isBlank()) {
                    status.text = "Enter both the website and the token."
                    return@setOnClickListener
                }
                btn.isEnabled = false
                status.text = "Connecting…"
                Thread {
                    val result = runCatching {
                        val ping = CrmClient.ping(url, token)
                        CrmPrefs.save(activity, url, token)
                        CrmClient.refreshBranding(activity)
                        ping
                    }
                    activity.runOnUiThread {
                        btn.isEnabled = true
                        result.onSuccess { ping ->
                            val ai = if (ping.optBoolean("uses_gemini")) "Google Gemini is ready."
                            else if ((ping.optJSONArray("ai_providers")?.length() ?: 0) > 0) "An AI provider is ready (Gemini isn't enabled, so another one will be used)."
                            else "No AI provider is enabled yet — add your Gemini key in the ERP under Weed AI Recommendations."
                            Toast.makeText(activity, "Connected to ${ping.optString("company")}. $ai", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                            onConnected?.invoke()
                        }.onFailure { e ->
                            status.text = e.message ?: "Couldn't connect."
                        }
                    }
                }.start()
            }
        }
    }

    private lateinit var list: ListView
    private lateinit var empty: TextView
    private var plans: List<CustomerPlan> = emptyList()
    @Volatile private var cancelled = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Customer plans"

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        val newBtn = MaterialButton(this)
        newBtn.text = "New customer plan"
        newBtn.setOnClickListener { startNewPlan() }
        root.addView(newBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(12), dp(12), dp(12), dp(4))
        })
        val followBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        followBtn.text = "Follow-up plan for a completed job"
        followBtn.setOnClickListener { startNewPlan(startFilter = "completed") }
        root.addView(followBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(12), 0, dp(12), dp(4))
        })
        empty = TextView(this)
        empty.text = "No customer plans yet.\n\nScan the plants on a property, then tap \"New customer plan\" to pick the customer from your CRM and generate a full plan with Gemini. You can edit everything before creating the PDF."
        empty.setPadding(dp(24), dp(24), dp(24), dp(24))
        empty.textSize = 15f
        root.addView(empty)
        list = ListView(this)
        root.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        list.setOnItemClickListener { _, _, pos, _ -> openPlan(plans[pos].id) }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            val p = plans[pos]
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete plan for ${p.customer.name.ifBlank { "this customer" }}?")
                .setPositiveButton("Delete") { _, _ ->
                    CustomerPlanStore.delete(this, p.id)
                    refreshList()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        plans = CustomerPlanStore.list(this)
        val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val rows = plans.map {
            val sent = it.sentLog.lastOrNull()?.let { last -> "\nSent: $last" } ?: "\nNot sent yet"
            "${it.customer.name.ifBlank { "Customer" }} — ${it.title}\n${it.planNumber} · edited ${df.format(Date(it.updatedAt))}$sent"
        }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        empty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openPlan(id: String) {
        startActivity(Intent(this, PlanEditorActivity::class.java).putExtra(PlanEditorActivity.EXTRA_PLAN_ID, id))
    }

    // ---------------- New plan flow ----------------

    private fun startNewPlan(startFilter: String = "all") {
        if (!CrmPrefs.isConfigured(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Connect to your CRM?")
                .setMessage("Connecting lets you pick the customer from your weed-control jobs, adds your logo and letterhead, and uses Gemini to write the plan.\n\nWithout it, you can still type the customer's details and use PlantScout's built-in guidance.")
                .setPositiveButton("Connect") { _, _ -> showConnectionDialog(this) { startNewPlan(startFilter) } }
                .setNeutralButton("Continue without") { _, _ ->
                    manualCustomer { c -> askPlanType(c, startFilter == "completed") { mode -> askWeeds(c, useAi = false, mode = mode) } }
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        CustomerPickerSheet(
            this,
            startFilter,
            onPicked = { c ->
                if (c.completed) {
                    askPlanType(c, true) { mode -> askWeeds(c, useAi = true, mode = mode) }
                } else {
                    askWeeds(c, useAi = true, mode = CustomerPlan.MODE_ERADICATION)
                }
            },
            onManual = {
                manualCustomer { c -> askPlanType(c, startFilter == "completed") { mode -> askWeeds(c, useAi = true, mode = mode) } }
            }
        ).show()
    }

    private fun askPlanType(c: CrmCustomer, jobDone: Boolean, onChosen: (String) -> Unit) {
        val options = arrayOf<CharSequence>(
            "After-service follow-up plan" + if (jobDone) " (recommended — job is done)" else "",
            "Full eradication plan"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("What kind of plan for ${c.name.ifBlank { "this customer" }}?")
            .setItems(options) { _, which ->
                onChosen(if (which == 0) CustomerPlan.MODE_FOLLOWUP else CustomerPlan.MODE_ERADICATION)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun manualCustomer(onPicked: (CrmCustomer) -> Unit) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(8), dp(20), 0)
        fun input(hint: String, type: Int): EditText {
            val e = EditText(this)
            e.hint = hint
            e.inputType = type
            box.addView(e)
            return e
        }
        val name = input("Customer name", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val address = input("Property address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val phone = input("Phone", InputType.TYPE_CLASS_PHONE)
        val email = input("Email", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val acres = input("Acres (optional)", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        MaterialAlertDialogBuilder(this)
            .setTitle("Customer details")
            .setView(box)
            .setPositiveButton("Next") { _, _ ->
                onPicked(
                    CrmCustomer(
                        name = name.text.toString().trim(),
                        address = address.text.toString().trim(),
                        phone = phone.text.toString().trim(),
                        email = email.text.toString().trim(),
                        acres = acres.text.toString().toDoubleOrNull() ?: 0.0
                    )
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** "Plants in this plan": scanned + manual plants + this job's earlier research, all editable. */
    private fun askWeeds(customer: CrmCustomer, useAi: Boolean, mode: String) {
        val scans = PlantStore.load(this)
        val initial = mutableListOf<Pair<String, Candidate?>>()
        scans.mapNotNull { it.selected }
            .groupBy { it.scientificName.lowercase() }
            .map { (_, cs) -> cs.maxByOrNull { it.score } ?: cs.first() }
            .forEach { c -> initial += PlantCatalog.displayLabel(c) to c }
        customer.previousWeeds.forEach { name ->
            val hit = PlantCatalog.find(name)
            initial += name to (if (hit != null) PlantCatalog.toCandidate(name) else null)
        }
        PlanPlantsSheet(this, customer, mode, useAi, initial) { weeds, notes ->
            generate(customer, weeds, notes, useAi, scans, mode)
        }.show()
    }

    private fun generate(customer: CrmCustomer, weeds: List<WeedEntry>, notes: String, useAi: Boolean, scans: List<PlantRecord>, mode: String) {
        cancelled = false
        val msg = TextView(this)
        msg.setPadding(dp(24), dp(16), dp(24), dp(8))
        msg.textSize = 15f
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        bar.max = weeds.size + 1
        bar.setPadding(dp(24), 0, dp(24), dp(16))
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.addView(msg)
        box.addView(bar)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (useAi) "Writing the plan with AI" else "Creating the plan")
            .setView(box)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> cancelled = true }
            .show()

        fun status(text: String, step: Int) = runOnUiThread {
            msg.text = text
            bar.progress = step
        }

        Thread {
            var overview: org.json.JSONObject? = null
            var overviewError: String? = null
            if (useAi) {
                // Refresh the letterhead (ignore failures; a cached copy is fine).
                runCatching { CrmClient.refreshBranding(this) }
                weeds.forEachIndexed { i, w ->
                    if (cancelled) return@forEachIndexed
                    status("Researching ${w.label}…\n(${i + 1} of ${weeds.size}. Each weed can take a minute.)", i)
                    try {
                        w.ai = CrmClient.recommendation(this, customer.jobId, w.label)
                    } catch (e: Exception) {
                        w.error = e.message
                    }
                }
                if (!cancelled) {
                    status("Writing the overall site plan…", weeds.size)
                    try {
                        overview = CrmClient.overview(this, customer.jobId, CustomerPlanBuilder.overviewInput(weeds), notes, mode)
                    } catch (e: Exception) {
                        overviewError = e.message
                    }
                }
            }
            // Only list scans for the weeds the user kept in the plan.
            val kept = weeds.mapNotNull { it.candidate?.scientificName?.lowercase() }.toSet()
            val keptScans = scans.filter { it.selected?.scientificName?.lowercase() in kept }
            val plan = CustomerPlanBuilder.build(customer, weeds, overview, overviewError, keptScans, mode)
            if (notes.isNotBlank()) plan.sections.add(1, PlanSection("Site notes", notes))
            val allFailed = useAi && weeds.all { it.ai == null } && overview == null
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                dialog.dismiss()
                if (cancelled) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                CustomerPlanStore.save(this, plan)
                if (allFailed) {
                    val why = weeds.firstNotNullOfOrNull { it.error } ?: overviewError ?: "Unknown error"
                    MaterialAlertDialogBuilder(this)
                        .setTitle("AI wasn't available")
                        .setMessage("$why\n\nThe plan was created with PlantScout's built-in guidance instead. You can edit it, or fix the problem and create a new one.")
                        .setPositiveButton("Open plan") { _, _ -> openPlan(plan.id) }
                        .show()
                } else {
                    val failed = weeds.count { it.ai == null }
                    if (useAi && failed > 0) {
                        Toast.makeText(this, "$failed weed(s) used built-in guidance because AI research failed.", Toast.LENGTH_LONG).show()
                    }
                    openPlan(plan.id)
                }
            }
        }.start()
    }

    // ---------------- Menu ----------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_CONNECTION, 1, "Company connection")
        menu.add(Menu.NONE, MENU_REFRESH_LETTERHEAD, 2, "Refresh letterhead & logo")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { finish(); return true }
            MENU_CONNECTION -> { showConnectionDialog(this); return true }
            MENU_REFRESH_LETTERHEAD -> {
                if (!CrmPrefs.isConfigured(this)) {
                    showConnectionDialog(this)
                    return true
                }
                Thread {
                    val r = runCatching { CrmClient.refreshBranding(this) }
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            r.fold({ "Letterhead updated: ${it.company}" }, { it.message ?: "Couldn't update" }),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.start()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
