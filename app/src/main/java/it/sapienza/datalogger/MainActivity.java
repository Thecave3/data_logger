package it.sapienza.datalogger;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ThreadLocalRandom;

import it.sapienza.datalogger.detector.Detector;
import it.sapienza.datalogger.detector.DynamicSignal;
import it.sapienza.datalogger.detector.DynamicState;

import static it.sapienza.datalogger.utility.Utility.DEBUG;


public class MainActivity extends AppCompatActivity implements SensorEventListener, Observer {
    private static final int REQUEST_EXTERNAL_STORAGE = 0;
    private static final int REQUEST_SAVE_PATH = 1;
    private final static long FOG_DELTA_TIME_DEFAULT = 5000;

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static String CHANNEL_ID = "falling_channel";
    private final static long[] vibrationPattern = new long[]{0, 1500, 0, 1500};

    private int samplingRate;

    private SensorManager mSensorManager;

    private Sensor accelerometer;
    private Detector detector;

    private long startTime = 0L;

    private TextView debugger;
    private TextInputEditText customTimeEditText;
    private RadioGroup timeRadioGroup;
    private Button startBtn, stopBtn, aboutBtn;

    /*
    DFA related variables
     */
    private DynamicState state;
    private long fogDeltaStart = 0;
    private long fogDeltaTime = FOG_DELTA_TIME_DEFAULT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBtn = findViewById(R.id.buttonStart);
        stopBtn = findViewById(R.id.buttonStop);
        Button settingsBtn = findViewById(R.id.buttonSettings);

        debugger = findViewById(R.id.debugger);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        customTimeEditText = findViewById(R.id.custom_time_value);

        TextInputEditText fogDeltaTimeEditText = findViewById(R.id.fogDeltaTime);
        fogDeltaTimeEditText.setText(String.format(Locale.ITALY, "%d", fogDeltaTime));
        fogDeltaTimeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    fogDeltaTime = Long.parseLong(editable.toString());
                } catch (NumberFormatException e) {
                    writeDebug("Error! Not a correct fog delta time selected! Using default (" + FOG_DELTA_TIME_DEFAULT + ").");
                    fogDeltaTime = FOG_DELTA_TIME_DEFAULT;
                }

            }
        });

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
            samplingRate = timeRadioGroup.getCheckedRadioButtonId() == R.id.custom_time ? getCustomTime(String.valueOf(customTimeEditText.getText())) : samplingRate;
            mSensorManager.registerListener(listener, accelerometer, samplingRate);

            timeRadioGroup.setEnabled(false);
            startBtn.setEnabled(false);
            customTimeEditText.setEnabled(false);
            stopBtn.setEnabled(true);
            this.state = DynamicState.STEADY;
        });

        stopBtn.setOnClickListener(v -> {
            writeDebug("Data taking stopped.");
            mSensorManager.unregisterListener(listener);
            stopBtn.setEnabled(false);
            timeRadioGroup.setEnabled(true);
            startBtn.setEnabled(true);
            customTimeEditText.setEnabled(true);
        });
        stopBtn.setEnabled(false);

        settingsBtn.setOnClickListener(v -> {
            LinearLayout settingsLayout = findViewById(R.id.settings);
            if (settingsLayout.getVisibility() == View.INVISIBLE) {
                ((Button) v).setText(R.string.toggle_settings);
                settingsLayout.setVisibility(View.VISIBLE);
            } else {
                ((Button) v).setText(R.string.settings);
                settingsLayout.setVisibility(View.INVISIBLE);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (DEBUG)
                Log.d(TAG, "ReadExternalStoragePermission() not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE);
        }

        // Instantiate the state var
        this.state = DynamicState.STEADY;

        detector = Detector.getInstance();

        detector.addObserver(this);

        createNotificationChannel();

        TextInputEditText AMEWalkThresholdEditText = findViewById(R.id.ame_walk_threshold);
        AMEWalkThresholdEditText.setText(String.format(Locale.ITALY, "%f", detector.getAbsMeanWalkThresh()));
        AMEWalkThresholdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                double temp = detector.getAbsMeanWalkThresh();
                try {
                    detector.setAbsMeanWalkThresh(Double.parseDouble(editable.toString().replace(',', '.')));
                } catch (NumberFormatException e) {
                    writeDebug("Error! Not a correct value selected! Restoring previous (" + temp + ").");
                    detector.setAbsMeanWalkThresh(temp);
                }
            }
        });

        TextInputEditText AMEFallThresholdEditText = findViewById(R.id.ame_fall_threshold);
        AMEFallThresholdEditText.setText(String.format(Locale.ITALY, "%f", detector.getAbsMeanFallThresh()));
        AMEFallThresholdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                double temp = detector.getAbsMeanFallThresh();
                try {
                    detector.setAbsMeanFallThresh(Double.parseDouble(editable.toString().replace(',', '.')));
                } catch (NumberFormatException e) {
                    writeDebug("Error! Not a correct value selected! Restoring previous (" + temp + ").");
                    detector.setAbsMeanFallThresh(temp);
                }

            }
        });

        TextInputEditText STDWalkThresholdEditText = findViewById(R.id.std_walk_threshold);
        STDWalkThresholdEditText.setText(String.format(Locale.ITALY, "%f", detector.getStddevWalkThresh()));
        STDWalkThresholdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                double temp = detector.getStddevWalkThresh();
                try {
                    detector.setStddevWalkThresh(Double.parseDouble(editable.toString().replace(',', '.')));
                } catch (NumberFormatException e) {
                    writeDebug("Error! Not a correct value selected! Restoring previous (" + temp + ").");
                    detector.setStddevWalkThresh(temp);
                }
            }
        });

        TextInputEditText STDFallThresholdEditText = findViewById(R.id.std_fall_threshold);
        STDFallThresholdEditText.setText(String.format(Locale.ITALY, "%f", detector.getStddevFallThresh()));
        STDFallThresholdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                double temp = detector.getStddevFallThresh();
                try {
                    detector.setStddevFallThresh(Double.parseDouble(editable.toString().replace(',', '.')));
                } catch (NumberFormatException e) {
                    writeDebug("Error! Not a correct value selected! Restoring previous (" + temp + ").");
                    detector.setStddevFallThresh(temp);
                }
            }
        });

        aboutBtn = findViewById(R.id.buttonAbout);

        aboutBtn.setOnClickListener(v ->
                Toast.makeText(this, "Balance Care Application\n" +
                        "Release 1.2\n" +
                        "Teleskill Italia S.r.l. © 2020 - Tutti i diritti riservati",
                        Toast.LENGTH_LONG).show());
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

//        String values = String.format("%s,%s,%s", event.values[0], event.values[1], event.values[2]);
        long timestamp = event.timestamp - startTime;
        processData(timestamp, event.values[0], event.values[1], event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        writeDebug("Accuracy of " + sensor.getStringType() + " changed, new accuracy: \"" + accuracy + "\"");
    }


    /**
     * function of data processing
     *
     * @param timestamp time of the measure
     * @param xValue    value of x axis of accelerometer
     * @param yValue    value of y axis of accelerometer
     * @param zValue    value of z axis of accelerometer
     */
    private void processData(long timestamp, float xValue, float yValue, float zValue) {
        Detector.getInstance().readSample(xValue, yValue, zValue);
    }

    @Override
    public void update(Observable o, Object arg) {

        if (o instanceof Detector) {
            DynamicSignal inputSignal = (DynamicSignal) arg;
            DynamicState newState = this.state.transition(inputSignal);
//            if (newState == DynamicState.STEADY) {
//                writeDebug("I'm steady.");
//            }

            if (newState != this.state) {
                writeDebug("New state set: " + newState.toString());
                if (newState == DynamicState.FALLING || newState == DynamicState.PATTACK) {
                    raiseAlarm();
                    stopBtn.callOnClick();
                }
                if (newState == DynamicState.WALKING && this.state == DynamicState.STEADY) {
                    // Start FoG counter
                    this.fogDeltaStart = System.currentTimeMillis();
                }
                if (newState == DynamicState.STEADY && this.state == DynamicState.WALKING) {
                    // Check FoG counter
                    if (System.currentTimeMillis() - this.fogDeltaStart > this.fogDeltaTime) {
                        sendMessageAndVibration("Warning: Possible FoG!");
                        writeDebug("Warning: Possible FoG!");

                    }
                }
                this.state = newState;
            }
        }
    }


    /**
     * Raise the allarm
     */
    private void raiseAlarm() {
        runOnUiThread(() -> {
            sendNotification("Fall Detected!");
//            sendMessageAndVibration("Fall Detected!");
        });
    }

    private void sendMessageAndVibration(String message) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(1500);
            }
        }

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();

            new CountDownTimer(1500, 750) {
                @Override
                public final void onTick(final long millisUntilFinished) {
                }

                @Override
                public final void onFinish() {
                    r.stop();
                }
            }.start();


        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
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

    private void sendNotification(String message) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setVibrate(vibrationPattern)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle(message)
                .setContentText(message)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true)
                .setTimeoutAfter(5000);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(ThreadLocalRandom.current().nextInt(), builder.build());

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            channel.setVibrationPattern(vibrationPattern);
            channel.enableVibration(true);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
