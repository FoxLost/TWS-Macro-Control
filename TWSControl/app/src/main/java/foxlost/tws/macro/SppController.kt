package foxlost.tws.macro

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class SppController(private val context: Context) {

    companion object {
        private const val TAG = "SppController"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        @Volatile var instance: SppController? = null; private set
    }

    data class EqBand(val freq: Float, val gain: Float, val q: Float)
    data class EqPreset(val name: String, val masterGain: Float, val bands: List<EqBand>)

    val EQ_PRESETS = EqData.PRESETS

    var deviceMac = ""; private set
    var deviceName = "TWS"; private set
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    @Volatile var isConnected = false; private set
    @Volatile private var isConnecting = false
    private val handler = Handler(Looper.getMainLooper())
    private var readerThread: Thread? = null

    var onConnectionChange: ((Boolean) -> Unit)? = null
    var onStatusUpdate: ((String) -> Unit)? = null
    var onBatteryUpdate: ((Int, Int) -> Unit)? = null
    var onGameModeUpdate: ((Boolean) -> Unit)? = null
    var onTouchUpdate: ((Boolean) -> Unit)? = null
    var onAncModeUpdate: ((Int) -> Unit)? = null
    var onEqPresetSent: ((Int) -> Unit)? = null

    private val bluetoothAdapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @SuppressLint("MissingPermission")
    fun findTwsDevice(): BluetoothDevice? {
        val adapter = bluetoothAdapter ?: return null
        return try {
            for (device in adapter.bondedDevices) {
                try {
                    val name = device.name ?: continue
                    if (name.contains("SOUNDPEATS", ignoreCase = true) ||
                        name.contains("Air3", ignoreCase = true) ||
                        name.contains("PEATS", ignoreCase = true)) {
                        return device
                    }
                } catch (_: Exception) {}
            }
            adapter.bondedDevices.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "findTwsDevice error: ${e.message}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun connect() {
        if (isConnecting || isConnected) return; isConnecting = true
        handler.post { onStatusUpdate?.invoke("Connecting...") }
        Thread {
            try {
                val adapter = bluetoothAdapter ?: throw IOException("No BT adapter")
                adapter.cancelDiscovery()
                val device = findTwsDevice()
                    ?: throw IOException("No TWS device found")
                deviceMac = device.address
                deviceName = try { device.name ?: "TWS" } catch (_: Exception) { "TWS" }
                var done = false
                for (a in 1..3) {
                    try {
                        socket = when (a) {
                            1 -> device.createRfcommSocketToServiceRecord(SPP_UUID)
                            2 -> device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                            else -> device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(device, 1) as BluetoothSocket
                        }
                        socket?.connect(); done = true; break
                    } catch (e: IOException) { try { socket?.close() } catch (_: Exception) {}; Thread.sleep(500) }
                }
                if (!done) throw IOException("SPP failed")
                outputStream = socket!!.outputStream; inputStream = socket!!.inputStream
                isConnected = true; isConnecting = false; instance = this
                handler.post { onStatusUpdate?.invoke("Connected") }; handler.post { onConnectionChange?.invoke(true) }
                startReader(); Thread.sleep(300); queryAll()
            } catch (e: Exception) {
                isConnected = false; isConnecting = false
                handler.post { onStatusUpdate?.invoke("Failed: ${e.message}") }; handler.post { onConnectionChange?.invoke(false) }; cleanup()
            }
        }.start()
    }

    fun disconnect() { Thread { cleanup(); isConnected = false; isConnecting = false; instance = null; handler.post { onStatusUpdate?.invoke("Disconnected") }; handler.post { onConnectionChange?.invoke(false) } }.start() }
    private fun cleanup() { try { outputStream?.close() } catch (_: Exception) {}; try { inputStream?.close() } catch (_: Exception) {}; try { socket?.close() } catch (_: Exception) {}; outputStream = null; inputStream = null; socket = null; readerThread?.interrupt(); readerThread = null }

    private fun startReader() {
        readerThread = Thread {
            val buf = ByteArray(512)
            try {
                while (isConnected && !Thread.interrupted()) {
                    val n = inputStream?.read(buf) ?: break; if (n > 0) handleResponse(buf.copyOf(n))
                }
            } catch (e: IOException) { if (isConnected) { isConnected = false; handler.post { onStatusUpdate?.invoke("Lost") }; handler.post { onConnectionChange?.invoke(false) } } }
        }; readerThread?.isDaemon = true; readerThread?.start()
    }

    private fun gaiaPacket(cmdId: Int, payload: ByteArray?, vendor: Int = 0x0A): ByteArray {
        val plen = payload?.size ?: 0
        val hdr = byteArrayOf(0xFF.toByte(), 0x04, 0x00, (plen and 0xFF).toByte(),
            ((vendor shr 8) and 0xFF).toByte(), (vendor and 0xFF).toByte(),
            (cmdId shr 8).toByte(), (cmdId and 0xFF).toByte())
        return if (payload != null) hdr + payload else hdr
    }

    fun write(data: ByteArray) {
        try { outputStream?.write(data); outputStream?.flush() } catch (e: IOException) {
            isConnected = false; handler.post { onStatusUpdate?.invoke("Write failed") }; handler.post { onConnectionChange?.invoke(false) }
        }
    }

    fun setAncMode(m: Int) { if (!isConnected) return; write(gaiaPacket(0x0311, byteArrayOf(m.toByte()))) }
    fun setGameMode(on: Boolean) { if (!isConnected) return; write(gaiaPacket(0x030F, byteArrayOf((if (on) 1 else 0).toByte()))) }
    fun setTouchEnabled(enabled: Boolean) { if (!isConnected) return; write(gaiaPacket(0x0313, byteArrayOf((if (enabled) 1 else 0).toByte()))) }
    fun reset() { if (!isConnected) return; write(gaiaPacket(0x0305, null)) }

    fun setEqPreset(presetId: Int) {
        if (!isConnected) return
        val preset = EQ_PRESETS.getOrNull(presetId) ?: return
        Thread {
            try {
                val bandCount = preset.bands.size
                val mgI = (preset.masterGain * 60).toInt()
                for ((i, band) in preset.bands.withIndex()) {
                    if (!isConnected) break
                    val hdr = ((bandCount and 0x0F) shl 4) or ((i + 1) and 0x0F)
                    val fI = (band.freq * 3).toInt()
                    val gI = (band.gain * 60).toInt()
                    val qI = (band.q * 4096).toInt()
                    val payload = byteArrayOf(
                        hdr.toByte(),
                        (mgI shr 8).toByte(), (mgI and 0xFF).toByte(),
                        (fI shr 8).toByte(), (fI and 0xFF).toByte(),
                        (gI shr 8).toByte(), (gI and 0xFF).toByte(),
                        (qI shr 8).toByte(), (qI and 0xFF).toByte()
                    )
                    write(gaiaPacket(0x0E01, payload, 0x1D))
                    Thread.sleep(50)
                }
                handler.post { onEqPresetSent?.invoke(presetId) }
                Log.d(TAG, "EQ preset sent: ${preset.name} ($bandCount bands)")
            } catch (e: Exception) { Log.e(TAG, "EQ err: ${e.message}") }
        }.start()
    }

    fun sendCustomEq(gains: FloatArray) {
        if (!isConnected) return
        val ref = EQ_PRESETS[0]
        Thread {
            try {
                val bandCount = minOf(gains.size, ref.bands.size)
                val mgI = (-9f * 60).toInt()
                for (i in 0 until bandCount) {
                    if (!isConnected) break
                    val band = ref.bands[i]
                    val hdr = ((bandCount and 0x0F) shl 4) or ((i + 1) and 0x0F)
                    val fI = (band.freq * 3).toInt()
                    val gI = (gains[i] * 60).toInt()
                    val qI = (band.q * 4096).toInt()
                    val payload = byteArrayOf(
                        hdr.toByte(),
                        (mgI shr 8).toByte(), (mgI and 0xFF).toByte(),
                        (fI shr 8).toByte(), (fI and 0xFF).toByte(),
                        (gI shr 8).toByte(), (gI and 0xFF).toByte(),
                        (qI shr 8).toByte(), (qI and 0xFF).toByte()
                    )
                    write(gaiaPacket(0x0E01, payload, 0x1D))
                    Thread.sleep(50)
                }
                handler.post { onEqPresetSent?.invoke(9) }
                Log.d(TAG, "Custom EQ sent ($bandCount bands)")
            } catch (e: Exception) { Log.e(TAG, "Custom EQ err: ${e.message}") }
        }.start()
    }

    fun queryAll() {
        Thread {
            for (c in listOf(0x0306, 0x0307, 0x0309, 0x030E, 0x0310, 0x0312)) {
                if (!isConnected) break; write(gaiaPacket(c, null)); Thread.sleep(120)
            }
        }.start()
    }

    private fun handleResponse(data: ByteArray) {
        for (i in 0 until data.size - 1) {
            if (data[i] == 0xFF.toByte() && data[i + 1] == 0x04.toByte()) { parseGaia(data.copyOfRange(i, data.size)); return }
        }
    }

    private fun parseGaia(data: ByteArray) {
        if (data.size < 8) return
        val plen = data[3].toInt() and 0xFF
        val cmd = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        val rp = if (data.size > 8) { val e = minOf(8 + plen, data.size); data.copyOfRange(8, e) } else byteArrayOf()
        val rc = cmd and 0x7FFF
        val isResp = (cmd and 0x8000) != 0
        val p = if (isResp && rp.size > 1) rp.copyOfRange(1, rp.size) else rp
        if (isResp) processGet(rc, p) else processSet(rc, p)
    }

    private fun processGet(cmd: Int, p: ByteArray) {
        val v = if (p.isNotEmpty()) p[0].toInt() and 0xFF else -1
        when (cmd) {
            0x0306 -> handler.post { onBatteryUpdate?.invoke(v, -1) }
            0x0307 -> handler.post { onBatteryUpdate?.invoke(-1, v) }
            0x030E -> handler.post { onGameModeUpdate?.invoke(v == 1) }
            0x0310 -> handler.post { onAncModeUpdate?.invoke(v) }
            0x0312 -> handler.post { onTouchUpdate?.invoke(v == 1) }
        }
    }

    private fun processSet(cmd: Int, p: ByteArray) {
        val v = if (p.isNotEmpty()) p[0].toInt() and 0xFF else -1
        when (cmd) {
            0x0311 -> handler.post { onAncModeUpdate?.invoke(v) }
            0x030F -> handler.post { onGameModeUpdate?.invoke(v == 1) }
            0x0313 -> {
                Thread.sleep(150)
                if (isConnected) write(gaiaPacket(0x0312, null))
            }
        }
    }
}
