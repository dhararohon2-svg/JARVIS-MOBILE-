package com.jarvis.assistant.ui.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.app.ActivityManager
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import java.util.Locale
import kotlin.math.roundToInt

/** JARVIS device dashboard: battery, battery temperature, storage and RAM. */
class DeviceMonitorActivity : AppCompatActivity() {
    private lateinit var batteryValue: TextView
    private lateinit var batteryTempValue: TextView
    private lateinit var storageValue: TextView
    private lateinit var ramValue: TextView
    private lateinit var batteryBar: ProgressBar
    private lateinit var storageBar: ProgressBar
    private lateinit var ramBar: ProgressBar
    private lateinit var batteryStatus: TextView
    private var receiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val percent = if (level >= 0) (level * 100f / scale).roundToInt() else -1
            val tenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val temp = if (tenths != Int.MIN_VALUE) tenths / 10f else null
            batteryValue.text = if (percent >= 0) "$percent%" else "Unavailable"
            batteryBar.progress = percent.coerceIn(0, 100)
            batteryTempValue.text = temp?.let { String.format(Locale.getDefault(), "%.1f °C", it) }
                ?: "Unavailable on this device"
            batteryStatus.text = when {
                percent < 0 -> "Battery status unavailable"
                intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0 -> "Connected to power"
                else -> "Running on battery"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 18, 35)
        window.navigationBarColor = Color.rgb(12, 18, 35)
        createLayout()
        refreshStorageAndRam()
    }

    override fun onResume() {
        super.onResume()
        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(batteryReceiver, filter)
            }
            receiverRegistered = true
        }
        refreshStorageAndRam()
    }

    override fun onPause() {
        if (receiverRegistered) {
            try { unregisterReceiver(batteryReceiver) } catch (_: IllegalArgumentException) {}
            receiverRegistered = false
        }
        super.onPause()
    }

    private fun createLayout() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            setBackgroundColor(Color.rgb(12, 18, 35))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        root.addView(TextView(this).apply {
            text = "‹   BACK"
            textSize = 14f
            setTextColor(Color.rgb(100, 181, 246))
            setPadding(0, 0, 0, dp(18))
            setOnClickListener { finish() }
            contentDescription = "Back"
        })
        root.addView(text("JARVIS DEVICE MONITOR", 12, Color.rgb(100, 181, 246), true))
        root.addView(text("Device overview", 28, Color.WHITE, true).apply {
            setPadding(0, dp(6), 0, dp(4))
        })
        root.addView(text("Live system information", 14, Color.rgb(177, 188, 210), false)
            .apply { setPadding(0, 0, 0, dp(18)) })

        val battery = card()
        battery.addView(text("BATTERY LEVEL", 12, Color.rgb(177, 188, 210), true))
        batteryValue = text("--%", 32, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(8)) }
        battery.addView(batteryValue)
        batteryBar = progress()
        battery.addView(batteryBar)
        battery.addView(text("BATTERY TEMPERATURE", 12, Color.rgb(177, 188, 210), true)
            .apply { setPadding(0, dp(16), 0, dp(4)) })
        batteryTempValue = text("-- °C", 22, Color.WHITE, true)
        battery.addView(batteryTempValue)
        batteryStatus = text("Reading battery status…", 12, Color.rgb(100, 181, 246), false)
        batteryStatus.setPadding(0, dp(8), 0, 0)
        battery.addView(batteryStatus)
        root.addView(battery)

        val storage = card()
        storage.addView(text("INTERNAL STORAGE", 12, Color.rgb(177, 188, 210), true))
        storageValue = text("Checking…", 18, Color.WHITE, true).apply { setPadding(0, dp(10), 0, dp(8)) }
        storage.addView(storageValue)
        storageBar = progress()
        storage.addView(storageBar)
        root.addView(storage)

        val ram = card()
        ram.addView(text("RAM MEMORY", 12, Color.rgb(177, 188, 210), true))
        ramValue = text("Checking…", 18, Color.WHITE, true).apply { setPadding(0, dp(10), 0, dp(8)) }
        ram.addView(ramValue)
        ramBar = progress()
        ram.addView(ramBar)
        root.addView(ram)

        root.addView(text(
            "Temperature is read from the battery sensor. Android may limit some system details on certain devices.",
            12, Color.rgb(177, 188, 210), false
        ).apply { setPadding(dp(2), dp(8), dp(2), 0) })
        setContentView(scroll)
    }

    private fun refreshStorageAndRam() {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            val used = (total - free).coerceAtLeast(0)
            val percent = if (total > 0) (used * 100.0 / total).roundToInt() else 0
            storageValue.text = "${bytes(used)} used / ${bytes(total)} total\n${bytes(free)} available • $percent% used"
            storageBar.progress = percent.coerceIn(0, 100)
        } catch (_: Exception) {
            storageValue.text = "Storage information unavailable"
            storageBar.progress = 0
        }
        try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(info)
            val total = info.totalMem
            val free = info.availMem.coerceAtMost(total)
            val used = (total - free).coerceAtLeast(0)
            val percent = if (total > 0) (used * 100.0 / total).roundToInt() else 0
            ramValue.text = "${bytes(used)} used / ${bytes(total)} total\n${bytes(free)} available • $percent% used"
            ramBar.progress = percent.coerceIn(0, 100)
        } catch (_: Exception) {
            ramValue.text = "RAM information unavailable"
            ramBar.progress = 0
        }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(25, 34, 57))
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), Color.rgb(48, 63, 91))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(14)
        layoutParams = lp
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.START
        }

    private fun progress() = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
        progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(100, 181, 246))
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(53, 65, 91))
        minimumHeight = dp(8)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun bytes(n: Long): String {
        if (n < 1024) return "$n B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = n.toDouble()
        var index = -1
        do { value /= 1024.0; index++ } while (value >= 1024 && index < units.lastIndex)
        return String.format(Locale.getDefault(), "%.1f %s", value, units[index.coerceAtMost(units.lastIndex)])
    }
}
