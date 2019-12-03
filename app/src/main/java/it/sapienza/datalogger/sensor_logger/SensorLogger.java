package it.sapienza.datalogger.sensor_logger;

import android.content.Context;
import android.os.Environment;
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
     * @param _context
     */
    public SensorLogger(Context _context) {

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdFileDir = Environment.getExternalStorageDirectory();
            this.path = sdFileDir.getPath();
            Log.d(TAG, "SensorLogger: ");
        } else {
            File externalFilesDir = _context.getExternalFilesDir(null);
            if (externalFilesDir != null)
                this.path = externalFilesDir.getPath();
        }
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
     * @throws IOException
     */
    public void openLogger() throws IOException {
        final String now = new SimpleDateFormat("MMMM_dd_yyyy_HH_mm_ss", Locale.ITALY).format(Calendar.getInstance(Locale.ITALY).getTime());
        file = new File(this.path, now.concat(".txt"));

        boolean res = file.createNewFile();
        if (DEBUG)
            Log.d(TAG, "file " + now.concat("txt") + "created? " + res);

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
        if (DEBUG)
            Log.d(TAG, "writeLog: value written");
    }


    /**
     * @throws IOException
     */
    public void closeLogger() throws IOException {
        fileWriter.flush();
        fileWriter.close();
        if (DEBUG)
            Log.d(TAG, "Logger closed");
    }
}
