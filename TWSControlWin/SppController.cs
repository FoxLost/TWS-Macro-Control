using System.IO;
using InTheHand.Net.Bluetooth;
using InTheHand.Net.Sockets;
using System.Text;

namespace TWSControlWin;

public class SppController : IDisposable
{
    private static readonly Guid SppUuid = new("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothClient? _client;
    private Stream? _stream;
    private Thread? _readerThread;
    private CancellationTokenSource? _cts;

    public bool IsConnected { get; private set; }
    public string DeviceName { get; private set; } = "TWS";
    public string DeviceMac { get; private set; } = "";

    public event Action<bool>? ConnectionChanged;
    public event Action<string>? StatusChanged;
    public event Action<int, int>? BatteryUpdated;
    public event Action<bool>? GameModeUpdated;
    public event Action<bool>? TouchUpdated;
    public event Action<int>? AncModeUpdated;
    public event Action<int>? EqPresetSent;

    public BluetoothDeviceInfo[]? FindTwsDevices()
    {
        try
        {
            using var searcher = new BluetoothClient();
            var devices = searcher.DiscoverDevices();
            return devices.Where(d =>
            {
                try
                {
                    var name = d.DeviceName ?? "";
                    return name.Contains("SOUNDPEATS", StringComparison.OrdinalIgnoreCase) ||
                           name.Contains("Air3", StringComparison.OrdinalIgnoreCase) ||
                           name.Contains("PEATS", StringComparison.OrdinalIgnoreCase);
                }
                catch { return false; }
            }).ToArray();
        }
        catch { return Array.Empty<BluetoothDeviceInfo>(); }
    }

    public async Task ConnectAsync()
    {
        if (IsConnected) return;
        StatusChanged?.Invoke("Scanning...");

        var devices = await Task.Run(FindTwsDevices);
        if (devices == null || devices.Length == 0)
        {
            StatusChanged?.Invoke("No TWS device found");
            return;
        }

        var device = devices[0];
        DeviceName = device.DeviceName;
        DeviceMac = device.DeviceAddress.ToString();

        StatusChanged?.Invoke($"Connecting to {DeviceName}...");

        try
        {
            _client = new BluetoothClient();
            _client.Connect(device.DeviceAddress, SppUuid);
            _stream = _client.GetStream();
            IsConnected = true;

            StatusChanged?.Invoke("Connected");
            ConnectionChanged?.Invoke(true);

            _cts = new CancellationTokenSource();
            _readerThread = new Thread(() => ReadLoop(_cts.Token)) { IsBackground = true };
            _readerThread.Start();

            await Task.Delay(300);
            QueryAll();
        }
        catch (Exception ex)
        {
            StatusChanged?.Invoke($"Failed: {ex.Message}");
            Cleanup();
        }
    }

    public void Disconnect()
    {
        Cleanup();
        StatusChanged?.Invoke("Disconnected");
        ConnectionChanged?.Invoke(false);
    }

    private void Cleanup()
    {
        IsConnected = false;
        _cts?.Cancel();
        try { _stream?.Close(); } catch { }
        try { _client?.Close(); } catch { }
        _stream = null;
        _client = null;
        _readerThread = null;
    }

    private void ReadLoop(CancellationToken ct)
    {
        var buf = new byte[512];
        try
        {
            while (!ct.IsCancellationRequested && IsConnected && _stream != null)
            {
                int n = _stream.Read(buf, 0, buf.Length);
                if (n > 0) HandleResponse(buf, n);
            }
        }
        catch
        {
            if (IsConnected)
            {
                IsConnected = false;
                StatusChanged?.Invoke("Lost");
                ConnectionChanged?.Invoke(false);
            }
        }
    }

    private void HandleResponse(byte[] data, int len)
    {
        for (int i = 0; i < len - 1; i++)
        {
            if (data[i] == 0xFF && data[i + 1] == 0x04)
            {
                ParseGaia(data, i, len);
                return;
            }
        }
    }

    private void ParseGaia(byte[] data, int offset, int totalLen)
    {
        if (totalLen - offset < 8) return;
        int plen = data[offset + 3] & 0xFF;
        int cmd = ((data[offset + 6] & 0xFF) << 8) | (data[offset + 7] & 0xFF);
        int start = offset + 8;
        int end = Math.Min(start + plen, totalLen);
        var rp = (end > start) ? data[start..end] : Array.Empty<byte>();

        bool isResp = (cmd & 0x8000) != 0;
        int rc = cmd & 0x7FFF;
        var p = (isResp && rp.Length > 1) ? rp[1..] : rp;

        if (isResp) ProcessGet(rc, p);
        else ProcessSet(rc, p);
    }

    private void ProcessGet(int cmd, byte[] p)
    {
        int v = p.Length > 0 ? p[0] & 0xFF : -1;
        switch (cmd)
        {
            case 0x0306: BatteryUpdated?.Invoke(v, -1); break;
            case 0x0307: BatteryUpdated?.Invoke(-1, v); break;
            case 0x030E: GameModeUpdated?.Invoke(v == 1); break;
            case 0x0310: AncModeUpdated?.Invoke(v); break;
            case 0x0312: TouchUpdated?.Invoke(v == 1); break;
        }
    }

    private void ProcessSet(int cmd, byte[] p)
    {
        int v = p.Length > 0 ? p[0] & 0xFF : -1;
        switch (cmd)
        {
            case 0x0311: AncModeUpdated?.Invoke(v); break;
            case 0x030F: GameModeUpdated?.Invoke(v == 1); break;
            case 0x0313:
                Thread.Sleep(150);
                if (IsConnected) Write(EqData.GaiaPacket(0x0312, null));
                break;
        }
    }

    public void Write(byte[] data)
    {
        try
        {
            _stream?.Write(data, 0, data.Length);
            _stream?.Flush();
        }
        catch
        {
            IsConnected = false;
            StatusChanged?.Invoke("Write failed");
            ConnectionChanged?.Invoke(false);
        }
    }

    public void SetAncMode(int m) { if (!IsConnected) return; Write(EqData.GaiaPacket(0x0311, new[] { (byte)m })); }
    public void SetGameMode(bool on) { if (!IsConnected) return; Write(EqData.GaiaPacket(0x030F, new[] { (byte)(on ? 1 : 0) })); }
    public void SetTouchEnabled(bool enabled) { if (!IsConnected) return; Write(EqData.GaiaPacket(0x0313, new[] { (byte)(enabled ? 1 : 0) })); }
    public void Reset() { if (!IsConnected) return; Write(EqData.GaiaPacket(0x0305, null)); }

    public void SetEqPreset(int presetId)
    {
        if (!IsConnected) return;
        var preset = EqData.Presets[presetId];
        new Thread(() =>
        {
            try
            {
                int bandCount = preset.Bands.Count;
                int mgI = (int)(preset.MasterGain * 60);
                for (int i = 0; i < bandCount; i++)
                {
                    if (!IsConnected) break;
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
                    Write(EqData.GaiaPacket(0x0E01, payload, 0x001D));
                    Thread.Sleep(50);
                }
                EqPresetSent?.Invoke(presetId);
            }
            catch { }
        }) { IsBackground = true }.Start();
    }

    public void SendCustomEq(float[] gains)
    {
        if (!IsConnected) return;
        var refPreset = EqData.Presets[0];
        new Thread(() =>
        {
            try
            {
                int bandCount = Math.Min(gains.Length, refPreset.Bands.Count);
                int mgI = (int)(-9f * 60);
                for (int i = 0; i < bandCount; i++)
                {
                    if (!IsConnected) break;
                    var band = refPreset.Bands[i];
                    int hdr = ((bandCount & 0x0F) << 4) | ((i + 1) & 0x0F);
                    int fI = (int)(band.Freq * 3);
                    int gI = (int)(gains[i] * 60);
                    int qI = (int)(band.Q * 4096);

                    var payload = new byte[]
                    {
                        (byte)hdr,
                        (byte)((mgI >> 8) & 0xFF), (byte)(mgI & 0xFF),
                        (byte)((fI >> 8) & 0xFF), (byte)(fI & 0xFF),
                        (byte)((gI >> 8) & 0xFF), (byte)(gI & 0xFF),
                        (byte)((qI >> 8) & 0xFF), (byte)(qI & 0xFF)
                    };
                    Write(EqData.GaiaPacket(0x0E01, payload, 0x001D));
                    Thread.Sleep(50);
                }
                EqPresetSent?.Invoke(9);
            }
            catch { }
        }) { IsBackground = true }.Start();
    }

    public void QueryAll()
    {
        new Thread(() =>
        {
            foreach (ushort c in new ushort[] { 0x0306, 0x0307, 0x0309, 0x030E, 0x0310, 0x0312 })
            {
                if (!IsConnected) break;
                Write(EqData.GaiaPacket(c, null));
                Thread.Sleep(120);
            }
        }) { IsBackground = true }.Start();
    }

    public void Dispose()
    {
        Cleanup();
        GC.SuppressFinalize(this);
    }
}
