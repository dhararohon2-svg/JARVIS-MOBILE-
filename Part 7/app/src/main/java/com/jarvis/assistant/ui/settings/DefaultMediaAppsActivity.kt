package com.jarvis.assistant.ui.settings

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.util.AppLauncher
import java.util.Locale
import kotlin.math.roundToInt

/** Theme-matched default media app picker with visible app icons and selected badges. */
class DefaultMediaAppsActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private lateinit var musicIcon: ImageView
    private lateinit var videoIcon: ImageView
    private lateinit var musicSummary: TextView
    private lateinit var videoSummary: TextView
    private lateinit var musicSelectedBadge: TextView
    private lateinit var videoSelectedBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 18, 35)
        window.navigationBarColor = Color.rgb(12, 18, 35)
        buildUi()
        refreshSummaries()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(Color.rgb(12, 18, 35))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        root.addView(text("JARVIS SETTINGS", 12, BLUE, true))
        root.addView(text("Default Media Apps", 27, Color.WHITE, true).apply {
            setPadding(0, dp(6), 0, dp(5))
        })
        root.addView(text("Choose the apps JARVIS should open for music and video requests. App icons and a selected indicator make your choices easy to recognize.", 14, MUTED, false).apply {
            setPadding(0, 0, 0, dp(18))
        })

        val musicCard = card()
        musicCard.addView(text("♫  MUSIC PLAYER", 13, BLUE, true))
        val musicRow = selectedAppRow().also { (icon, summary, badge) ->
            musicIcon = icon; musicSummary = summary; musicSelectedBadge = badge
        }
        musicCard.addView(musicRow)
        musicCard.addView(actionText("Choose music app  ›") { chooseApp("music") })
        musicCard.addView(actionText("Clear selection") {
            prefs.edit().remove(KEY_MUSIC).remove("${KEY_MUSIC}_label").apply()
            refreshSummaries()
            Toast.makeText(this, "Default music app cleared", Toast.LENGTH_SHORT).show()
        }.apply { setPadding(0, dp(12), 0, 0) })
        root.addView(musicCard)

        val videoCard = card()
        videoCard.addView(text("▶  VIDEO PLAYER", 13, BLUE, true))
        val videoRow = selectedAppRow().also { (icon, summary, badge) ->
            videoIcon = icon; videoSummary = summary; videoSelectedBadge = badge
        }
        videoCard.addView(videoRow)
        videoCard.addView(actionText("Choose video app  ›") { chooseApp("video") })
        videoCard.addView(actionText("Clear selection") {
            prefs.edit().remove(KEY_VIDEO).remove("${KEY_VIDEO}_label").apply()
            refreshSummaries()
            Toast.makeText(this, "Default video app cleared", Toast.LENGTH_SHORT).show()
        }.apply { setPadding(0, dp(12), 0, 0) })
        root.addView(videoCard)

        root.addView(text(
            "Voice examples: “Play music” opens your selected music app; “Play video” opens your selected video app. Automatic playback or search depends on the selected app's supported Android intents. Specific commands such as “Play this on YouTube” retain their existing behavior.",
            12, MUTED, false
        ).apply { setPadding(dp(2), dp(8), dp(2), 0) })
        setContentView(scroll)
    }

    private fun selectedAppRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.rgb(17, 25, 45), dp(14))
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.topMargin = dp(12); lp.bottomMargin = dp(10)
            layoutParams = lp
        }
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(android.R.drawable.ic_menu_help)
            contentDescription = "Selected app icon"
        }
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val summary = text("Not selected — tap Choose app", 15, Color.WHITE, true)
        val packageHint = text("Select an installed app", 11, MUTED, false).apply { tag = "packageHint" }
        details.addView(summary); details.addView(packageHint)
        val badge = TextView(this).apply {
            text = "✓ SELECTED"
            textSize = 10f
            setTextColor(Color.rgb(7, 24, 24))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = rounded(Color.rgb(70, 226, 184), dp(18))
            visibility = View.GONE
        }
        row.addView(icon); row.addView(details); row.addView(badge)
        return row
    }

    private fun chooseApp(category: String) {
        val apps = try {
            AppLauncher.getLaunchableApps(this)
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.ROOT) }
        } catch (_: Exception) { emptyList() }
        if (apps.isEmpty()) {
            Toast.makeText(this, "No launchable apps were found on this device", Toast.LENGTH_LONG).show()
            return
        }
        val key = if (category == "music") KEY_MUSIC else KEY_VIDEO
        val currentPackage = prefs.getString(key, null)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (category == "music") "Select default music app" else "Select default video app")
            .setView(ScrollView(this).apply { addView(list) })
            .setNegativeButton("Cancel", null)
            .create()

        apps.forEach { app ->
            val selected = app.packageName == currentPackage
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = rounded(if (selected) Color.rgb(24, 68, 75) else Color.rgb(25, 34, 57), dp(12))
                isClickable = true; isFocusable = true
                val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(6); layoutParams = lp
            }
            val appIcon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
                scaleType = ImageView.ScaleType.FIT_CENTER
                try { setImageDrawable(packageManager.getApplicationIcon(app.packageName)) }
                catch (_: Exception) { setImageResource(android.R.drawable.sym_def_app_icon) }
                contentDescription = "${app.label} icon"
            }
            val names = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            names.addView(text(app.label, 15, Color.WHITE, true))
            names.addView(text(app.packageName, 10, MUTED, false))
            val check = TextView(this).apply {
                text = if (selected) "✓ SELECTED" else "SELECT"
                textSize = 10f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (selected) Color.rgb(7, 24, 24) else BLUE)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(7), dp(8), dp(7))
                if (selected) background = rounded(Color.rgb(70, 226, 184), dp(18))
            }
            item.addView(appIcon); item.addView(names); item.addView(check)
            item.setOnClickListener {
                prefs.edit().putString(key, app.packageName).putString("${key}_label", app.label).apply()
                refreshSummaries()
                Toast.makeText(this, "${app.label} selected as default ${category} app", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            list.addView(item)
        }
        dialog.show()
    }

    private fun refreshSummaries() {
        if (!::musicSummary.isInitialized || !::videoSummary.isInitialized) return
        updateSummary(KEY_MUSIC, musicIcon, musicSummary, musicSelectedBadge)
        updateSummary(KEY_VIDEO, videoIcon, videoSummary, videoSelectedBadge)
    }

    private fun updateSummary(key: String, icon: ImageView, summary: TextView, badge: TextView) {
        val pkg = prefs.getString(key, null)
        val label = prefs.getString("${key}_label", null)
        if (pkg.isNullOrBlank() || label.isNullOrBlank()) {
            icon.setImageResource(android.R.drawable.ic_menu_help)
            summary.text = "Not selected — choose an app"
            badge.visibility = View.GONE
        } else {
            summary.text = label
            try { icon.setImageDrawable(packageManager.getApplicationIcon(pkg)) }
            catch (_: Exception) {
                icon.setImageResource(android.R.drawable.sym_def_app_icon)
                summary.text = "$label (not currently installed)"
            }
            badge.visibility = View.VISIBLE
        }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.rgb(25, 34, 57), dp(18))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
    }

    private fun actionText(value: String, action: () -> Unit) = text(value, 15, BLUE, true).apply {
        isClickable = true; isFocusable = true
        setPadding(0, dp(8), 0, dp(8))
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.START
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val PREFS = "jarvis_default_media_apps"
        const val KEY_MUSIC = "music_package"
        const val KEY_VIDEO = "video_package"
        private val BLUE = Color.rgb(100, 181, 246)
        private val MUTED = Color.rgb(177, 188, 210)
    }
}
