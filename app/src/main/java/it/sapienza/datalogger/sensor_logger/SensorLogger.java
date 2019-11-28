package it.sapienza.datalogger.sensor_logger;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static it.sapienza.datalogger.utility.Utility.DEBUG;

public class SensorLogger {
    private final static String TAG = SensorLogger.class.getSimpleName();
    private String path;
    private File file;
    private FileWriter fileWriter;

    /**
     *
     */
    public SensorLogger() {

    }

    /**
     * @param path
     */
    public SensorLogger(String path) {

    }

    /**
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @param _context
     * @throws IOException
     */
    public void openLogger(Context _context) throws IOException {
        final String now = new SimpleDateFormat("MMMM_dd_yyyy_HH_mm_ss", Locale.ITALY).format(Calendar.getInstance(Locale.ITALY).getTime());
        if (path == null)
            file = new File(_context.getFilesDir(), now.concat(".txt"));
        else
            file = new File(this.path, now.concat(".txt"));

        boolean res = file.createNewFile();
        if (DEBUG)
            Log.d(TAG, "file created? " + res);

        fileWriter = new FileWriter(file);
    }


    /**
     * Official format
     * <p>
     * timestamp,sensorType,values
     *
     * @param sensorType
     * @param timestamp
     * @param values
     * @throws IOException
     */
    public void writeLog(String sensorType, long timestamp, String values) throws IOException {
        fileWriter.append(String.valueOf(timestamp)).append(",").append(sensorType).append(",").append(values).append("\n");
    }


    /**
     * @throws IOException
     */
    public void closeLogger() throws IOException {
        fileWriter.flush();
        fileWriter.close();
    }
}
