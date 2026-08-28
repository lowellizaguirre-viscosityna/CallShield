package com.lowell.callshield.audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.lowell.callshield.MainActivity;

public final class FaxButtonService extends Service {
    public static final String ACTION_SHOW = "com.lowell.callshield.action.SHOW_FAX_BUTTON";
    public static final String ACTION_PLAY = "com.lowell.callshield.action.PLAY_FAX";
    public static final String ACTION_STOP = "com.lowell.callshield.action.STOP_FAX";
    public static final String ACTION_HIDE = "com.lowell.callshield.action.HIDE_FAX_BUTTON";

    private static final String CHANNEL_ID = "fax_button";
    private static final int NOTIFICATION_ID = 2201;

    private FaxTonePlayer player;
    private boolean playing;
    private boolean controlsVisible;

    public static Intent createIntent(Context context, String action) {
        return new Intent(context, FaxButtonService.class).setAction(action);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Call-time fax button",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Controls the 30-second fax tone while a call is active");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW : intent.getAction();
        if (ACTION_HIDE.equals(action)) {
            controlsVisible = false;
            stopPlayback();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        controlsVisible = true;
        startForeground(NOTIFICATION_ID, createNotification());
        if (ACTION_PLAY.equals(action)) {
            startPlayback();
        } else if (ACTION_STOP.equals(action)) {
            stopPlayback();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        controlsVisible = false;
        stopPlayback();
        super.onDestroy();
    }

    private void startPlayback() {
        stopPlayback();
        try {
            player = new FaxTonePlayer();
            playing = true;
            updateNotification();
            player.play(() -> {
                player = null;
                playing = false;
                updateNotification();
            });
        } catch (RuntimeException exception) {
            stopPlayback();
        }
    }

    private void stopPlayback() {
        if (player != null) {
            player.close();
            player = null;
        }
        playing = false;
        updateNotification();
    }

    private void updateNotification() {
        if (!controlsVisible) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    private Notification createNotification() {
        PendingIntent openApp = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(playing ? "Fax sound playing" : "Call-time fax button")
                .setContentText(playing
                        ? "Playing the fax calling tone for 30 seconds"
                        : "Answer, enable speakerphone, then tap Play fax")
                .setContentIntent(openApp)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE);

        if (playing) {
            builder.addAction(action(android.R.drawable.ic_media_pause, "Stop", ACTION_STOP, 2));
        } else {
            builder.addAction(action(android.R.drawable.ic_media_play, "Play fax (30 sec)", ACTION_PLAY, 1));
        }
        builder.addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "Hide", ACTION_HIDE, 3));
        return builder.build();
    }

    private Notification.Action action(int icon, String title, String action, int requestCode) {
        PendingIntent pendingIntent = PendingIntent.getService(
                this,
                requestCode,
                createIntent(this, action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }
}
