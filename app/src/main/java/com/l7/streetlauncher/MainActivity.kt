package com.l7.streetlauncher

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.AlarmClock
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var repository: AppRepository
    private lateinit var adapter: AppAdapter
    private val launcherApps by lazy { getSystemService(LauncherApps::class.java) }

    private lateinit var wallpaperImage: ImageView
    private lateinit var homeContent: LinearLayout
    private lateinit var brandTitle: TextView
    private lateinit var brandSubtitle: TextView
    private lateinit var clock: TextClock
    private lateinit var date: TextClock
    private lateinit var batteryLabel: TextView
    private lateinit var batteryBar: ProgressBar
    private lateinit var defaultBanner: TextView
    private lateinit var favoritesTitle: TextView
    private lateinit var favoritesEmpty: TextView
    private lateinit var dock: LinearLayout
    private lateinit var btnAndroid: TextView
    private lateinit var btnApps: TextView
    private lateinit var btnL7: TextView
    private lateinit var drawer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var appCount: TextView
    private lateinit var appGrid: RecyclerView
    private lateinit var gridLayoutManager: GridLayoutManager

    private var apps: List<AppInfo> = emptyList()
    private var appsLoaded = false
    private var loadGeneration = 0
    private var accent = Color.MAGENTA
    private var drawerOpen = false
    private var loadedWallpaperStamp = -1L

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!DefaultLauncher.isDefault(this)) {
            Toast.makeText(this, R.string.default_hint, Toast.LENGTH_LONG).show()
            DefaultLauncher.openHomeSettings(this)
        }
        updateDefaultBanner()
    }

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reloadApps()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reloadApps()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reloadApps()
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
            reloadApps()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
            reloadApps()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateBattery(intent)
    }

    // ------------------------------------------------------------------ ciclo de vida

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        repository = AppRepository(this)

        bindViews()
        homeContent.applySystemInsetsPadding()
        drawer.applySystemInsetsPadding(includeIme = true)
        setupDrawer()
        setupButtons()
        setupGestures()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Launcher: o "voltar" só fecha a gaveta; na tela inicial não faz nada.
                if (drawerOpen) closeDrawer()
            }
        })

        launcherApps.registerCallback(packageCallback, Handler(Looper.getMainLooper()))

        if (savedInstanceState?.getBoolean(KEY_DRAWER_OPEN) == true) openDrawer(animate = false)
        reloadApps()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED // ACTION_BATTERY_CHANGED é protegido: só o sistema envia
        )
    }

    override fun onResume() {
        super.onResume()
        applySettings()
        refreshDock()
        updateDefaultBanner()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // já removido
        }
        // Ao sair para outro app, volta para a tela inicial limpa.
        if (drawerOpen && !isChangingConfigurations) closeDrawer(animate = false)
    }

    override fun onDestroy() {
        launcherApps.unregisterCallback(packageCallback)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Botão/gesto "Início" com o launcher já aberto: fecha a gaveta.
        if (drawerOpen) closeDrawer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DRAWER_OPEN, drawerOpen)
    }

    // ------------------------------------------------------------------ montagem da tela

    private fun bindViews() {
        wallpaperImage = findViewById(R.id.wallpaperImage)
        homeContent = findViewById(R.id.homeContent)
        brandTitle = findViewById(R.id.brandTitle)
        brandSubtitle = findViewById(R.id.brandSubtitle)
        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        batteryLabel = findViewById(R.id.batteryLabel)
        batteryBar = findViewById(R.id.batteryBar)
        defaultBanner = findViewById(R.id.defaultBanner)
        favoritesTitle = findViewById(R.id.favoritesTitle)
        favoritesEmpty = findViewById(R.id.favoritesEmpty)
        dock = findViewById(R.id.dock)
        btnAndroid = findViewById(R.id.btnAndroid)
        btnApps = findViewById(R.id.btnApps)
        btnL7 = findViewById(R.id.btnL7)
        drawer = findViewById(R.id.drawer)
        searchInput = findViewById(R.id.searchInput)
        appCount = findViewById(R.id.appCount)
        appGrid = findViewById(R.id.appGrid)
    }

    private fun setupDrawer() {
        adapter = AppAdapter(
            onClick = { app, view -> launchApp(app, view) },
            onLongClick = { app, _ -> showAppMenu(app) }
        )
        gridLayoutManager = GridLayoutManager(this, computeColumns())
        appGrid.layoutManager = gridLayoutManager
        appGrid.adapter = adapter
        appGrid.setHasFixedSize(true)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString().orEmpty())
                appGrid.scrollToPosition(0)
            }
        })
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                adapter.visibleItems.firstOrNull()?.let { launchApp(it, searchInput) }
                true
            } else {
                false
            }
        }
        findViewById<ImageButton>(R.id.btnCloseDrawer).setOnClickListener { closeDrawer() }
    }

    private fun setupButtons() {
        btnApps.setOnClickListener { openDrawer() }
        btnL7.setOnClickListener { openL7Settings() }
        btnAndroid.setOnClickListener {
            if (!DefaultLauncher.openAndroidSettings(this)) toast(R.string.error_open)
        }
        defaultBanner.setOnClickListener { requestDefaultLauncher() }

        clock.setOnClickListener {
            tryStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        }
        date.setOnClickListener {
            tryStart(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val minDistance = NeonStyler.dp(this, 60f)
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val startY = e1?.y ?: return false
                val dy = e2.y - startY
                if (dy < -minDistance && abs(velocityY) > abs(velocityX)) {
                    openDrawer()
                    return true
                }
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                openL7Settings()
            }
        })
        homeContent.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
    }

    // ------------------------------------------------------------------ aparência

    private fun applySettings() {
        val neon = NeonPalette.get(prefs.neonColorIndex)
        accent = neon.color

        NeonStyler.glowText(brandTitle, neon.color, 18f)
        NeonStyler.glowText(brandSubtitle, neon.secondary, 10f)
        clock.setTextColor(Color.WHITE)
        clock.setShadowLayer(22f, 0f, 0f, neon.color)
        NeonStyler.glowText(date, neon.secondary, 10f)
        NeonStyler.glowText(favoritesTitle, neon.color, 10f)
        NeonStyler.glowText(appCount, neon.secondary, 8f)

        val timePattern = when {
            prefs.use24h && prefs.showSeconds -> "HH:mm:ss"
            prefs.use24h -> "HH:mm"
            prefs.showSeconds -> "h:mm:ss a"
            else -> "h:mm a"
        }
        clock.format12Hour = timePattern
        clock.format24Hour = timePattern
        val datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
        date.format12Hour = datePattern
        date.format24Hour = datePattern

        for (button in listOf(btnAndroid, btnApps, btnL7)) {
            button.background = NeonStyler.pill(this, neon.color)
            button.setShadowLayer(10f, 0f, 0f, neon.color)
        }
        btnApps.background = NeonStyler.pill(this, neon.secondary)
        btnApps.setShadowLayer(10f, 0f, 0f, neon.secondary)
        defaultBanner.background = NeonStyler.pill(this, neon.secondary)
        dock.background = NeonStyler.panel(this, neon.color, fillAlpha = 0x99, radiusDp = 22f)
        searchInput.background = NeonStyler.pill(this, neon.color)

        gridLayoutManager.spanCount = computeColumns()
        adapter.updateStyle(neon.color, prefs.neonIconFrame, prefs.showLabels)
        applyWallpaper()
    }

    private fun computeColumns(): Int {
        val widthDp = resources.configuration.screenWidthDp
        val auto = ((widthDp - 24) / 72).coerceIn(4, 10)
        val chosen = prefs.gridColumns
        return if (chosen > 0) max(chosen, if (widthDp > resources.configuration.screenHeightDp) auto else 0) else auto
    }

    private fun applyWallpaper() {
        when (prefs.wallpaperMode) {
            Prefs.WALL_SYSTEM -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                wallpaperImage.visibility = View.GONE
                loadedWallpaperStamp = -1L
            }
            Prefs.WALL_CUSTOM -> {
                showOwnWallpaper()
                val file = WallpaperStore.file(this)
                if (!file.exists()) {
                    wallpaperImage.setImageResource(R.drawable.bg_neon_city)
                    loadedWallpaperStamp = -1L
                    return
                }
                val stamp = file.lastModified()
                if (stamp == loadedWallpaperStamp) return
                loadedWallpaperStamp = stamp
                val metrics = resources.displayMetrics
                val maxSide = max(metrics.widthPixels, metrics.heightPixels)
                val appContext = applicationContext
                AppExecutors.io.execute {
                    val bitmap = WallpaperStore.load(appContext, maxSide)
                    AppExecutors.main.post {
                        if (isDestroyed) return@post
                        if (bitmap != null) {
                            wallpaperImage.setImageBitmap(bitmap)
                        } else {
                            wallpaperImage.setImageResource(R.drawable.bg_neon_city)
                        }
                    }
                }
            }
            else -> {
                showOwnWallpaper()
                if (loadedWallpaperStamp != 0L) {
                    wallpaperImage.setImageResource(R.drawable.bg_neon_city)
                    loadedWallpaperStamp = 0L
                }
            }
        }
    }

    private fun showOwnWallpaper() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        wallpaperImage.visibility = View.VISIBLE
    }

    private fun updateBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val percent = (level * 100 / scale).coerceIn(0, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        batteryLabel.text = getString(
            if (charging) R.string.battery_label_charging else R.string.battery_label,
            percent
        )
        batteryBar.progress = percent
        val barColor = when {
            percent > 50 -> 0xFF39FF14.toInt()
            percent > 20 -> 0xFFFFC400.toInt()
            else -> 0xFFFF3B3B.toInt()
        }
        batteryBar.progressTintList = ColorStateList.valueOf(barColor)
        batteryBar.progressBackgroundTintList = ColorStateList.valueOf(0x55FFFFFF)
    }

    private fun updateDefaultBanner() {
        defaultBanner.visibility = if (DefaultLauncher.isDefault(this)) View.GONE else View.VISIBLE
    }

    // ------------------------------------------------------------------ apps e favoritos

    private fun reloadApps() {
        val generation = ++loadGeneration
        AppExecutors.io.execute {
            val list = try {
                repository.loadApps()
            } catch (e: Exception) {
                emptyList()
            }
            AppExecutors.main.post {
                if (isDestroyed || generation != loadGeneration) return@post
                apps = list
                appsLoaded = true
                adapter.submit(list)
                appCount.text = getString(R.string.app_count, list.size)
                refreshDock()
            }
        }
    }

    private fun refreshDock() {
        dock.removeAllViews()
        val byKey = apps.associateBy { it.key }
        val stored = prefs.favorites
        if (appsLoaded && apps.isNotEmpty()) {
            // Remove favoritos de apps que foram desinstalados.
            val valid = stored.filter { byKey.containsKey(it) }
            if (valid.size != stored.size) prefs.favorites = valid
        }
        val favorites = stored.mapNotNull { byKey[it] }

        favoritesEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        dock.visibility = if (favorites.isEmpty()) View.GONE else View.VISIBLE

        for (app in favorites) {
            val item = layoutInflater.inflate(R.layout.item_app, dock, false)
            item.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            NeonStyler.bindApp(item, app, accent, prefs.neonIconFrame, prefs.showLabels)
            item.setOnClickListener { launchApp(app, it) }
            item.setOnLongClickListener {
                showAppMenu(app)
                true
            }
            dock.addView(item)
        }
    }

    private fun launchApp(app: AppInfo, source: View) {
        try {
            val location = IntArray(2)
            source.getLocationOnScreen(location)
            val bounds = Rect(
                location[0], location[1],
                location[0] + source.width, location[1] + source.height
            )
            val options = ActivityOptions.makeClipRevealAnimation(
                source, 0, 0, source.width, source.height
            ).toBundle()
            launcherApps.startMainActivity(app.component, app.user, bounds, options)
        } catch (e: Exception) {
            toast(R.string.error_launch)
        }
    }

    private fun showAppMenu(app: AppInfo) {
        val isFavorite = prefs.favorites.contains(app.key)
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        actions += getString(if (isFavorite) R.string.menu_unfavorite else R.string.menu_favorite) to {
            toggleFavorite(app)
        }
        actions += getString(R.string.menu_app_info) to { openAppInfo(app) }
        if (app.canUninstall) actions += getString(R.string.menu_uninstall) to { uninstall(app) }

        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .show()
    }

    private fun toggleFavorite(app: AppInfo) {
        val list = prefs.favorites.toMutableList()
        if (list.remove(app.key)) {
            toast(R.string.favorite_removed)
        } else {
            if (list.size >= Prefs.MAX_FAVORITES) {
                Toast.makeText(
                    this,
                    getString(R.string.favorites_full, Prefs.MAX_FAVORITES),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            list += app.key
            toast(R.string.favorite_added)
        }
        prefs.favorites = list
        refreshDock()
    }

    private fun openAppInfo(app: AppInfo) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            toast(R.string.error_open)
        }
    }

    private fun uninstall(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
        tryStart(intent)
    }

    // ------------------------------------------------------------------ gaveta

    private fun openDrawer(animate: Boolean = true) {
        if (drawerOpen) return
        drawerOpen = true
        drawer.visibility = View.VISIBLE
        drawer.animate().cancel()
        if (animate) {
            drawer.alpha = 0f
            drawer.translationY = NeonStyler.dp(this, 48f)
            drawer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            drawer.alpha = 1f
            drawer.translationY = 0f
        }
    }

    private fun closeDrawer(animate: Boolean = true) {
        if (!drawerOpen) return
        drawerOpen = false
        hideKeyboard()
        searchInput.setText("")
        drawer.animate().cancel()
        if (animate) {
            drawer.animate()
                .alpha(0f)
                .translationY(NeonStyler.dp(this, 48f))
                .setDuration(180)
                .withEndAction { if (!drawerOpen) drawer.visibility = View.GONE }
                .start()
        } else {
            drawer.visibility = View.GONE
        }
        appGrid.scrollToPosition(0)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
        searchInput.clearFocus()
    }

    // ------------------------------------------------------------------ utilidades

    private fun openL7Settings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun requestDefaultLauncher() {
        val intent = DefaultLauncher.roleRequestIntent(this)
        if (intent != null) {
            try {
                roleRequest.launch(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // segue para as configurações
            }
        }
        Toast.makeText(this, R.string.default_hint, Toast.LENGTH_LONG).show()
        DefaultLauncher.openHomeSettings(this)
    }

    private fun tryStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.error_open)
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val KEY_DRAWER_OPEN = "drawer_open"
    }
}
