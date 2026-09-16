package apk.oriommd.demopicker;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final double BYTES_PER_GB = 1000d * 1000d * 1000d;

    private static final String PREFS_NAME = "demopicker_prefs";
    private static final String PREF_ACTIVE_ALIAS = "active_alias";
    private static final String ALIAS_PACKAGE_PREFIX = "apk.oriommd.demopicker.alias.";

    /** Component suffix and matching display-name string resource, index-aligned. */
    private static final String[] ALIAS_SUFFIXES = {
            "AliasSettings", "AliasSystem", "AliasTools", "AliasFiles",
            "AliasCalendar", "AliasCalculator", "AliasNotes", "AliasClock"
    };
    private static final int[] ALIAS_LABEL_RES = {
            R.string.alias_label_settings, R.string.alias_label_system,
            R.string.alias_label_tools, R.string.alias_label_files,
            R.string.alias_label_calendar, R.string.alias_label_calculator,
            R.string.alias_label_notes, R.string.alias_label_clock
    };

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

        batterySubtitle = findViewById(R.id.text_battery_subtitle);
        storageSubtitle = findViewById(R.id.text_storage_subtitle);

        View.OnClickListener openBlank = v -> startActivity(new Intent(this, BlankActivity.class));
        findViewById(R.id.row_battery).setOnClickListener(openBlank);
        findViewById(R.id.row_display).setOnClickListener(openBlank);
        findViewById(R.id.row_sound).setOnClickListener(openBlank);
        findViewById(R.id.row_storage).setOnClickListener(openBlank);
        findViewById(R.id.row_accessibility).setOnClickListener(openBlank);
        findViewById(R.id.row_system).setOnClickListener(openBlank);

        View aboutRow = findViewById(R.id.row_about);
        aboutRow.setOnClickListener(openBlank);
        aboutRow.setOnLongClickListener(v -> {
            showRenameDialog();
            return true;
        });

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

    /** Long-press "About tablet" to pick which of the pre-declared launcher labels is shown. */
    private void showRenameDialog() {
        String[] labels = new String[ALIAS_SUFFIXES.length];
        for (int i = 0; i < ALIAS_SUFFIXES.length; i++) {
            labels[i] = getString(ALIAS_LABEL_RES[i]);
        }

        String activeSuffix = getActiveAliasSuffix();
        int checkedIndex = indexOfSuffix(activeSuffix);

        new AlertDialog.Builder(this)
                .setTitle(R.string.rename_dialog_title)
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    applyAlias(which);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyAlias(int index) {
        String newSuffix = ALIAS_SUFFIXES[index];
        String currentSuffix = getActiveAliasSuffix();
        if (newSuffix.equals(currentSuffix)) {
            return;
        }

        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(aliasComponent(currentSuffix),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(aliasComponent(newSuffix),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(PREF_ACTIVE_ALIAS, newSuffix)
                .apply();

        String newLabel = getString(ALIAS_LABEL_RES[index]);
        Toast.makeText(this, getString(R.string.rename_applied_toast, newLabel), Toast.LENGTH_LONG).show();
    }

    private String getActiveAliasSuffix() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_ACTIVE_ALIAS, ALIAS_SUFFIXES[0]);
    }

    private static int indexOfSuffix(String suffix) {
        for (int i = 0; i < ALIAS_SUFFIXES.length; i++) {
            if (ALIAS_SUFFIXES[i].equals(suffix)) {
                return i;
            }
        }
        return 0;
    }

    private ComponentName aliasComponent(String suffix) {
        return new ComponentName(getPackageName(), ALIAS_PACKAGE_PREFIX + suffix);
    }
}
