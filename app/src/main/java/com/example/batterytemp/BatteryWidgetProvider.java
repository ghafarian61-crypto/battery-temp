package com.example.batterytemp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.RemoteViews;

import java.util.Locale;

public class BatteryWidgetProvider extends AppWidgetProvider {

    private static final int COLOR_GREEN = 0xFF2ECC71;   // 0..30
    private static final int COLOR_ORANGE = 0xFFFF9800;  // 31..40
    private static final int COLOR_RED = 0xFFF44336;     // 41..50 and above
    private static final int COLOR_UNKNOWN = 0xFF9E9E9E;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateAll(context);
        MonitorService.start(context);
    }

    @Override
    public void onEnabled(Context context) {
        MonitorService.start(context);
        updateAll(context);
    }

    /** Battery temperature in degrees Celsius, or NaN if unavailable. */
    static float readTemperature(Context context) {
        Intent i = context.getApplicationContext()
                .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return Float.NaN;
        int raw = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        if (raw == Integer.MIN_VALUE) return Float.NaN;
        return raw / 10f;
    }

    static int colorFor(float celsius) {
        int rounded = Math.round(celsius);
        if (rounded <= 30) return COLOR_GREEN;
        if (rounded <= 40) return COLOR_ORANGE;
        return COLOR_RED;
    }

    static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        ComponentName component = new ComponentName(app, BatteryWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids == null || ids.length == 0) return;

        float temp = readTemperature(app);
        RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.widget_battery);
        if (Float.isNaN(temp)) {
            views.setTextViewText(R.id.temp_value, "--");
            views.setTextColor(R.id.temp_value, COLOR_UNKNOWN);
        } else {
            views.setTextViewText(R.id.temp_value,
                    String.format(Locale.US, "%.1f°", temp));
            views.setTextColor(R.id.temp_value, colorFor(temp));
        }

        Intent open = new Intent(app, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(app, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_root, pi);

        manager.updateAppWidget(ids, views);
    }
}
