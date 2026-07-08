package foxlost.tws.macro

object EqData {
    data class Band(val freq: Float, val gain: Float, val q: Float)
    data class Preset(val name: String, val masterGain: Float, val bands: List<Band>)

    val PRESETS = listOf(
        Preset("Classic", -9f, listOf(
            Band(180f, -14f, 0.8f), Band(200f, -6f, 4f), Band(350f, -4f, 1f),
            Band(550f, -7f, 0.8f), Band(1200f, -2f, 1.6f), Band(3300f, 8f, 1.5f),
            Band(5500f, -4f, 1.5f), Band(9000f, 2f, 1.5f))),
        Preset("Bass Boost", -9f, listOf(
            Band(20f, 2f, 1f), Band(30f, 3f, 0.4f), Band(60f, 5f, 0.5f),
            Band(180f, -12f, 0.8f), Band(300f, -4f, 1.5f), Band(550f, -6f, 1f),
            Band(3300f, 10f, 1.5f), Band(5500f, -2f, 1.5f), Band(9000f, 4f, 1.5f))),
        Preset("Bass Reduction", -9f, listOf(
            Band(30f, 3f, 0.4f), Band(60f, -4f, 0.5f), Band(180f, -12f, 0.8f),
            Band(300f, -4f, 1.5f), Band(550f, -6f, 1f), Band(3300f, 10f, 1.5f),
            Band(5500f, -2f, 1.5f), Band(9000f, 4f, 1.5f))),
        Preset("Electronic", -9f, listOf(
            Band(30f, 3f, 0.4f), Band(180f, -12f, 0.8f), Band(300f, -4f, 1.5f),
            Band(400f, -6f, 1.2f), Band(550f, -6f, 1f), Band(3500f, 4f, 0.8f),
            Band(5500f, -2f, 1.5f), Band(9000f, 4f, 1.5f))),
        Preset("Popular", -9f, listOf(
            Band(30f, 3f, 0.4f), Band(80f, -7f, 0.5f), Band(150f, -12f, 0.8f),
            Band(300f, -4f, 1.5f), Band(500f, -7f, 0.8f), Band(1700f, -5f, 1.5f),
            Band(3300f, 10f, 1f), Band(5500f, -6f, 0.5f), Band(9000f, 4f, 1.5f))),
        Preset("Classical Music", -9f, listOf(
            Band(20f, 3f, 1f), Band(30f, 3f, 0.4f), Band(180f, -12f, 0.8f),
            Band(350f, -6f, 0.9f), Band(550f, -6f, 1f), Band(1700f, -3f, 1f),
            Band(3300f, 10f, 1.5f), Band(5500f, -2f, 1.5f), Band(9000f, 4f, 1.5f))),
        Preset("Rock & Roll", -9f, listOf(
            Band(20f, 0f, 1f), Band(30f, 3f, 0.4f), Band(180f, -12f, 0.8f),
            Band(350f, -6f, 0.9f), Band(550f, -9f, 1f), Band(1700f, -3f, 1f),
            Band(3300f, 12f, 1.5f), Band(5500f, -2f, 1.5f), Band(9000f, 4f, 1.5f))),
        Preset("Folk", -9f, listOf(
            Band(30f, 3f, 0.4f), Band(180f, -12f, 0.8f), Band(350f, -6f, 0.9f),
            Band(550f, -6f, 1f), Band(1000f, 5f, 1f), Band(3300f, 4f, 1.5f),
            Band(5500f, -6f, 1.5f), Band(9000f, 4f, 1.5f))),
        Preset("Treble Enhancement", -9f, listOf(
            Band(30f, 3f, 0.4f), Band(180f, -12f, 0.8f), Band(300f, -4f, 1.5f),
            Band(550f, -6f, 1f), Band(3000f, -2f, 1f), Band(3300f, 12f, 1f),
            Band(5500f, 0f, 1f), Band(9000f, 4f, 1.5f)))
    )

    val PRESET_NAMES = listOf(
        "eq_classic", "eq_bass_boost", "eq_bass_reduction", "eq_electronic",
        "eq_popular", "eq_classical", "eq_rock", "eq_folk", "eq_treble"
    )

    fun buildPackets(presetIndex: Int): List<ByteArray> {
        val preset = PRESETS.getOrNull(presetIndex) ?: return emptyList()
        val packets = mutableListOf<ByteArray>()
        val bandCount = preset.bands.size
        val mgI = (preset.masterGain * 60).toInt()
        for ((i, band) in preset.bands.withIndex()) {
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
            packets.add(gaiaPkt(0x0E01, payload, 0x1D))
        }
        return packets
    }

    fun gaiaPkt(cmdId: Int, payload: ByteArray?, vendor: Int = 0x0A): ByteArray {
        val plen = payload?.size ?: 0
        val hdr = byteArrayOf(0xFF.toByte(), 0x04, 0x00, (plen and 0xFF).toByte(),
            ((vendor shr 8) and 0xFF).toByte(), (vendor and 0xFF).toByte(),
            (cmdId shr 8).toByte(), (cmdId and 0xFF).toByte())
        return if (payload != null) hdr + payload else hdr
    }
}
