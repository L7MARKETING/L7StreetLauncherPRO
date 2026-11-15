package com.l7.streetlauncher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/** Paleta de cores neon disponíveis em Ajustes L7. */
object NeonPalette {

    class NeonColor(@StringRes val nameRes: Int, val color: Int, val secondary: Int)

    private val PINK = 0xFFFF2BD6.toInt()
    private val CYAN = 0xFF00F0FF.toInt()
    private val GREEN = 0xFF39FF14.toInt()
    private val ORANGE = 0xFFFF8A00.toInt()
    private val PURPLE = 0xFFB026FF.toInt()
    private val YELLOW = 0xFFFFE600.toInt()

    val colors: List<NeonColor> = listOf(
        NeonColor(R.string.color_pink, PINK, CYAN),
        NeonColor(R.string.color_cyan, CYAN, PINK),
        NeonColor(R.string.color_green, GREEN, YELLOW),
        NeonColor(R.string.color_orange, ORANGE, PINK),
        NeonColor(R.string.color_purple, PURPLE, CYAN),
        NeonColor(R.string.color_yellow, YELLOW, ORANGE)
    )

    fun get(index: Int): NeonColor = colors[index.coerceIn(colors.indices)]
}

/** Utilitários visuais: painéis com borda neon, texto com brilho e ícones. */
object NeonStyler {

    fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density

    fun panel(
        context: Context,
        accent: Int,
        fillAlpha: Int = 0xB3,
        radiusDp: Float = 18f,
        strokeDp: Float = 1.5f
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, radiusDp)
        setColor(Color.argb(fillAlpha, 10, 4, 24))
        setStroke(max(1, dp(context, strokeDp).toInt()), accent)
    }

    fun pill(context: Context, accent: Int): GradientDrawable =
        panel(context, accent, fillAlpha = 0xCC, radiusDp = 28f, strokeDp = 2f)

    fun glowText(view: TextView, color: Int, radius: Float = 14f) {
        view.setTextColor(color)
        view.setShadowLayer(radius, 0f, 0f, color)
    }

    fun bindApp(view: View, app: AppInfo, accent: Int, neonFrame: Boolean, showLabel: Boolean) {
        val frame = view.findViewById<View>(R.id.iconFrame)
        val icon = view.findViewById<ImageView>(R.id.appIcon)
        val label = view.findViewById<TextView>(R.id.appLabel)

        icon.setImageDrawable(app.icon)
        if (neonFrame) {
            frame.background = panel(view.context, accent, fillAlpha = 0xCC, radiusDp = 16f)
            val pad = dp(view.context, 7f).toInt()
            frame.setPadding(pad, pad, pad, pad)
        } else {
            frame.background = null
            frame.setPadding(0, 0, 0, 0)
        }

        label.text = app.label
        label.visibility = if (showLabel) View.VISIBLE else View.GONE
        label.setShadowLayer(6f, 0f, 0f, Color.BLACK)
        view.contentDescription = app.label
    }
}

/** Threads de apoio: trabalho pesado fora da tela, resultado de volta na thread principal. */
object AppExecutors {
    val io: ExecutorService = Executors.newFixedThreadPool(2)
    val main = Handler(Looper.getMainLooper())
}

/**
 * Soma as barras do sistema, recortes de câmera (notch/furo) e, opcionalmente, o teclado
 * ao padding original da view. Garante layout correto em Realme e outros aparelhos com tela cheia.
 */
fun View.applySystemInsetsPadding(includeIme: Boolean = false) {
    val left = paddingLeft
    val top = paddingTop
    val right = paddingRight
    val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = if (includeIme) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        v.setPadding(
            left + bars.left,
            top + bars.top,
            right + bars.right,
            bottom + max(bars.bottom, ime)
        )
        insets
    }
}
