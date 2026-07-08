package foxlost.tws.macro

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var spp: SppController
    private lateinit var tvTitle: TextView
    private lateinit var tvMac: TextView
    private lateinit var tvBatteryL: TextView
    private lateinit var tvBatteryR: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnGameToggle: Button
    private lateinit var btnTouchToggle: Button
    private lateinit var eqButtons: List<Button>
    private lateinit var customEqContainer: LinearLayout
    private lateinit var customEqSliders: MutableList<SeekBar>
    private lateinit var customEqLabels: MutableList<TextView>

    private var gameOn = false
    private var touchEnabled = true
    private var activeEqIndex = -1

    private val CLR_ACCENT = 0xFFE94560.toInt()
    private val CLR_DIM = 0xFF16213E.toInt()
    private val CLR_ACTIVE = 0xFF4CAF50.toInt()
    private val CUSTOM_GAINS = FloatArray(8) { 0f }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spp = SppController(this)
        tvTitle = findViewById(R.id.tvTitle)
        tvMac = findViewById(R.id.tvMac)
        tvBatteryL = findViewById(R.id.tvBatteryL)
        tvBatteryR = findViewById(R.id.tvBatteryR)
        tvStatus = findViewById(R.id.tvStatus)
        btnConnect = findViewById(R.id.btnConnect)
        btnGameToggle = findViewById(R.id.btnGameToggle)
        btnTouchToggle = findViewById(R.id.btnTouchToggle)
        customEqContainer = findViewById(R.id.customEqContainer)

        eqButtons = listOf(
            findViewById(R.id.btnEq1), findViewById(R.id.btnEq2), findViewById(R.id.btnEq3),
            findViewById(R.id.btnEq4), findViewById(R.id.btnEq5), findViewById(R.id.btnEq6),
            findViewById(R.id.btnEq7), findViewById(R.id.btnEq8), findViewById(R.id.btnEq9),
            findViewById(R.id.btnEq0)
        )

        buildCustomEqSliders()

        spp.onStatusUpdate = { runOnUiThread { tvStatus.text = it } }
        spp.onConnectionChange = { connected ->
            runOnUiThread {
                tvTitle.text = spp.deviceName
                tvMac.text = spp.deviceMac
                btnConnect.text = if (connected) "DISCONNECT" else "CONNECT"
                if (!connected) {
                    tvBatteryL.text = "N/A"; tvBatteryR.text = "N/A"
                    gameOn = false; touchEnabled = true; activeEqIndex = -1
                    updateGameBtn(); updateTouchBtn(); updateEqHighlight()
                }
            }
        }

        spp.onBatteryUpdate = { l, r ->
            runOnUiThread {
                if (l in 0..100) tvBatteryL.text = "$l%"
                if (r in 0..100) tvBatteryR.text = "$r%"
            }
        }

        spp.onGameModeUpdate = { on -> runOnUiThread { gameOn = on; updateGameBtn() } }
        spp.onTouchUpdate = { enabled -> runOnUiThread { touchEnabled = enabled; updateTouchBtn() } }
        spp.onEqPresetSent = { idx -> runOnUiThread { activeEqIndex = idx; updateEqHighlight(); if (idx == 9) customEqContainer.visibility = View.VISIBLE else customEqContainer.visibility = View.GONE } }

        spp.onAncModeUpdate = { mode ->
            runOnUiThread {
                findViewById<Button>(R.id.btnAncOn).backgroundTintList = ColorStateList.valueOf(if (mode == 1) CLR_ACTIVE else CLR_DIM)
                findViewById<Button>(R.id.btnTransparency).backgroundTintList = ColorStateList.valueOf(if (mode == 2) CLR_ACTIVE else CLR_DIM)
                findViewById<Button>(R.id.btnAncOff).backgroundTintList = ColorStateList.valueOf(if (mode == 0) CLR_ACTIVE else CLR_DIM)
            }
        }

        btnConnect.setOnClickListener {
            if (!spp.isConnected) spp.connect() else spp.disconnect()
        }

        findViewById<Button>(R.id.btnAncOn).setOnClickListener { spp.setAncMode(1) }
        findViewById<Button>(R.id.btnTransparency).setOnClickListener { spp.setAncMode(2) }
        findViewById<Button>(R.id.btnAncOff).setOnClickListener { spp.setAncMode(0) }

        btnGameToggle.setOnClickListener { gameOn = !gameOn; spp.setGameMode(gameOn); updateGameBtn() }
        btnTouchToggle.setOnClickListener {
            touchEnabled = !touchEnabled
            spp.setTouchEnabled(touchEnabled)
            updateTouchBtn()
        }

        for ((i, btn) in eqButtons.withIndex()) {
            btn.setOnClickListener {
                if (i < 9) {
                    spp.setEqPreset(i)
                    activeEqIndex = i
                    updateEqHighlight()
                    customEqContainer.visibility = View.GONE
                } else {
                    spp.sendCustomEq(CUSTOM_GAINS)
                    activeEqIndex = 9
                    updateEqHighlight()
                    customEqContainer.visibility = View.VISIBLE
                }
            }
        }

        findViewById<Button>(R.id.btnCustomSend).setOnClickListener {
            for (i in customEqSliders.indices) {
                CUSTOM_GAINS[i] = (customEqSliders[i].progress - 12).toFloat()
            }
            spp.sendCustomEq(CUSTOM_GAINS)
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener { showResetDialog() }
    }

    private fun buildCustomEqSliders() {
        customEqSliders = mutableListOf()
        customEqLabels = mutableListOf()
        val ref = spp.EQ_PRESETS[0]
        for ((i, band) in ref.bands.withIndex()) {
            val label = TextView(this).apply {
                text = "${band.freq.toInt()}Hz: 0dB"
                setTextColor(0xFFBBDEFB.toInt())
                textSize = 11f
                gravity = Gravity.CENTER_HORIZONTAL
            }
            customEqLabels.add(label)
            customEqContainer.addView(label)

            val seekBar = SeekBar(this).apply {
                max = 24
                progress = 12
                progressTintList = ColorStateList.valueOf(0xFFE94560.toInt())
                thumbTintList = ColorStateList.valueOf(0xFFE94560.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val gain = progress - 12
                    label.text = "${band.freq.toInt()}Hz: ${if (gain >= 0) "+" else ""}${gain}dB"
                    CUSTOM_GAINS[i] = gain.toFloat()
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
            customEqSliders.add(seekBar)
            customEqContainer.addView(seekBar)
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Device")
            .setMessage("Factory reset TWS?")
            .setPositiveButton("Reset") { _, _ -> spp.reset() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateGameBtn() {
        btnGameToggle.text = if (gameOn) "ON" else "OFF"
        btnGameToggle.backgroundTintList = ColorStateList.valueOf(if (gameOn) CLR_ACTIVE else CLR_DIM)
    }

    private fun updateTouchBtn() {
        btnTouchToggle.text = if (touchEnabled) "ON" else "OFF"
        btnTouchToggle.backgroundTintList = ColorStateList.valueOf(if (touchEnabled) CLR_ACTIVE else CLR_DIM)
    }

    private fun updateEqHighlight() {
        for ((i, btn) in eqButtons.withIndex()) {
            btn.backgroundTintList = ColorStateList.valueOf(if (i == activeEqIndex) CLR_ACTIVE else CLR_DIM)
        }
    }

    override fun onDestroy() { spp.disconnect(); super.onDestroy() }
}
