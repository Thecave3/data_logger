package it.sapienza.datalogger.sensor_logger;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SensorLogger {
    private final static String TAG = SensorLogger.class.getSimpleName();
    private String path;
    private File file;
    private FileWriter fileWriter;

    public SensorLogger(Context _context, String path) throws IOException {
        final String now = new SimpleDateFormat("MMMM_dd_yyyy_HH_mm_ss", Locale.ITALY).format(Calendar.getInstance(Locale.ITALY).getTime());
        file = new File(_context.getFilesDir(), now.concat(".txt"));

        boolean res = file.createNewFile();
        Log.d(TAG, "file created? " + res);

        fileWriter = new FileWriter(file);

        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void writeLog(long timestamp, String values) throws IOException {
        fileWriter.append(String.valueOf(timestamp)).append(",").append(values).append("\n");
    }


    public void closeLogger() throws IOException {
        fileWriter.flush();
        fileWriter.close();
    }
}
