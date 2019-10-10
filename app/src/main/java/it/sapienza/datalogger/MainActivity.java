package it.sapienza.datalogger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.Date;

import it.sapienza.datalogger.sensor_logger.SensorLogger;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int SAMPLING_RATE = 20000;
    private boolean gyroReady = false;
    private boolean accelReady = false;
    private SensorLogger sensorLogger;
    private SensorManager mSensorManager;
    private Sensor gyroscope, accelerometer;

    private float accelXaxis, accelYaxis, accelZaxis;
    private float gyroXaxis, gyroYaxis, gyroZaxis;
    private long startTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startBtn = findViewById(R.id.buttonStart);
        Button stopBtn = findViewById(R.id.buttonStop);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        final SensorEventListener listener = this;

        startBtn.setOnClickListener(v -> {
            try {
                sensorLogger = new SensorLogger(getApplicationContext(), "");
                mSensorManager.registerListener(listener, accelerometer, SAMPLING_RATE);
                mSensorManager.registerListener(listener, gyroscope, SAMPLING_RATE);
//                startBtn.setClickable(false);
//                startBtn.setVisibility(View.INVISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        stopBtn.setOnClickListener(v -> {
            try {
                mSensorManager.unregisterListener(listener);
                sensorLogger.closeLogger();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sensorLogger = null;
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelReady = true;
            accelXaxis = event.values[0];
            accelYaxis = event.values[1];
            accelZaxis = event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroReady = true;
            gyroXaxis = event.values[0];
            gyroYaxis = event.values[1];
            gyroZaxis = event.values[2];
        }


        if (accelReady && gyroReady) {
            accelReady = false;
            gyroReady = false;

            if (startTime == 0L)
                startTime = event.timestamp;

            String values = String.format("%s,%s,%s,%s,%s,%s", accelXaxis, accelYaxis, accelZaxis, gyroXaxis, gyroYaxis, gyroZaxis);
            long timestamp = event.timestamp - startTime;

            try {
                sensorLogger.writeLog(timestamp, values);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
