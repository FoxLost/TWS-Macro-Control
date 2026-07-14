namespace TWSControlWin;

public static class EqData
{
    public record Band(float Freq, float Gain, float Q);
    public record Preset(string Name, float MasterGain, List<Band> Bands);

    public static readonly List<Preset> Presets = new()
    {
        new("Classic", -9f, new List<Band> {
            new(180f, -14f, 0.8f), new(200f, -6f, 4f), new(350f, -4f, 1f),
            new(550f, -7f, 0.8f), new(1200f, -2f, 1.6f), new(3300f, 8f, 1.5f),
            new(5500f, -4f, 1.5f), new(9000f, 2f, 1.5f)
        }),
        new("Bass Boost", -9f, new List<Band> {
            new(20f, 2f, 1f), new(30f, 3f, 0.4f), new(60f, 5f, 0.5f),
            new(180f, -12f, 0.8f), new(300f, -4f, 1.5f), new(550f, -6f, 1f),
            new(3300f, 10f, 1.5f), new(5500f, -2f, 1.5f), new(9000f, 4f, 1.5f)
        }),
        new("Bass Reduction", -9f, new List<Band> {
            new(30f, 3f, 0.4f), new(60f, -4f, 0.5f), new(180f, -12f, 0.8f),
            new(300f, -4f, 1.5f), new(550f, -6f, 1f), new(3300f, 10f, 1.5f),
            new(5500f, -2f, 1.5f), new(9000f, 4f, 1.5f)
        }),
        new("Electronic", -9f, new List<Band> {
            new(30f, 3f, 0.4f), new(180f, -12f, 0.8f), new(300f, -4f, 1.5f),
            new(400f, -6f, 1.2f), new(550f, -6f, 1f), new(3500f, 4f, 0.8f),
            new(5500f, -2f, 1.5f), new(9000f, 4f, 1.5f)
        }),
        new("Popular", -9f, new List<Band> {
            new(30f, 3f, 0.4f), new(80f, -7f, 0.5f), new(150f, -12f, 0.8f),
            new(300f, -4f, 1.5f), new(500f, -7f, 0.8f), new(1700f, -5f, 1.5f),
            new(3300f, 10f, 1f), new(5500f, -6f, 0.5f), new(9000f, 4f, 1.5f)
        }),
        new("Classical Music", -9f, new List<Band> {
            new(20f, 3f, 1f), new(30f, 3f, 0.4f), new(180f, -12f, 0.8f),
            new(350f, -6f, 0.9f), new(550f, -6f, 1f), new(1700f, -3f, 1f),
            new(3300f, 10f, 1.5f), new(5500f, -2f, 1.5f), new(9000f, 4f, 1.5f)
        }),
        new("Rock & Roll", -9f, new List<Band> {
            new(20f, 0f, 1f), new(30f, 3f, 0.4f), new(180f, -12f, 0.8f),
            new(350f, -6f, 0.9f), new(550f, -9f, 1f), new(1700f, -3f, 1f),
            new(3300f, 12f, 1.5f), new(5500f, -2f, 1.5f), new(9000f, 4f, 1.5f)
        }),
        new("Folk", -9f, new List<Band> {
            new(30f, 3f, 0.4f), new(180f, -12f, 0.8f), new(350f, -6f, 0.9f),
            new(550f, -6f, 1f), new(1000f, 5f, 1f), new(3300f, 4f, 1.5f),
            new(5500f, -6f, 1.5f), new(9000f, 4f, 1.5f)
        }),
        new("Treble Enhancement", -9f, new List<Band> {
            new(30f, 3f, 0.4f), new(180f, -12f, 0.8f), new(300f, -4f, 1.5f),
            new(550f, -6f, 1f), new(3000f, -2f, 1f), new(3300f, 12f, 1f),
            new(5500f, 0f, 1f), new(9000f, 4f, 1.5f)
        })
    };

    public static byte[] GaiaPacket(ushort cmdId, byte[]? payload, ushort vendor = 0x000A)
    {
        int plen = payload?.Length ?? 0;
        var hdr = new byte[]
        {
            0xFF, 0x04, 0x00, (byte)(plen & 0xFF),
            (byte)((vendor >> 8) & 0xFF), (byte)(vendor & 0xFF),
            (byte)((cmdId >> 8) & 0xFF), (byte)(cmdId & 0xFF)
        };
        if (payload == null || plen == 0) return hdr;
        var result = new byte[8 + plen];
        Buffer.BlockCopy(hdr, 0, result, 0, 8);
        Buffer.BlockCopy(payload, 0, result, 8, plen);
        return result;
    }

    public static List<byte[]> BuildEqPackets(int presetIndex)
    {
        var preset = Presets[presetIndex];
        var packets = new List<byte[]>();
        int bandCount = preset.Bands.Count;
        int mgI = (int)(preset.MasterGain * 60);

        for (int i = 0; i < bandCount; i++)
        {
            var band = preset.Bands[i];
            int hdr = ((bandCount & 0x0F) << 4) | ((i + 1) & 0x0F);
            int fI = (int)(band.Freq * 3);
            int gI = (int)(band.Gain * 60);
            int qI = (int)(band.Q * 4096);

            var payload = new byte[]
            {
                (byte)hdr,
                (byte)((mgI >> 8) & 0xFF), (byte)(mgI & 0xFF),
                (byte)((fI >> 8) & 0xFF), (byte)(fI & 0xFF),
                (byte)((gI >> 8) & 0xFF), (byte)(gI & 0xFF),
                (byte)((qI >> 8) & 0xFF), (byte)(qI & 0xFF)
            };
            packets.Add(GaiaPacket(0x0E01, payload, 0x001D));
        }
        return packets;
    }
}
