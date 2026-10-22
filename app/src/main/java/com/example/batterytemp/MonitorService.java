package com.example.batterytemp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.IBinder;

/**
 * Small foreground service. Android does not allow SCREEN_ON to be received from the
 * manifest, so this service stays alive and listens for screen-on, unlock and battery
 * changes. The widget and the notification are only redrawn when the displayed number
 * actually changes (battery events also fire for charge level and voltage).
 */
public class MonitorService extends Service {

    private static final String CHANNEL_ID = "monitor_temp";
    private static final String OLD_CHANNEL_ID = "monitor";
    private static final int NOTIFICATION_ID = 1;

    private String lastText = null;
    private boolean registered = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Battery events are only acted on when the number changed; screen events always refresh.
            boolean force = !Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction());
            refresh(force);
        }
    };

    static void start(Context context) {
        try {
            context.getApplicationContext().startForegroundService(
                    new Intent(context.getApplicationContext(), MonitorService.class));
        } catch (Exception e) {
            // Android may refuse to start it from the background; opening the app once fixes it.
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID,
                buildNotification(BatteryWidgetProvider.readTemperature(this)));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(receiver, filter);
        registered = true;

        refresh(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID,
                buildNotification(BatteryWidgetProvider.readTemperature(this)));
        refresh(true);
        return START_STICKY;
    }

    private void refresh(boolean force) {
        float t = BatteryWidgetProvider.readTemperature(this);
        String text = BatteryWidgetProvider.textFor(t);
        if (!force && text.equals(lastText)) return;
        lastText = text;

        BatteryWidgetProvider.updateAll(this);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ID, buildNotification(t));
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        // The old channel was "minimum" importance, which hides the status-bar icon.
        nm.deleteNotificationChannel(OLD_CHANNEL_ID);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(float t) {
        String text = BatteryWidgetProvider.textFor(t);
        String title = Float.isNaN(t) ? text : text + "°C";

        PendingIntent pi = PendingIntent.getActivity(this, 1,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(makeIcon(t))
                .setContentTitle(title)
                .setContentText(getString(R.string.notif_title))
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
    }

    /** Status-bar icon: the rounded temperature drawn as white digits. */
    private Icon makeIcon(float t) {
        String s = Float.isNaN(t) ? "--" : String.valueOf(Math.round(t));
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);

        float textSize = 100f;
        paint.setTextSize(textSize);
        while (paint.measureText(s) > size && textSize > 10f) {
            textSize -= 2f;
            paint.setTextSize(textSize);
        }

        Rect bounds = new Rect();
        paint.getTextBounds(s, 0, s.length(), bounds);
        float y = size / 2f - bounds.exactCenterY();
        canvas.drawText(s, size / 2f, y, paint);

        return Icon.createWithBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        if (registered) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
            registered = false;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
