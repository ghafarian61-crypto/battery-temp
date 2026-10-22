package com.example.batterytemp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final String NOTIF_PERMISSION = "android.permission.POST_NOTIFICATIONS";

    private TextView tempText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        tempText = new TextView(this);
        tempText.setTextSize(28);
        tempText.setGravity(Gravity.CENTER);
        root.addView(tempText);

        TextView info = new TextView(this);
        info.setTextSize(16);
        info.setPadding(0, pad, 0, pad);
        info.setText(R.string.main_info);
        root.addView(info);

        Button battery = new Button(this);
        battery.setText(R.string.btn_battery);
        battery.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        root.addView(battery);

        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(NOTIF_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{NOTIF_PERMISSION}, 1);
        }

        MonitorService.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        float t = BatteryWidgetProvider.readTemperature(this);
        if (Float.isNaN(t)) {
            tempText.setText("--");
        } else {
            tempText.setText(String.format(Locale.US, "%.1f°C", t));
            tempText.setTextColor(BatteryWidgetProvider.colorFor(t));
        }
        BatteryWidgetProvider.updateAll(this);
    }
}
