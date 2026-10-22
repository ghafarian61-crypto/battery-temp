package com.example.batterytemp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Full-screen black page showing the battery temperature, closes itself after 1 second. */
public class MainActivity extends Activity {

    private static final long SHOW_MILLIS = 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable closer = new Runnable() {
        @Override
        public void run() {
            // Close the page and remove it from the recent-apps list completely.
            finishAndRemoveTask();
        }
    };

    private TextView tempText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        tempText = new TextView(this);
        tempText.setGravity(Gravity.CENTER);
        tempText.setTypeface(Typeface.DEFAULT_BOLD);
        tempText.setIncludeFontPadding(false);
        tempText.setTextColor(Color.WHITE);
        // One single line only, so the digits are never split onto separate lines.
        tempText.setSingleLine(true);
        tempText.setMaxLines(1);
        tempText.setHorizontallyScrolling(false);
        tempText.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tempText.setAutoSizeTextTypeUniformWithConfiguration(
                40, 900, 2, TypedValue.COMPLEX_UNIT_SP);
        root.addView(tempText);

        setContentView(root);
        hideSystemBars();

        // Make sure the background service that refreshes the widget is running.
        MonitorService.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        float t = BatteryWidgetProvider.readTemperature(this);
        tempText.setText(BatteryWidgetProvider.textFor(t));
        if (!Float.isNaN(t)) {
            tempText.setTextColor(BatteryWidgetProvider.colorFor(t));
        }
        BatteryWidgetProvider.updateAll(this);

        handler.removeCallbacks(closer);
        handler.postDelayed(closer, SHOW_MILLIS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(closer);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(closer);
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
