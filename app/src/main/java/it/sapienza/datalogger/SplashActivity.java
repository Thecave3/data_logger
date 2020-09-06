package it.sapienza.datalogger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG_ACTIVITY = SplashActivity.class.getSimpleName();
    private boolean done = false;
    private static final long DELAY = 2000;
    Handler timerHandler;
    Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (done) {
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);
                    finish();
                    return;
                }
                done = true;
                timerHandler.postDelayed(this, DELAY);
            }
        };
    }

    @Override
    public void onPause() {
        timerHandler.removeCallbacks(timerRunnable);
        super.onPause();
    }

    @Override
    public void onResume() {
        timerHandler.postDelayed(timerRunnable, 500);
        super.onResume();
    }
}