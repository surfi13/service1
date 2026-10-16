package com.example.service1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MediaService extends Service {

    public static final String ACTION_TICK = "com.example.service1.TICK";
    public static final String ACTION_FINISH = "com.example.service1.FINISH";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_VALUE = "value";
    public static final String EXTRA_SECONDS = "seconds";

    public static final int MODE_STOPWATCH = 1;
    public static final int MODE_TIMER = 2;

    private static final String CHANNEL_ONGOING_ID = "service_channel";
    private static final String CHANNEL_FINISH_ID = "timer_finish_channel_v3";
    private static final int NOTIFICATION_ONGOING_ID = 1;
    private static final int NOTIFICATION_FINISH_ID = 2;

    private Handler handler;
    private Runnable runnable;
    private int seconds = 0;
    private int remaining = 60;
    private int mode = MODE_STOPWATCH;
    private boolean isRunning = false;
    private MediaPlayer player;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        handler = new Handler(Looper.getMainLooper());

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                if (mode == MODE_STOPWATCH) {
                    seconds++;
                    sendTick(seconds, MODE_STOPWATCH);
                    updateOngoingNotification("Секундомер",
                            String.format("%02d:%02d", seconds / 60, seconds % 60));
                    handler.postDelayed(this, 1000);
                } else {
                    sendTick(remaining, MODE_TIMER);
                    updateOngoingNotification("Таймер",
                            "Осталось: " + String.format("%02d:%02d",
                                    remaining / 60, remaining % 60));
                    if (remaining <= 0) {
                        finishTimer();
                        return;
                    }
                    remaining--;
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        mode = intent.getIntExtra(EXTRA_MODE, MODE_STOPWATCH);

        if (mode == MODE_STOPWATCH) {
            seconds = 0;
        } else {
            remaining = intent.getIntExtra(EXTRA_VALUE, 60);
        }

        if (!isRunning) {
            isRunning = true;
            startForeground(NOTIFICATION_ONGOING_ID,
                    buildOngoingNotification(
                            mode == MODE_STOPWATCH ? "Секундомер" : "Таймер",
                            "00:00"));
            handler.post(runnable);
        }
        return START_STICKY;
    }

    private void sendTick(int value, int mode) {
        Intent i = new Intent(ACTION_TICK);
        i.putExtra(EXTRA_SECONDS, value);
        i.putExtra(EXTRA_MODE, mode);
        sendBroadcast(i);
    }

    private void finishTimer() {
        isRunning = false;

        Log.d("TIMER_DEBUG", "finishTimer called");

        Intent finish = new Intent(ACTION_FINISH);
        sendBroadcast(finish);

        stopForeground(true);

        showFinishNotification();

        stopSelf();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel ongoing = new NotificationChannel(
                    CHANNEL_ONGOING_ID,
                    "Сервис",
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ongoing);

            NotificationChannel finish = new NotificationChannel(
                    CHANNEL_FINISH_ID,
                    "Завершение таймера",
                    NotificationManager.IMPORTANCE_HIGH);

            Uri soundUri = Uri.parse(
                    "android.resource://" + getPackageName() + "/" + R.raw.beep);

            Log.d("TIMER_DEBUG", "channel sound URI = " + soundUri);
            Log.d("TIMER_DEBUG", "R.raw.beep = " + R.raw.beep);
            Log.d("TIMER_DEBUG", "package = " + getPackageName());

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            finish.setSound(soundUri, attrs);
            finish.enableVibration(true);

            nm.createNotificationChannel(finish);
        }
    }

    private Notification buildOngoingNotification(String title, String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ONGOING_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void updateOngoingNotification(String title, String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ONGOING_ID, buildOngoingNotification(title, text));
        }
    }

    private void showFinishNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_FINISH_ID)
                .setContentTitle("Таймер завершён")
                .setContentText("Время вышло!")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_FINISH_ID, notification);
            Log.d("TIMER_DEBUG", "finish notification posted");
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (handler != null) handler.removeCallbacks(runnable);
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}