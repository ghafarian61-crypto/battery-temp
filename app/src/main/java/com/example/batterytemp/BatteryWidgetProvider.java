package com.example.batterytemp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.Locale;

public class BatteryWidgetProvider extends AppWidgetProvider {

    private static final int COLOR_BLUE = 0xFF29B6F6;    // 0..20
    private static final int COLOR_YELLOW = 0xFFFFEB3B;  // 21..30
    private static final int COLOR_ORANGE = 0xFFFF9800;  // 31..40
    private static final int COLOR_RED = 0xFFF44336;     // 41..100
    private static final int COLOR_UNKNOWN = 0xFF9E9E9E;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateAll(context);
        MonitorService.start(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, Bundle newOptions) {
        // The user resized this widget: redraw so the number is rescaled to the new size.
        updateAll(context);
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

    /** The colour band is decided from the rounded whole number. */
    static int colorFor(float celsius) {
        int rounded = Math.round(celsius);
        if (rounded <= 20) return COLOR_BLUE;
        if (rounded <= 30) return COLOR_YELLOW;
        if (rounded <= 40) return COLOR_ORANGE;
        return COLOR_RED;
    }

    /** Text shown to the user: one decimal digit, e.g. 31.5 */
    static String textFor(float celsius) {
        if (Float.isNaN(celsius)) return "--";
        return String.format(Locale.US, "%.1f", celsius);
    }

    /** Largest text size (in sp) that keeps the text fully inside the widget box. */
    static float fitTextSizeSp(Context context, Bundle options, String text) {
        float w = 70f;
        float h = 70f;
        if (options != null) {
            boolean portrait = context.getResources().getConfiguration().orientation
                    != Configuration.ORIENTATION_LANDSCAPE;
            float ow = options.getInt(portrait
                    ? AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                    : AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
            float oh = options.getInt(portrait
                    ? AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                    : AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
            if (ow > 0) w = ow;
            if (oh > 0) h = oh;
        }
        w -= 8f; // layout padding
        h -= 8f;

        // Approximate width of the text in "em" units (bold digits are about 0.62 em wide).
        float em = 0f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.') em += 0.32f;
            else if (c == '-') em += 0.40f;
            else em += 0.62f;
        }
        if (em <= 0f) em = 1f;

        float sizeDp = Math.min(w / em, h * 0.82f);
        float fontScale = context.getResources().getConfiguration().fontScale;
        if (fontScale <= 0f) fontScale = 1f;
        float sizeSp = sizeDp / fontScale;
        return Math.max(8f, Math.min(sizeSp, 220f));
    }

    /** Redraws every placed widget with the current temperature. */
    static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        ComponentName component = new ComponentName(app, BatteryWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids == null || ids.length == 0) return;

        float temp = readTemperature(app);
        String text = textFor(temp);
        int color = Float.isNaN(temp) ? COLOR_UNKNOWN : colorFor(temp);

        Intent open = new Intent(app, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(app, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Each widget can have its own size, so build the view per widget.
        for (int id : ids) {
            RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.widget_battery);
            views.setTextViewText(R.id.temp_value, text);
            views.setTextColor(R.id.temp_value, color);
            views.setTextViewTextSize(R.id.temp_value, TypedValue.COMPLEX_UNIT_SP,
                    fitTextSizeSp(app, manager.getAppWidgetOptions(id), text));
            views.setOnClickPendingIntent(R.id.widget_root, pi);
            manager.updateAppWidget(id, views);
        }
    }
}
