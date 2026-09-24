package com.plantscout.app

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout

/** Shared helpers for the plant-name fields. */
private object PlantNameField {

    fun attachSuggestions(activity: AppCompatActivity, input: AutoCompleteTextView) {
        input.setAdapter(ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, PlantCatalog.labels))
        input.threshold = 1
    }

    /** One-line description of what the typed name will become. */
    fun describe(text: String): String? {
        if (text.isBlank()) return null
        val hit = PlantCatalog.find(text)
        if (hit == null) {
            return "Not in PlantScout's list — it will still be added. AI customer plans research it fully; the basic plan gives general advice."
        }
        val c = PlantCatalog.toCandidate(text)
        val info = KnowledgeBase.lookup(c).info
        val hazards = info.hazards.joinToString("  ") { "⚠ ${it.label}" }
        return "✓ ${hit.common} · ${hit.scientific} · ${hit.family}\n${info.strategy.label}" +
            if (hazards.isNotEmpty()) "\n$hazards" else ""
    }
}

/** "Add a plant by name" from the main screen. Stays open so several plants can be added in a row. */
class AddPlantSheet(
    private val activity: AppCompatActivity,
    private val onAdded: (Candidate) -> Unit
) {
    private val dialog = BottomSheetDialog(activity)
    private val root: View = activity.layoutInflater.inflate(R.layout.sheet_add_plant, null)
    private val layout: TextInputLayout = root.findViewById(R.id.plantNameLayout)
    private val input: AutoCompleteTextView = root.findViewById(R.id.plantNameInput)
    private val matchInfo: TextView = root.findViewById(R.id.plantMatchInfo)
    private val addedInfo: TextView = root.findViewById(R.id.addedInfo)
    private val added = mutableListOf<String>()

    fun show() {
        dialog.setContentView(root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        PlantNameField.attachSuggestions(activity, input)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                layout.error = null
                val d = PlantNameField.describe(s?.toString().orEmpty())
                matchInfo.text = d.orEmpty()
                matchInfo.visibility = if (d == null) View.GONE else View.VISIBLE
            }
        })
        input.setOnItemClickListener { _, _, _, _ -> add() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                add()
                true
            } else false
        }
        root.findViewById<MaterialButton>(R.id.btnAddPlant).setOnClickListener { add() }
        root.findViewById<MaterialButton>(R.id.btnDoneAdding).setOnClickListener { dialog.dismiss() }

        dialog.show()
        input.requestFocus()
    }

    private fun add() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) {
            layout.error = "Type a plant name"
            return
        }
        val c = PlantCatalog.toCandidate(text)
        onAdded(c)
        added += c.displayName
        addedInfo.text = "Added: " + added.joinToString(", ") + "\nAdd another, or tap Done."
        addedInfo.visibility = View.VISIBLE
        input.setText("")
        input.dismissDropDown()
    }
}

/** The "Plants in this plan" step when creating a customer plan. */
class PlanPlantsSheet(
    private val activity: AppCompatActivity,
    customer: CrmCustomer,
    mode: String,
    private val useAi: Boolean,
    initial: List<Pair<String, Candidate?>>,
    private val onGenerate: (List<WeedEntry>, String) -> Unit
) {
    private val dialog = BottomSheetDialog(activity)
    private val root: View = activity.layoutInflater.inflate(R.layout.sheet_plan_plants, null)
    private val chips: ChipGroup = root.findViewById(R.id.planPlantChips)
    private val empty: TextView = root.findViewById(R.id.planPlantsEmpty)
    private val count: TextView = root.findViewById(R.id.planPlantsCount)
    private val status: TextView = root.findViewById(R.id.planPlantsStatus)
    private val addLayout: TextInputLayout = root.findViewById(R.id.planAddLayout)
    private val addInput: AutoCompleteTextView = root.findViewById(R.id.planAddInput)
    private val addInfo: TextView = root.findViewById(R.id.planAddInfo)
    private val notes: EditText = root.findViewById(R.id.planNotesInput)

    private val items = mutableListOf<Pair<String, Candidate?>>()

    init {
        val who = customer.name.ifBlank { "this customer" }
        val kind = if (mode == CustomerPlan.MODE_FOLLOWUP) "Follow-up plan" else "Eradication plan"
        root.findViewById<TextView>(R.id.planPlantsSubtitle).text = "$kind for $who"
        root.findViewById<MaterialButton>(R.id.btnGeneratePlan).text = if (useAi) "Generate" else "Create"
        if (!useAi) root.findViewById<View>(R.id.planNotesLayout).visibility = View.GONE
        initial.forEach { (label, c) -> addItem(label, c, quiet = true) }
        refresh()
    }

    private fun key(label: String, c: Candidate?) =
        (c?.scientificName?.takeIf { it.isNotBlank() } ?: label.substringBefore(" (")).lowercase().trim()

    private fun addItem(label: String, c: Candidate?, quiet: Boolean = false): Boolean {
        val k = key(label, c)
        if (items.any { key(it.first, it.second) == k }) {
            if (!quiet) addLayout.error = "Already in the plan"
            return false
        }
        val item = label to c
        items += item
        val chip = activity.layoutInflater.inflate(R.layout.chip_plant, chips, false) as Chip
        chip.text = label
        chip.setOnCloseIconClickListener {
            items.remove(item)
            chips.removeView(chip)
            refresh()
        }
        chips.addView(chip)
        if (!quiet) refresh()
        return true
    }

    private fun refresh() {
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        count.text = when (items.size) {
            0 -> "Plants"
            1 -> "1 plant"
            else -> "${items.size} plants"
        }
    }

    private fun addTyped() {
        val text = addInput.text.toString().trim()
        if (text.isEmpty()) return
        val c = PlantCatalog.toCandidate(text)
        if (addItem(PlantCatalog.displayLabel(c), c)) {
            addInput.setText("")
            addInput.dismissDropDown()
            addLayout.error = null
        }
    }

    fun show() {
        val height = (activity.resources.displayMetrics.heightPixels * 0.92).toInt()
        dialog.setContentView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height))
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        PlantNameField.attachSuggestions(activity, addInput)
        addInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                addLayout.error = null
                val d = PlantNameField.describe(s?.toString().orEmpty())
                addInfo.text = d.orEmpty()
                addInfo.visibility = if (d == null) View.GONE else View.VISIBLE
            }
        })
        addInput.setOnItemClickListener { _, _, _, _ -> addTyped() }
        addInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTyped()
                true
            } else false
        }
        addLayout.setEndIconOnClickListener { addTyped() }

        root.findViewById<MaterialButton>(R.id.btnGeneratePlan).setOnClickListener {
            if (addInput.text.toString().isNotBlank()) addTyped()
            status.visibility = View.GONE
            when {
                items.isEmpty() -> {
                    status.text = "Add at least one plant."
                    status.visibility = View.VISIBLE
                }
                items.size > 12 -> {
                    status.text = "Plans can include up to 12 plants. Remove ${items.size - 12}."
                    status.visibility = View.VISIBLE
                }
                else -> {
                    dialog.dismiss()
                    onGenerate(items.map { WeedEntry(it.first, it.second) }, notes.text.toString().trim())
                }
            }
        }
        dialog.show()
    }
}
