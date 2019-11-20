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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import it.sapienza.datalogger.sensor_logger.SensorLogger;

import static it.sapienza.datalogger.utility.Utility.DEBUG;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private int samplingRate;

    private boolean gyroReady = false;
    private boolean accelReady = false;

    private SensorLogger sensorLogger;
    private SensorManager mSensorManager;

    private Sensor gyroscope, accelerometer;

    private float accelXaxis, accelYaxis, accelZaxis;
    private float gyroXaxis, gyroYaxis, gyroZaxis;
    private long startTime = 0L;

    private TextView debugger;
    private TextInputEditText customTimeEditText;
    private RadioGroup timeRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startBtn = findViewById(R.id.buttonStart);
        Button stopBtn = findViewById(R.id.buttonStop);

        debugger = findViewById(R.id.debugger);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        customTimeEditText = findViewById(R.id.custom_time_value);

        timeRadioGroup = findViewById(R.id.timeRadioGroup);

        timeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.fastest:
                    samplingRate = SensorManager.SENSOR_DELAY_FASTEST;
                    customTimeEditText.setEnabled(false);
                    break;
                case R.id.ui:
                    samplingRate = SensorManager.SENSOR_DELAY_UI;
                    customTimeEditText.setEnabled(false);
                    break;
                case R.id.game:
                    samplingRate = SensorManager.SENSOR_DELAY_GAME;
                    customTimeEditText.setEnabled(false);
                    break;
                case R.id.normal:
                    samplingRate = SensorManager.SENSOR_DELAY_NORMAL;
                    customTimeEditText.setEnabled(false);
                    break;
                case R.id.custom:
                    customTimeEditText.setEnabled(true);
                    break;
                default:
                    writeDebug("Error in selecting checking id");
            }
        });

        timeRadioGroup.check(R.id.fastest);

        final SensorEventListener listener = this;
        startBtn.setOnClickListener(v -> {
            try {
                sensorLogger = new SensorLogger(getApplicationContext(), "");
                samplingRate = customTimeEditText.isEnabled() ? getCustomTime(String.valueOf(customTimeEditText.getText())) : samplingRate;
                mSensorManager.registerListener(listener, accelerometer, samplingRate);
                mSensorManager.registerListener(listener, gyroscope, samplingRate);
                timeRadioGroup.setEnabled(false);
                startBtn.setEnabled(false);
                customTimeEditText.setEnabled(false);
                stopBtn.setEnabled(true);
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
            stopBtn.setEnabled(false);
            timeRadioGroup.setEnabled(true);
            startBtn.setEnabled(true);
            customTimeEditText.setEnabled(true);
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
        writeDebug("onAccuracyChanged: accuracy changed");
    }

    /**
     * Auxiliary method to parse correctly the custom time in input by the user
     *
     * @param inputTime #{String} version of the input
     * @return integer version of inputTime
     */
    private int getCustomTime(String inputTime) {
        // if there're some constraints they've to be checked here
        return Integer.parseInt(String.format("%s", inputTime));
    }


    /**
     * Write a message debug into log and text debugger.
     * The message will be logged into the debug logger of Android if DEBUG is enabled.
     *
     * @param message message to be written
     */
    private void writeDebug(final String message) {
        runOnUiThread(() -> {
            if (debugger.getLineCount() == debugger.getMaxLines())
                debugger.setText(String.format("%s\n", message));
            else
                debugger.setText(String.format("%s%s\n", String.valueOf(debugger.getText()), message));
        });

        if (DEBUG)
            Log.d(TAG, "OUD: " + message);
    }

}
