package com.l7.streetlauncher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Ajustes L7: papel de parede, cor neon, relógio, ícones e atalhos do sistema. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    private lateinit var wallPreview: ImageView
    private lateinit var wallGroup: RadioGroup
    private lateinit var colorRow: LinearLayout
    private lateinit var colorName: TextView
    private lateinit var defaultStatus: TextView
    private val sectionTitles = mutableListOf<TextView>()
    private val actionButtons = mutableListOf<TextView>()

    /** Evita que mudanças feitas pelo código disparem os listeners. */
    private var updatingUi = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importWallpaper(uri) else syncWallpaperRadio()
    }

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!DefaultLauncher.isDefault(this)) {
            Toast.makeText(this, R.string.default_hint, Toast.LENGTH_LONG).show()
            DefaultLauncher.openHomeSettings(this)
        }
        updateDefaultStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        // A barra superior trata do topo (status bar/notch); o conteúdo rolável trata da base.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                NeonStyler.dp(this, 8f).toInt() + bars.left,
                bars.top,
                NeonStyler.dp(this, 16f).toInt() + bars.right,
                0
            )
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsScroll)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            insets
        }

        wallPreview = findViewById(R.id.wallPreview)
        wallGroup = findViewById(R.id.wallGroup)
        colorRow = findViewById(R.id.colorRow)
        colorName = findViewById(R.id.colorName)
        defaultStatus = findViewById(R.id.defaultStatus)

        sectionTitles += listOf(
            findViewById<TextView>(R.id.titleWallpaper),
            findViewById<TextView>(R.id.titleColor),
            findViewById<TextView>(R.id.titleClock),
            findViewById<TextView>(R.id.titleIcons),
            findViewById<TextView>(R.id.titleSystem)
        )
        actionButtons += listOf(
            findViewById<TextView>(R.id.btnPickImage),
            findViewById<TextView>(R.id.btnSystemWallpaper),
            findViewById<TextView>(R.id.btnSetDefault),
            findViewById<TextView>(R.id.btnAndroidSettings)
        )

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        setupWallpaper()
        setupColors()
        setupSwitches()
        setupColumns()
        setupSystem()

        findViewById<TextView>(R.id.versionLabel).text =
            getString(R.string.version_label, BuildConfig.VERSION_NAME)

        applyAccent()
    }

    override fun onResume() {
        super.onResume()
        updateDefaultStatus()
    }

    // ------------------------------------------------------------------ papel de parede

    private fun setupWallpaper() {
        syncWallpaperRadio()
        refreshPreview()

        wallGroup.setOnCheckedChangeListener { _, checkedId ->
            if (updatingUi) return@setOnCheckedChangeListener
            when (checkedId) {
                R.id.wallNeon -> prefs.wallpaperMode = Prefs.WALL_NEON
                R.id.wallSystem -> prefs.wallpaperMode = Prefs.WALL_SYSTEM
                R.id.wallCustom -> {
                    if (WallpaperStore.hasCustom(this)) {
                        prefs.wallpaperMode = Prefs.WALL_CUSTOM
                    } else {
                        launchPicker()
                    }
                }
            }
            refreshPreview()
        }

        findViewById<TextView>(R.id.btnPickImage).setOnClickListener { launchPicker() }
        findViewById<TextView>(R.id.btnSystemWallpaper).setOnClickListener {
            try {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SET_WALLPAPER),
                        getString(R.string.wall_system_pick)
                    )
                )
                prefs.wallpaperMode = Prefs.WALL_SYSTEM
                syncWallpaperRadio()
                refreshPreview()
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, R.string.error_open, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchPicker() {
        try {
            pickImage.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_open, Toast.LENGTH_SHORT).show()
            syncWallpaperRadio()
        }
    }

    private fun importWallpaper(uri: Uri) {
        Toast.makeText(this, R.string.wall_importing, Toast.LENGTH_SHORT).show()
        val appContext = applicationContext
        AppExecutors.io.execute {
            val ok = WallpaperStore.importImage(appContext, uri)
            AppExecutors.main.post {
                if (isDestroyed) return@post
                if (ok) {
                    prefs.wallpaperMode = Prefs.WALL_CUSTOM
                    Toast.makeText(this, R.string.wall_imported, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.wall_error, Toast.LENGTH_LONG).show()
                }
                syncWallpaperRadio()
                refreshPreview()
            }
        }
    }

    private fun syncWallpaperRadio() {
        updatingUi = true
        wallGroup.check(
            when (prefs.wallpaperMode) {
                Prefs.WALL_CUSTOM -> R.id.wallCustom
                Prefs.WALL_SYSTEM -> R.id.wallSystem
                else -> R.id.wallNeon
            }
        )
        updatingUi = false
    }

    private fun refreshPreview() {
        when (prefs.wallpaperMode) {
            Prefs.WALL_CUSTOM -> {
                val appContext = applicationContext
                val side = NeonStyler.dp(this, 480f).toInt()
                AppExecutors.io.execute {
                    val bitmap = WallpaperStore.load(appContext, side)
                    AppExecutors.main.post {
                        if (isDestroyed) return@post
                        if (bitmap != null) wallPreview.setImageBitmap(bitmap)
                        else wallPreview.setImageResource(R.drawable.bg_neon_city)
                    }
                }
            }
            Prefs.WALL_SYSTEM -> {
                wallPreview.setImageDrawable(null)
                wallPreview.setBackgroundColor(Color.parseColor("#1A0836"))
            }
            else -> wallPreview.setImageResource(R.drawable.bg_neon_city)
        }
    }

    // ------------------------------------------------------------------ cores

    private fun setupColors() {
        colorRow.removeAllViews()
        val size = NeonStyler.dp(this, 40f).toInt()
        val margin = NeonStyler.dp(this, 10f).toInt()
        NeonPalette.colors.forEachIndexed { index, neon ->
            val swatch = View(this)
            swatch.layoutParams = LinearLayout.LayoutParams(size, size).apply { rightMargin = margin }
            swatch.contentDescription = getString(neon.nameRes)
            swatch.isClickable = true
            swatch.isFocusable = true
            swatch.setOnClickListener {
                prefs.neonColorIndex = index
                applyAccent()
            }
            colorRow.addView(swatch)
        }
    }

    private fun applyAccent() {
        val selected = prefs.neonColorIndex.coerceIn(NeonPalette.colors.indices)
        val neon = NeonPalette.get(selected)

        for (i in 0 until colorRow.childCount) {
            val color = NeonPalette.colors[i].color
            colorRow.getChildAt(i).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                if (i == selected) setStroke(NeonStyler.dp(this@SettingsActivity, 3f).toInt(), Color.WHITE)
            }
        }
        colorName.text = getString(neon.nameRes)

        val title = findViewById<TextView>(R.id.settingsTitle)
        NeonStyler.glowText(title, neon.color, 14f)
        sectionTitles.forEach { NeonStyler.glowText(it, neon.color, 8f) }
        actionButtons.forEach { it.background = NeonStyler.pill(this, neon.color) }

        val tint = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(neon.color, Color.parseColor("#80FFFFFF"))
        )
        listOf(R.id.wallNeon, R.id.wallCustom, R.id.wallSystem, R.id.colAuto, R.id.col4, R.id.col5, R.id.col6)
            .forEach { findViewById<RadioButton>(it).buttonTintList = tint }
        listOf(R.id.sw24h, R.id.swSeconds, R.id.swNeonFrame, R.id.swLabels).forEach {
            findViewById<SwitchCompat>(it).thumbTintList = tint
        }
    }

    // ------------------------------------------------------------------ relógio e ícones

    private fun setupSwitches() {
        bindSwitch(R.id.sw24h, prefs.use24h) { prefs.use24h = it }
        bindSwitch(R.id.swSeconds, prefs.showSeconds) { prefs.showSeconds = it }
        bindSwitch(R.id.swNeonFrame, prefs.neonIconFrame) { prefs.neonIconFrame = it }
        bindSwitch(R.id.swLabels, prefs.showLabels) { prefs.showLabels = it }
    }

    private fun bindSwitch(id: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        val switch = findViewById<SwitchCompat>(id)
        switch.isChecked = initial
        switch.setOnCheckedChangeListener { _, checked -> onChange(checked) }
    }

    private fun setupColumns() {
        val group = findViewById<RadioGroup>(R.id.columnsGroup)
        group.check(
            when (prefs.gridColumns) {
                4 -> R.id.col4
                5 -> R.id.col5
                6 -> R.id.col6
                else -> R.id.colAuto
            }
        )
        group.setOnCheckedChangeListener { _, checkedId ->
            prefs.gridColumns = when (checkedId) {
                R.id.col4 -> 4
                R.id.col5 -> 5
                R.id.col6 -> 6
                else -> 0
            }
        }
    }

    // ------------------------------------------------------------------ sistema

    private fun setupSystem() {
        findViewById<TextView>(R.id.btnSetDefault).setOnClickListener {
            val intent = DefaultLauncher.roleRequestIntent(this)
            if (intent != null) {
                try {
                    roleRequest.launch(intent)
                    return@setOnClickListener
                } catch (e: ActivityNotFoundException) {
                    // segue para as configurações
                }
            }
            Toast.makeText(this, R.string.default_hint, Toast.LENGTH_LONG).show()
            DefaultLauncher.openHomeSettings(this)
        }
        findViewById<TextView>(R.id.btnAndroidSettings).setOnClickListener {
            if (!DefaultLauncher.openAndroidSettings(this)) {
                Toast.makeText(this, R.string.error_open, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDefaultStatus() {
        defaultStatus.setText(
            if (DefaultLauncher.isDefault(this)) R.string.default_yes else R.string.default_no
        )
    }
}
