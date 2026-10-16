package com.example.service1;

import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView timerText;
    private TextView stopwatchText;
    private EditText inputSeconds;

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int value = intent.getIntExtra(MediaService.EXTRA_SECONDS, 0);
            int mode = intent.getIntExtra(MediaService.EXTRA_MODE, 0);
            String time = String.format("%02d:%02d", value / 60, value % 60);

            if (mode == MediaService.MODE_STOPWATCH) {
                stopwatchText.setText(time);
            } else {
                timerText.setText(time);
            }
        }
    };

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            timerText.setText("00:00");
            Toast.makeText(MainActivity.this, "Таймер завершён!", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerText = findViewById(R.id.timerText);
        stopwatchText = findViewById(R.id.stopwatchText);
        inputSeconds = findViewById(R.id.inputSeconds);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f1 = new IntentFilter(MediaService.ACTION_TICK);
        IntentFilter f2 = new IntentFilter(MediaService.ACTION_FINISH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tickReceiver, f1, Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(finishReceiver, f2, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(tickReceiver, f1);
            registerReceiver(finishReceiver, f2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(tickReceiver);
        unregisterReceiver(finishReceiver);
    }

    public void startStopwatch(View v) {
        Intent i = new Intent(this, MediaService.class);
        i.putExtra(MediaService.EXTRA_MODE, MediaService.MODE_STOPWATCH);
        startForegroundService(i);
    }

    public void stopStopwatch(View v) {
        stopService(new Intent(this, MediaService.class));
        stopwatchText.setText("00:00");
    }

    public void startTimer(View v) {
        String s = inputSeconds.getText().toString().trim();
        int sec = s.isEmpty() ? 60 : Integer.parseInt(s);
        Intent i = new Intent(this, MediaService.class);
        i.putExtra(MediaService.EXTRA_MODE, MediaService.MODE_TIMER);
        i.putExtra(MediaService.EXTRA_VALUE, sec);
        startForegroundService(i);
    }

    public void stopTimer(View v) {
        stopService(new Intent(this, MediaService.class));
        timerText.setText("00:00");
    }
}