package com.plantscout.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val plants = mutableListOf<PlantRecord>()
    private val thumbCache = HashMap<String, Bitmap>()
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var adapter: PlantAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var status: TextView
    private lateinit var empty: TextView
    private lateinit var planButton: MaterialButton

    private var pendingOrgan = "auto"
    private var cameraFile: File? = null
    private var pendingJobs = 0
    private var pendingApk: File? = null
    private var updateCheckRunning = false

    private val organKeys = arrayOf("auto", "leaf", "flower", "fruit", "bark")
    private val organLabels = arrayOf("Let the app decide", "Leaf", "Flower", "Fruit / seeds", "Bark / stem")

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null && f.exists() && f.length() > 0) {
            identify(listOf(Uri.fromFile(f)), pendingOrgan, cleanup = f)
        }
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) identify(uris, pendingOrgan, cleanup = null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState?.getString("cameraFile")?.let { cameraFile = File(it) }
        savedInstanceState?.getString("organ")?.let { pendingOrgan = it }

        recycler = findViewById(R.id.list)
        progress = findViewById(R.id.progress)
        status = findViewById(R.id.status)
        empty = findViewById(R.id.empty)
        planButton = findViewById(R.id.btnPlan)

        plants.addAll(PlantStore.load(this))
        adapter = PlantAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btnCamera).setOnClickListener {
            chooseOrgan { launchCamera() }
        }
        findViewById<MaterialButton>(R.id.btnGallery).setOnClickListener {
            chooseOrgan { pickImages.launch("image/*") }
        }
        planButton.setOnClickListener {
            startActivity(Intent(this, PlanActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnAddManual).setOnClickListener {
            AddPlantSheet(this) { c -> addManualPlant(c) }.show()
        }

        updateUi()
        if (savedInstanceState == null) {
            if (Prefs.apiKey(this).isBlank()) showSettings(firstRun = true)
            checkForUpdates(manual = false)
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the "allow installs" settings screen
        val apk = pendingApk
        if (apk != null && packageManager.canRequestPackageInstalls()) {
            pendingApk = null
            installApk(apk)
        }
    }

    // ---------- Updates ----------

    private fun checkForUpdates(manual: Boolean) {
        if (updateCheckRunning) return
        updateCheckRunning = true
        if (manual) toast("Checking for updates…")
        Thread {
            val result = runCatching { UpdateChecker.fetchLatest() }
            runOnUiThread {
                updateCheckRunning = false
                if (isDestroyed) return@runOnUiThread
                val release = result.getOrNull()
                val current = UpdateChecker.currentVersionCode(this)
                when {
                    result.isFailure -> if (manual) toast("Couldn't check for updates. Check your internet connection.")
                    release == null -> if (manual) toast("No releases found on GitHub yet.")
                    release.versionCode > current -> showUpdateDialog(release)
                    else -> if (manual) toast("You're up to date (version ${UpdateChecker.currentVersionName(this)}).")
                }
            }
        }.start()
    }

    private fun showUpdateDialog(release: UpdateChecker.Release) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Update available")
            .setMessage("${release.name} is ready to install.\n\nYou have version ${UpdateChecker.currentVersionName(this)}. Your scans and settings will be kept.")
            .setPositiveButton("Update") { _, _ -> downloadUpdate(release) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadUpdate(release: UpdateChecker.Release) {
        val bar = LinearProgressIndicator(this)
        bar.isIndeterminate = true
        bar.max = 100
        val pad = (24 * resources.displayMetrics.density).toInt()
        val box = FrameLayout(this)
        box.setPadding(pad, pad / 2, pad, pad / 2)
        box.addView(bar)
        val dialog: AlertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Downloading update…")
            .setView(box)
            .setCancelable(false)
            .show()

        // Stored in the app's cache folder already shared through FileProvider
        val dest = File(File(cacheDir, "camera").apply { mkdirs() }, "PlantScout-update.apk")
        Thread {
            val result = runCatching {
                UpdateChecker.download(release.apkUrl, dest) { pct ->
                    runOnUiThread {
                        if (pct >= 0) {
                            if (bar.isIndeterminate) {
                                bar.visibility = View.INVISIBLE
                                bar.isIndeterminate = false
                                bar.visibility = View.VISIBLE
                            }
                            bar.setProgressCompat(pct, true)
                        }
                    }
                }
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                dialog.dismiss()
                result.onSuccess { installApk(dest) }
                    .onFailure {
                        toast("Download failed — opening it in your browser instead.")
                        openUrl(release.apkUrl)
                    }
            }
        }.start()
    }

    private fun installApk(apk: File) {
        if (!packageManager.canRequestPackageInstalls()) {
            pendingApk = apk
            MaterialAlertDialogBuilder(this)
                .setTitle("Allow updates")
                .setMessage("To install updates, Android needs your permission. On the next screen, turn on \"Allow from this source\", then come back.")
                .setPositiveButton("Open settings") { _, _ ->
                    try {
                        startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
                        )
                    } catch (e: ActivityNotFoundException) {
                        toast("Open Settings > Apps > PlantScout > Install unknown apps")
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> pendingApk = null }
                .show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apk)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            toast("Couldn't open the installer: ${e.message}")
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("Open this in your browser: $url")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cameraFile?.let { outState.putString("cameraFile", it.absolutePath) }
        outState.putString("organ", pendingOrgan)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    // ---------- Scanning ----------

    private fun chooseOrgan(then: () -> Unit) {
        if (Prefs.apiKey(this).isBlank()) {
            showSettings(firstRun = true)
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("What's mostly in the photo?")
            .setItems(organLabels) { _, which ->
                pendingOrgan = organKeys[which]
                then()
            }
            .show()
    }

    private fun launchCamera() {
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        val f = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        cameraFile = f
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            toast("No camera app found. Use \"From photos\" instead.")
        }
    }

    private fun identify(uris: List<Uri>, organ: String, cleanup: File?) {
        val key = Prefs.apiKey(this)
        if (key.isBlank()) {
            showSettings(firstRun = true)
            return
        }
        pendingJobs += uris.size
        updateUi()

        uris.forEachIndexed { index, uri ->
            executor.execute {
                val result = runCatching {
                    val bmp = ImageUtils.loadScaled(this, uri, 1280)
                    val jpeg = ImageUtils.toJpeg(bmp, 85)
                    bmp.recycle()
                    val candidates = PlantNetClient.identify(key, jpeg, organ)
                    val id = UUID.randomUUID().toString()
                    val file = File(PlantStore.imageDir(this), "$id.jpg")
                    file.writeBytes(jpeg)
                    PlantRecord(id, file.absolutePath, organ, candidates)
                }
                if (cleanup != null && index == uris.lastIndex) cleanup.delete()

                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    pendingJobs--
                    result.onSuccess { record ->
                        plants.add(0, record)
                        adapter.notifyItemInserted(0)
                        recycler.scrollToPosition(0)
                        PlantStore.save(this, plants)
                    }.onFailure { e ->
                        val msg = when (e) {
                            is PlantNetException -> e.message
                            is java.net.UnknownHostException -> "No internet connection."
                            is java.net.SocketTimeoutException -> "Pl@ntNet took too long to respond. Try again."
                            else -> e.message
                        } ?: "Identification failed."
                        val label = if (uris.size > 1) "Photo ${index + 1}: " else ""
                        toast(label + msg)
                    }
                    updateUi()
                }
            }
        }
    }

    /** A plant typed in by name: no photo, marked as added manually. */
    private fun addManualPlant(c: Candidate) {
        val record = PlantRecord(UUID.randomUUID().toString(), "", "manual", listOf(c))
        plants.add(0, record)
        adapter.notifyItemInserted(0)
        recycler.scrollToPosition(0)
        PlantStore.save(this, plants)
        updateUi()
    }

    // ---------- UI ----------

    private fun updateUi() {
        empty.visibility = if (plants.isEmpty() && pendingJobs == 0) View.VISIBLE else View.GONE
        if (pendingJobs > 0) {
            progress.visibility = View.VISIBLE
            status.visibility = View.VISIBLE
            status.text = if (pendingJobs == 1) "Identifying plant…" else "Identifying $pendingJobs plants…"
        } else {
            progress.visibility = View.GONE
            status.visibility = View.GONE
        }
        planButton.isEnabled = plants.isNotEmpty()
        planButton.text = if (plants.isEmpty()) "Generate eradication plan"
        else "Generate eradication plan (${plants.size})"
    }

    private fun reviewPlant(position: Int) {
        val p = plants.getOrNull(position) ?: return
        if (p.isManual) {
            val c = p.selected
            MaterialAlertDialogBuilder(this)
                .setTitle(c?.displayName ?: "Plant")
                .setMessage(
                    "Added manually" +
                        (c?.let { if (it.commonName != it.scientificName) "\nScientific name: ${it.scientificName}" else "" } ?: "") +
                        (c?.family?.takeIf { it.isNotBlank() }?.let { "\nFamily: $it" } ?: "")
                )
                .setNeutralButton("Remove") { _, _ ->
                    plants.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    PlantStore.save(this, plants)
                    updateUi()
                }
                .setPositiveButton("Close", null)
                .show()
            return
        }
        val labels = p.candidates.map {
            "${it.displayName}\n${it.scientificName} · ${(it.score * 100).roundToInt()}%"
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Pick the correct match")
            .setSingleChoiceItems(labels, p.selectedIndex) { dialog, which ->
                p.selectedIndex = which
                adapter.notifyItemChanged(position)
                PlantStore.save(this, plants)
                dialog.dismiss()
            }
            .setNeutralButton("Remove") { _, _ ->
                File(p.imagePath).delete()
                thumbCache.remove(p.imagePath)
                plants.removeAt(position)
                adapter.notifyItemRemoved(position)
                PlantStore.save(this, plants)
                updateUi()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSettings(firstRun: Boolean = false) {
        val input = EditText(this)
        input.setText(Prefs.apiKey(this))
        input.hint = "Paste your Pl@ntNet API key"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        input.setSingleLine(true)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this)
        container.setPadding(pad, pad / 2, pad, 0)
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle(if (firstRun) "Add your free Pl@ntNet key" else "Pl@ntNet API key")
            .setMessage("Plant identification uses the free Pl@ntNet service (500 IDs per day). Tap \"Get a key\", create an account, copy the API key from your account settings, then paste it here.")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                Prefs.setApiKey(this, input.text.toString().trim())
                toast("Key saved")
            }
            .setNeutralButton("Get a key") { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://my.plantnet.org/")))
                } catch (e: ActivityNotFoundException) {
                    toast("Open my.plantnet.org in your browser")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAll() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear all scans?")
            .setMessage("This removes every scanned plant and photo from the app.")
            .setPositiveButton("Clear") { _, _ ->
                plants.forEach { File(it.imagePath).delete() }
                plants.clear()
                thumbCache.clear()
                adapter.notifyDataSetChanged()
                PlantStore.save(this, plants)
                updateUi()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.add(Menu.NONE, MENU_CUSTOMER_PLANS, Menu.NONE, "Customer plans (letterhead + AI)")
        menu.add(Menu.NONE, MENU_CONNECTION, Menu.NONE, "Company connection")
        menu.add(Menu.NONE, MENU_UPDATE, Menu.NONE,
            "Check for updates (v${UpdateChecker.currentVersionName(this)})")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> { showSettings(); true }
        R.id.action_clear -> { clearAll(); true }
        MENU_UPDATE -> { checkForUpdates(manual = true); true }
        MENU_CUSTOMER_PLANS -> { startActivity(Intent(this, CustomerPlansActivity::class.java)); true }
        MENU_CONNECTION -> { CustomerPlansActivity.showConnectionDialog(this); true }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        private const val MENU_UPDATE = 9001
        private const val MENU_CUSTOMER_PLANS = 9002
        private const val MENU_CONNECTION = 9003
    }

    private fun themeColor(attr: Int): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    // ---------- List adapter ----------

    inner class PlantAdapter : RecyclerView.Adapter<PlantAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val thumb: ImageView = v.findViewById(R.id.thumb)
            val common: TextView = v.findViewById(R.id.common)
            val sci: TextView = v.findViewById(R.id.sci)
            val conf: TextView = v.findViewById(R.id.conf)
            val flags: TextView = v.findViewById(R.id.flags)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false))

        override fun getItemCount(): Int = plants.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = plants[position]
            val c = p.selected
            holder.common.text = c?.displayName ?: "Unknown plant"
            holder.sci.text = c?.scientificName ?: ""
            val pct = ((c?.score ?: 0.0) * 100).roundToInt()
            val others = if (p.candidates.size > 1) " · tap to see ${p.candidates.size - 1} other match(es)" else ""
            holder.conf.text = if (p.isManual) "Added manually" else "$pct% match · ${p.organ}$others"
            if (p.isManual && c != null && c.commonName.equals(c.scientificName, ignoreCase = true)) holder.sci.text = ""

            val flags = mutableListOf<String>()
            if (pct < 30 && !p.isManual) flags += "Low confidence – tap to review"
            if (c != null) KnowledgeBase.lookup(c).info.hazards.forEach { flags += "⚠ ${it.label}" }
            holder.flags.text = flags.joinToString("\n")
            holder.flags.visibility = if (flags.isEmpty()) View.GONE else View.VISIBLE

            if (p.imagePath.isBlank()) {
                // Manual plant: a leaf icon on a soft tinted circle-ish background.
                holder.thumb.scaleType = ImageView.ScaleType.CENTER
                holder.thumb.setImageResource(R.drawable.ic_grass)
                holder.thumb.setBackgroundResource(R.drawable.bg_avatar)
                holder.thumb.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor(com.google.android.material.R.attr.colorPrimaryContainer))
            } else {
                holder.thumb.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.thumb.background = null
                val bmp = thumbCache[p.imagePath]
                    ?: ImageUtils.thumbnail(p.imagePath, 200)?.also { thumbCache[p.imagePath] = it }
                holder.thumb.setImageBitmap(bmp)
            }

            holder.itemView.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) reviewPlant(pos)
            }
        }
    }
}
