package it.sapienza.datalogger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.BoardiesITSolutions.FileDirectoryPicker.DirectoryPicker;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import it.sapienza.datalogger.sensor_logger.SensorLogger;

import static it.sapienza.datalogger.utility.Utility.DEBUG;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int REQUEST_EXTERNAL_STORAGE = 0;
    private static final int REQUEST_DIRECTORY_PICKER = 1;
    private static final String TAG = MainActivity.class.getSimpleName();

    private int samplingRate;

    private SensorLogger sensorLogger;
    private SensorManager mSensorManager;

    private Sensor gyroscope, accelerometer;

    private long startTime = 0L;

    private TextView debugger;
    private TextInputEditText customTimeEditText, customPathEditText;
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
        customPathEditText = findViewById(R.id.custom_path);

        customPathEditText.setOnClickListener(v -> pickSaveDirectory());

        timeRadioGroup = findViewById(R.id.timeRadioGroup);

        timeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.fastest:
                    samplingRate = SensorManager.SENSOR_DELAY_FASTEST;
                    customTimeEditText.setEnabled(false);
                    writeDebug("Fastest delay selected.");
                    break;
                case R.id.ui:
                    samplingRate = SensorManager.SENSOR_DELAY_UI;
                    customTimeEditText.setEnabled(false);
                    writeDebug("UI delay selected.");
                    break;
                case R.id.game:
                    samplingRate = SensorManager.SENSOR_DELAY_GAME;
                    customTimeEditText.setEnabled(false);
                    writeDebug("Game delay selected.");
                    break;
                case R.id.normal:
                    samplingRate = SensorManager.SENSOR_DELAY_NORMAL;
                    customTimeEditText.setEnabled(false);
                    writeDebug("Normal delay selected.");
                    break;
                case R.id.custom:
                    customTimeEditText.setEnabled(true);
                    writeDebug("Selected a custom delay, please put the value in the following field.");
                    break;
                default:
                    writeDebug("Error in selecting rate sampling.");
            }
        });

        timeRadioGroup.check(R.id.fastest);

        final SensorEventListener listener = this;
        startBtn.setOnClickListener(v -> {
            writeDebug("Started data taking.");
            try {
                customPathEditText.setEnabled(false);
                customPathEditText.setOnClickListener(null);
                sensorLogger.openLogger();

                samplingRate = customTimeEditText.isEnabled() ? getCustomTime(String.valueOf(customTimeEditText.getText())) : samplingRate;
                mSensorManager.registerListener(listener, accelerometer, samplingRate);
                mSensorManager.registerListener(listener, gyroscope, samplingRate);

                timeRadioGroup.setEnabled(false);
                startBtn.setEnabled(false);
                customTimeEditText.setEnabled(false);
                stopBtn.setEnabled(true);

            } catch (IOException e) {
                e.printStackTrace();
                writeDebug(e.getMessage());
                stopBtn.performClick();
            }
        });

        stopBtn.setOnClickListener(v -> {
            writeDebug("Data taking stopped.");
            try {
                mSensorManager.unregisterListener(listener);
                sensorLogger.closeLogger();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stopBtn.setEnabled(false);
            timeRadioGroup.setEnabled(true);
            startBtn.setEnabled(true);
            customTimeEditText.setEnabled(true);
            customPathEditText.setEnabled(true);
            customPathEditText.setOnClickListener(view -> pickSaveDirectory());
        });

        stopBtn.setEnabled(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (DEBUG)
                Log.d(TAG, "ReadExternalStoragePermission() not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE);
        } else {
            if (DEBUG)
                Log.d(TAG, "ReadExternalStoragePermission() granted");

            sensorLogger = new SensorLogger(getApplicationContext());
            customPathEditText.setText(sensorLogger.getPath());
        }
    }

    /**
     * values = xValue,yValue,zValue
     *
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (startTime == 0L)
            startTime = event.timestamp;

        String values = String.format("%s,%s,%s", event.values[0], event.values[1], event.values[2]);
        long timestamp = event.timestamp - startTime;

        try {
            sensorLogger.writeLog(event.sensor.getStringType(), timestamp, values);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        writeDebug("Accuracy of " + sensor.getStringType() + " changed.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_DIRECTORY_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    String pickedPath = data.getStringExtra(DirectoryPicker.BUNDLE_CHOSEN_DIRECTORY);
                    if (pickedPath != null && !pickedPath.isEmpty()) {
                        sensorLogger.setPath(pickedPath);
                        customPathEditText.setText(pickedPath);
                        writeDebug("New path selected.");
                    } else {
                        writeDebug("Path not picked.");
                    }
                }
                break;
            case REQUEST_EXTERNAL_STORAGE:
                if (resultCode == Activity.RESULT_OK) {
                    sensorLogger = new SensorLogger(getApplicationContext());
                    customPathEditText.setText(sensorLogger.getPath());
                } else {
                    writeDebug("Permission not provided. The application cannot work without permissions!");
                }
                break;
        }
    }


    private void pickSaveDirectory() {
        //Create the intent and start the activity
        Intent intent = new Intent(this, DirectoryPicker.class);
        startActivityForResult(intent, REQUEST_DIRECTORY_PICKER);
    }


    /**
     * Auxiliary method to parse correctly the custom time in input by the user
     *
     * @param inputTime #{String} version of the input
     * @return integer version of inputTime
     */
    private int getCustomTime(String inputTime) {
        // if there're some constraints they've to be checked here
        int result;
        try {
            result = Integer.parseInt(String.format("%s", inputTime));
        } catch (NumberFormatException e) {
            writeDebug("Error! Not a correct custom time selected! Using Fastest.");
            result = SensorManager.SENSOR_DELAY_FASTEST;
        }
        return result;
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
            Log.d(TAG, message);
    }

}
