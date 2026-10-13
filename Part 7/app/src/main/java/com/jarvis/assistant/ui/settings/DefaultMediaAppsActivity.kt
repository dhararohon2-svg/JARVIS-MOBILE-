package com.jarvis.assistant.ui.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.util.AppLauncher
import java.util.Locale
import kotlin.math.roundToInt

/** Lets users choose the apps JARVIS should open for generic music and video requests. */
class DefaultMediaAppsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private lateinit var musicSummary: TextView
    private lateinit var videoSummary: TextView

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
        root.addView(text("JARVIS SETTINGS", 12, Color.rgb(100, 181, 246), true))
        root.addView(text("Default Media Apps", 27, Color.WHITE, true).apply {
            setPadding(0, dp(6), 0, dp(5))
        })
        root.addView(text("Choose which installed app JARVIS opens when you ask it to play music or videos.", 14, Color.rgb(177, 188, 210), false).apply {
            setPadding(0, 0, 0, dp(18))
        })

        val musicCard = card()
        musicCard.addView(text("♫  MUSIC PLAYER", 13, Color.rgb(100, 181, 246), true))
        musicSummary = text("Not selected", 17, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(12)) }
        musicCard.addView(musicSummary)
        musicCard.addView(actionText("Choose music app  ›") { chooseApp("music") })
        musicCard.addView(actionText("Clear selection") {
            prefs.edit().remove(KEY_MUSIC).apply()
            refreshSummaries()
            Toast.makeText(this, "Default music app cleared", Toast.LENGTH_SHORT).show()
        }.apply { setPadding(0, dp(12), 0, 0) })
        root.addView(musicCard)

        val videoCard = card()
        videoCard.addView(text("▶  VIDEO PLAYER", 13, Color.rgb(100, 181, 246), true))
        videoSummary = text("Not selected", 17, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(12)) }
        videoCard.addView(videoSummary)
        videoCard.addView(actionText("Choose video app  ›") { chooseApp("video") })
        videoCard.addView(actionText("Clear selection") {
            prefs.edit().remove(KEY_VIDEO).apply()
            refreshSummaries()
            Toast.makeText(this, "Default video app cleared", Toast.LENGTH_SHORT).show()
        }.apply { setPadding(0, dp(12), 0, 0) })
        root.addView(videoCard)

        root.addView(text(
            "Voice examples: “Play music” opens your selected music app; “Open/play video” opens your selected video app. App-specific search and playback depend on whether that app supports Android search intents. Explicit commands such as “Play this on YouTube” keep their existing behavior.",
            12, Color.rgb(177, 188, 210), false
        ).apply { setPadding(dp(2), dp(8), dp(2), 0) })
        setContentView(scroll)
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
        val labels = apps.map { "${it.label}\n${it.packageName}" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(if (category == "music") "Select default music app" else "Select default video app")
            .setItems(labels) { dialog, which ->
                val selected = apps[which]
                val key = if (category == "music") KEY_MUSIC else KEY_VIDEO
                prefs.edit().putString(key, selected.packageName).putString("${key}_label", selected.label).apply()
                refreshSummaries()
                Toast.makeText(this, "${selected.label} set as default ${category} app", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshSummaries() {
        if (!::musicSummary.isInitialized || !::videoSummary.isInitialized) return
        musicSummary.text = prefs.getString("${KEY_MUSIC}_label", null) ?: "Not selected — tap to choose"
        videoSummary.text = prefs.getString("${KEY_VIDEO}_label", null) ?: "Not selected — tap to choose"
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        setBackgroundColor(Color.rgb(25, 34, 57))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
    }

    private fun actionText(value: String, action: () -> Unit) = text(value, 15, Color.rgb(100, 181, 246), true).apply {
        isClickable = true
        isFocusable = true
        setPadding(0, dp(4), 0, dp(4))
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        gravity = Gravity.START
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val PREFS = "jarvis_default_media_apps"
        const val KEY_MUSIC = "music_package"
        const val KEY_VIDEO = "video_package"
    }
}
