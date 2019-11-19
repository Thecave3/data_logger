package it.sapienza.datalogger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import it.sapienza.datalogger.sensor_logger.SensorLogger;

import static it.sapienza.datalogger.utility.Utility.DEBUG;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private int samplingRate = 20000;
    private boolean gyroReady = false;
    private boolean accelReady = false;
    private SensorLogger sensorLogger;
    private SensorManager mSensorManager;
    private Sensor gyroscope, accelerometer;

    private float accelXaxis, accelYaxis, accelZaxis;
    private float gyroXaxis, gyroYaxis, gyroZaxis;
    private long startTime = 0L;

    private RadioGroup timeRadioGroup;

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


        timeRadioGroup = findViewById(R.id.timeRadioGroup);

        timeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.fastest:
                    samplingRate = SensorManager.SENSOR_DELAY_FASTEST;
                    break;
                case R.id.ui:
                    samplingRate = SensorManager.SENSOR_DELAY_UI;
                    break;
                case R.id.game:
                    samplingRate = SensorManager.SENSOR_DELAY_GAME;
                    break;
                case R.id.normal:
                    samplingRate = SensorManager.SENSOR_DELAY_NORMAL;
                    break;
                case R.id.custom:
                    // enable input
                    break;
                default:
                    if (DEBUG)
                        Log.e(TAG, "Error in selecting checking id");
            }
        });


        startBtn.setOnClickListener(v -> {
            try {
                sensorLogger = new SensorLogger(getApplicationContext(), "");
                mSensorManager.registerListener(listener, accelerometer, samplingRate);
                mSensorManager.registerListener(listener, gyroscope, samplingRate);
//                startBtn.setClickable(false);
//                startBtn.setVisibility(View.INVISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        stopBtn.setOnClickListener(v ->

        {
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
