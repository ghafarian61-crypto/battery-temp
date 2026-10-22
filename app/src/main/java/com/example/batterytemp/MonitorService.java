package com.example.batterytemp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Small foreground service. Android does not allow SCREEN_ON to be received from the
 * manifest, so this service stays alive and refreshes the widget on screen-on,
 * unlock and battery changes.
 */
public class MonitorService extends Service {

    private static final String CHANNEL_ID = "monitor";
    private static final int NOTIFICATION_ID = 1;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BatteryWidgetProvider.updateAll(context);
        }
    };

    private boolean registered = false;

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
        goForeground();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(receiver, filter);
        registered = true;
        BatteryWidgetProvider.updateAll(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        BatteryWidgetProvider.updateAll(this);
        return START_STICKY;
    }

    private void goForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN);
        nm.createNotificationChannel(channel);
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(getString(R.string.notif_title))
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, n);
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
