using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace TWSControlWin;

public partial class MainWindow : Window
{
    private readonly SppController _spp = new();
    private bool _gameOn;
    private bool _touchEnabled = true;
    private int _activeEqIndex = -1;
    private readonly Button[] _eqButtons = new Button[10];
    private readonly float[] _customGains = new float[8];
    private readonly Slider[] _customSliders = new Slider[8];

    private static readonly SolidColorBrush ClrDim = new(Color.FromRgb(0x16, 0x21, 0x3E));
    private static readonly SolidColorBrush ClrActive = new(Color.FromRgb(0x4C, 0xAF, 0x50));
    private static readonly SolidColorBrush ClrAccent = new(Color.FromRgb(0xE9, 0x45, 0x60));

    public MainWindow()
    {
        InitializeComponent();
        BuildEqGrid();
        BuildCustomSliders();
        WireEvents();
    }

    private void BuildEqGrid()
    {
        eqGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        eqGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        eqGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        eqGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        eqGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        eqGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        eqGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

        string[] labels = { "Classic", "Bass Boost", "Bass Reduction", "Electronic",
                            "Popular", "Classical", "Rock & Roll", "Folk", "Treble", "Custom" };
        for (int i = 0; i < 10; i++)
        {
            int row = i / 2, col = i % 2;
            var btn = new Button
            {
                Content = labels[i],
                Height = 34,
                FontSize = 11,
                Foreground = Brushes.White,
                Background = i == 9 ? new SolidColorBrush(Color.FromRgb(0x53, 0x34, 0x83)) : ClrDim,
                BorderThickness = new Thickness(0),
                Margin = new Thickness(col == 0 ? 0 : 3, i > 0 && col == 0 ? 3 : 0, col == 1 ? 0 : 3, 0),
                Tag = i
            };
            btn.Click += EqButton_Click;
            Grid.SetRow(btn, row);
            Grid.SetColumn(btn, col);
            eqGrid.Children.Add(btn);
            _eqButtons[i] = btn;
        }
    }

    private void BuildCustomSliders()
    {
        var refPreset = EqData.Presets[0];
        for (int i = 0; i < 8; i++)
        {
            var band = refPreset.Bands[i];
            var label = new TextBlock
            {
                Text = $"{(int)band.Freq}Hz: 0dB",
                FontSize = 10,
                Foreground = new SolidColorBrush(Color.FromRgb(0xBB, 0xDE, 0xFB)),
                HorizontalAlignment = HorizontalAlignment.Center,
                Margin = new Thickness(0, 0, 0, 2)
            };
            customSliders.Children.Add(label);

            int idx = i;
            var slider = new Slider
            {
                Minimum = 0, Maximum = 24, Value = 12, TickFrequency = 1,
                IsSnapToTickEnabled = true,
                Foreground = ClrAccent,
                Margin = new Thickness(0, 0, 0, 8)
            };
            slider.ValueChanged += (s, e) =>
            {
                int gain = (int)e.NewValue - 12;
                _customGains[idx] = gain;
                label.Text = $"{(int)band.Freq}Hz: {(gain >= 0 ? "+" : "")}{gain}dB";
            };
            customSliders.Children.Add(slider);
            _customSliders[i] = slider;
        }
    }

    private void WireEvents()
    {
        _spp.StatusChanged += s => Dispatcher.Invoke(() => txtStatus.Text = s);
        _spp.ConnectionChanged += c => Dispatcher.Invoke(() =>
        {
            btnConnect.Content = c ? "DISCONNECT" : "CONNECT";
            if (!c)
            {
                txtBatteryL.Text = "N/A"; txtBatteryR.Text = "N/A";
                _gameOn = false; _touchEnabled = true; _activeEqIndex = -1;
                UpdateGameBtn(); UpdateTouchBtn(); UpdateEqHighlight();
            }
            if (c) { txtTitle.Text = _spp.DeviceName; txtMac.Text = _spp.DeviceMac; }
        });
        _spp.BatteryUpdated += (l, r) => Dispatcher.Invoke(() =>
        {
            if (l is >= 0 and <= 100) txtBatteryL.Text = $"{l}%";
            if (r is >= 0 and <= 100) txtBatteryR.Text = $"{r}%";
        });
        _spp.GameModeUpdated += on => Dispatcher.Invoke(() => { _gameOn = on; UpdateGameBtn(); });
        _spp.TouchUpdated += en => Dispatcher.Invoke(() => { _touchEnabled = en; UpdateTouchBtn(); });
        _spp.AncModeUpdated += mode => Dispatcher.Invoke(() =>
        {
            btnAncOn.Background = mode == 1 ? ClrActive : ClrDim;
            btnTransparency.Background = mode == 2 ? ClrActive : ClrDim;
            btnAncOff.Background = mode == 0 ? ClrActive : ClrDim;
        });
        _spp.EqPresetSent += idx => Dispatcher.Invoke(() =>
        {
            _activeEqIndex = idx;
            UpdateEqHighlight();
            customEqPanel.Visibility = idx == 9 ? Visibility.Visible : Visibility.Collapsed;
        });
    }

    private async void BtnConnect_Click(object sender, RoutedEventArgs e)
    {
        if (_spp.IsConnected) _spp.Disconnect();
        else await _spp.ConnectAsync();
    }

    private void BtnAncOn_Click(object sender, RoutedEventArgs e) => _spp.SetAncMode(1);
    private void BtnTransparency_Click(object sender, RoutedEventArgs e) => _spp.SetAncMode(2);
    private void BtnAncOff_Click(object sender, RoutedEventArgs e) => _spp.SetAncMode(0);

    private void BtnGame_Click(object sender, RoutedEventArgs e)
    {
        _gameOn = !_gameOn;
        _spp.SetGameMode(_gameOn);
        UpdateGameBtn();
    }

    private void BtnTouch_Click(object sender, RoutedEventArgs e)
    {
        _touchEnabled = !_touchEnabled;
        _spp.SetTouchEnabled(_touchEnabled);
        UpdateTouchBtn();
    }

    private void EqButton_Click(object sender, RoutedEventArgs e)
    {
        int i = (int)((Button)sender).Tag!;
        if (i < 9)
        {
            _spp.SetEqPreset(i);
            _activeEqIndex = i;
            UpdateEqHighlight();
            customEqPanel.Visibility = Visibility.Collapsed;
        }
        else
        {
            _spp.SendCustomEq(_customGains);
            _activeEqIndex = 9;
            UpdateEqHighlight();
            customEqPanel.Visibility = Visibility.Visible;
        }
    }

    private void BtnCustomSend_Click(object sender, RoutedEventArgs e)
    {
        for (int i = 0; i < 8; i++)
            _customGains[i] = (float)(_customSliders[i].Value - 12);
        _spp.SendCustomEq(_customGains);
    }

    private void BtnReset_Click(object sender, RoutedEventArgs e)
    {
        if (MessageBox.Show("Factory reset TWS?", "Reset", MessageBoxButton.YesNo, MessageBoxImage.Warning) == MessageBoxResult.Yes)
            _spp.Reset();
    }

    private void UpdateGameBtn()
    {
        btnGame.Content = _gameOn ? "ON" : "OFF";
        btnGame.Background = _gameOn ? ClrActive : ClrDim;
    }

    private void UpdateTouchBtn()
    {
        btnTouch.Content = _touchEnabled ? "ON" : "OFF";
        btnTouch.Background = _touchEnabled ? ClrActive : ClrDim;
    }

    private void UpdateEqHighlight()
    {
        for (int i = 0; i < 10; i++)
            _eqButtons[i].Background = i == _activeEqIndex ? ClrActive :
                (i == 9 ? new SolidColorBrush(Color.FromRgb(0x53, 0x34, 0x83)) : ClrDim);
    }

    protected override void OnClosed(EventArgs e)
    {
        _spp.Dispose();
        base.OnClosed(e);
    }
}
