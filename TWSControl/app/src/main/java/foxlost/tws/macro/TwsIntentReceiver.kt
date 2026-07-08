package foxlost.tws.macro

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.IOException
import java.util.UUID

class TwsIntentReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TwsIntentReceiver"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private fun findTwsDevice(context: Context): BluetoothDevice? {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter() ?: return null
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

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != "foxlost.tws.macro.SET_MODE") return
        val mode = intent.getStringExtra("mode") ?: return
        Log.d(TAG, "mode=$mode")

        val packets = when (mode) {
            "anc_normal" -> listOf(EqData.gaiaPkt(0x0311, byteArrayOf(0)))
            "anc_strong" -> listOf(EqData.gaiaPkt(0x0311, byteArrayOf(1)))
            "anc_transparency" -> listOf(EqData.gaiaPkt(0x0311, byteArrayOf(2)))
            "game_on" -> listOf(EqData.gaiaPkt(0x030F, byteArrayOf(1)))
            "game_off" -> listOf(EqData.gaiaPkt(0x030F, byteArrayOf(0)))
            "touch_on" -> listOf(EqData.gaiaPkt(0x0313, byteArrayOf(1)))
            "touch_off" -> listOf(EqData.gaiaPkt(0x0313, byteArrayOf(0)))
            "reset" -> listOf(EqData.gaiaPkt(0x0305, null))
            "get_battery" -> listOf(EqData.gaiaPkt(0x0306, null), EqData.gaiaPkt(0x0307, null))
            else -> {
                val eqIdx = EqData.PRESET_NAMES.indexOf(mode)
                if (eqIdx >= 0) {
                    EqData.buildPackets(eqIdx)
                } else if (mode.startsWith("raw:")) {
                    listOf(mode.removePrefix("raw:").chunked(2)
                        .map { it.toInt(16).toByte() }.toByteArray())
                } else {
                    Log.w(TAG, "Unknown mode: $mode"); return
                }
            }
        }

        Thread {
            val existing = SppController.instance
            if (existing != null && existing.isConnected) {
                for (pkt in packets) { existing.write(pkt); Thread.sleep(50) }
                Log.d(TAG, "Sent $mode via singleton OK (${packets.size} packets)")
            } else {
                try {
                    val device = findTwsDevice(context) ?: throw IOException("No TWS device found")
                    val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    socket.connect()
                    val out = socket.outputStream
                    for (pkt in packets) {
                        out.write(pkt)
                        out.flush()
                        Thread.sleep(50)
                    }
                    socket.close()
                    Log.d(TAG, "Sent $mode via own SPP OK (${packets.size} packets)")
                } catch (e: IOException) {
                    Log.e(TAG, "SPP failed: ${e.message}")
                }
            }
        }.start()
    }
}
