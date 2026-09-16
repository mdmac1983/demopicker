package apk.oriommd.demopicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final double BYTES_PER_GB = 1000d * 1000d * 1000d;

    private TextView batterySubtitle;
    private TextView storageSubtitle;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBatterySubtitle(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());

        batterySubtitle = findViewById(R.id.text_battery_subtitle);
        storageSubtitle = findViewById(R.id.text_storage_subtitle);

        View.OnClickListener openBlank = v -> startActivity(new Intent(this, BlankActivity.class));
        findViewById(R.id.row_battery).setOnClickListener(openBlank);
        findViewById(R.id.row_display).setOnClickListener(openBlank);
        findViewById(R.id.row_sound).setOnClickListener(openBlank);
        findViewById(R.id.row_storage).setOnClickListener(openBlank);
        findViewById(R.id.row_accessibility).setOnClickListener(openBlank);
        findViewById(R.id.row_system).setOnClickListener(openBlank);
        findViewById(R.id.row_about).setOnClickListener(openBlank);

        updateStorageSubtitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent sticky = registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        updateBatterySubtitle(sticky);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
    }

    private void updateBatterySubtitle(Intent batteryStatus) {
        if (batteryStatus == null) {
            return;
        }
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return;
        }
        int percent = Math.round(level * 100f / scale);
        batterySubtitle.setText(getString(R.string.battery_remaining_format, percent));
    }

    private void updateStorageSubtitle() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long totalBytes = stat.getTotalBytes();
        long availableBytes = stat.getAvailableBytes();
        long usedBytes = totalBytes - availableBytes;

        String used = String.format(Locale.US, "%.1f", usedBytes / BYTES_PER_GB);
        String total = String.format(Locale.US, "%.1f", totalBytes / BYTES_PER_GB);
        storageSubtitle.setText(getString(R.string.storage_used_format, used, total));
    }
}
