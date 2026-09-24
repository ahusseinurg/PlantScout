package com.plantscout.app

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator

/** Full-height bottom sheet for choosing a weed-control customer from the CRM. */
class CustomerPickerSheet(
    private val activity: AppCompatActivity,
    startFilter: String,
    private val onPicked: (CrmCustomer) -> Unit,
    private val onManual: () -> Unit
) {
    private val dialog = BottomSheetDialog(activity)
    private val root: View = activity.layoutInflater.inflate(R.layout.sheet_customer_picker, null)
    private val search: EditText = root.findViewById(R.id.searchInput)
    private val progress: LinearProgressIndicator = root.findViewById(R.id.pickerProgress)
    private val info: TextView = root.findViewById(R.id.pickerInfo)
    private val list: RecyclerView = root.findViewById(R.id.pickerList)

    private val filterChips = linkedMapOf(
        "all" to R.id.chipAll,
        "completed" to R.id.chipDone,
        "active" to R.id.chipActive
    )
    private var filter = if (startFilter in filterChips) startFilter else "all"
    private var results: List<CrmCustomer> = emptyList()
    private var seq = 0
    private val handler = Handler(Looper.getMainLooper())
    private val searchRunnable = Runnable { load() }
    private val adapter = CustomerAdapter()

    fun show() {
        val height = (activity.resources.displayMetrics.heightPixels * 0.9).toInt()
        dialog.setContentView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height))
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = adapter

        for ((key, id) in filterChips) {
            val chip = root.findViewById<Chip>(id)
            chip.isChecked = key == filter
            chip.setOnClickListener {
                if (filter != key) {
                    filter = key
                    load()
                }
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacks(searchRunnable)
                handler.postDelayed(searchRunnable, 400)
            }
        })
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(searchRunnable)
                load()
                true
            } else false
        }

        root.findViewById<MaterialButton>(R.id.btnManual).setOnClickListener {
            dialog.dismiss()
            onManual()
        }
        dialog.setOnDismissListener { handler.removeCallbacks(searchRunnable) }
        dialog.show()
        load()
    }

    private fun load() {
        val mySeq = ++seq
        val q = search.text.toString().trim()
        val f = filter
        progress.visibility = View.VISIBLE
        Thread {
            val r = runCatching { CrmClient.customers(activity, q, f) }
            activity.runOnUiThread {
                if (mySeq != seq || !dialog.isShowing) return@runOnUiThread
                progress.visibility = View.INVISIBLE
                r.onSuccess { found ->
                    results = found
                    adapter.notifyDataSetChanged()
                    info.text = when {
                        found.isNotEmpty() -> if (found.size == 1) "1 customer" else "${found.size} customers"
                        q.isNotEmpty() -> "No matches for \"$q\""
                        f == "completed" -> "No completed weed-control jobs yet"
                        f == "active" -> "No active weed-control jobs"
                        else -> "No weed-control customers yet"
                    }
                }.onFailure { e ->
                    results = emptyList()
                    adapter.notifyDataSetChanged()
                    info.text = e.message ?: "Couldn't load customers."
                }
            }
        }.start()
    }

    // ---------------- Cards ----------------

    private fun themeColor(attr: Int): Int {
        val tv = TypedValue()
        activity.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    /** Container/on-container colour pairs from the app theme, picked per customer for variety. */
    private val avatarPalettes by lazy {
        listOf(
            themeColor(com.google.android.material.R.attr.colorPrimaryContainer) to themeColor(com.google.android.material.R.attr.colorOnPrimaryContainer),
            themeColor(com.google.android.material.R.attr.colorSecondaryContainer) to themeColor(com.google.android.material.R.attr.colorOnSecondaryContainer),
            themeColor(com.google.android.material.R.attr.colorTertiaryContainer) to themeColor(com.google.android.material.R.attr.colorOnTertiaryContainer)
        )
    }

    private fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    private fun trimNum(d: Double) = if (d == Math.floor(d)) d.toLong().toString() else String.format(java.util.Locale.US, "%.1f", d)

    private inner class CustomerAdapter : RecyclerView.Adapter<CustomerAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val avatar: TextView = v.findViewById(R.id.avatar)
            val name: TextView = v.findViewById(R.id.custName)
            val pill: TextView = v.findViewById(R.id.statusPill)
            val address: TextView = v.findViewById(R.id.custAddress)
            val meta: TextView = v.findViewById(R.id.custMeta)
            val weeds: TextView = v.findViewById(R.id.custWeeds)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false))

        override fun getItemCount(): Int = results.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = results[position]
            val displayName = c.name.ifBlank { c.email.ifBlank { "Unnamed customer" } }
            holder.name.text = displayName
            holder.avatar.text = initials(displayName)
            val (bg, fg) = avatarPalettes[Math.floorMod(displayName.hashCode(), avatarPalettes.size)]
            holder.avatar.backgroundTintList = ColorStateList.valueOf(bg)
            holder.avatar.setTextColor(fg)

            val ctx = holder.itemView.context
            when {
                c.completed -> {
                    holder.pill.visibility = View.VISIBLE
                    holder.pill.text = "✓ Done"
                    holder.pill.backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.pill_done_bg))
                    holder.pill.setTextColor(ctx.getColor(R.color.pill_done_text))
                }
                c.status.isNotBlank() -> {
                    holder.pill.visibility = View.VISIBLE
                    holder.pill.text = c.status.let { if (it.length > 16) it.take(15) + "…" else it }
                    holder.pill.backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.pill_active_bg))
                    holder.pill.setTextColor(ctx.getColor(R.color.pill_active_text))
                }
                else -> holder.pill.visibility = View.GONE
            }

            holder.address.text = c.address.ifBlank { c.email.ifBlank { c.phone.ifBlank { "No address on file" } } }

            val meta = mutableListOf<String>()
            if (c.jobNumber.isNotBlank()) meta += "#${c.jobNumber.removePrefix("#")}"
            if (c.acres > 0) meta += "${trimNum(c.acres)} ac"
            if (c.goatsNeeded > 0) meta += "${c.goatsNeeded} goats"
            if (c.completed && c.completedAt.isNotBlank()) meta += "done ${c.completedAt.take(10)}"
            holder.meta.text = meta.joinToString("  ·  ")
            holder.meta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE

            if (c.previousWeeds.isNotEmpty()) {
                holder.weeds.visibility = View.VISIBLE
                holder.weeds.text = c.previousWeeds.joinToString(", ") { it.substringBefore(" (") }
            } else {
                holder.weeds.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                val picked = results.getOrNull(pos) ?: return@setOnClickListener
                dialog.dismiss()
                onPicked(picked)
            }
        }
    }
}
