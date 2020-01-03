package it.sapienza.datalogger.sensor_logger;

import android.content.ContentResolver;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static it.sapienza.datalogger.utility.Utility.DEBUG;

public class SensorLogger {
    private final static String TAG = SensorLogger.class.getSimpleName();
    private DocumentFile saveDirectory;
    private OutputStream outputStream;

    /**
     * @param saveDirectory the save directory picked by the user
     */
    public SensorLogger(DocumentFile saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    /**
     * @return the path of the save directory
     */
    public String getPath() {
        return saveDirectory.getName();
    }

    /**
     * @throws IOException various reasons
     */
    public void openLogger(ContentResolver _contentResolver) throws IOException {
        final String now = new SimpleDateFormat("MMMM_dd_yyyy_HH_mm_ss", Locale.ITALY).format(Calendar.getInstance(Locale.ITALY).getTime());
        DocumentFile file = saveDirectory.createFile("text/plain", now.concat(".txt"));
        outputStream = _contentResolver.openOutputStream(file.getUri(), "wa");
    }


    /**
     * Official format:
     * <p>
     * timestamp,sensorType,values
     * </p>
     *
     * @param sensorType from what sensor data comes
     * @param timestamp  timestamp of the measure
     * @param values     actual value
     * @throws IOException various reasons
     */
    public void writeLog(String sensorType, long timestamp, String values) throws IOException {
        String input = timestamp + "," + sensorType + "," + values + "\n";
        outputStream.write(input.getBytes());
        if (DEBUG)
            Log.d(TAG, "writeLog: value written");
    }


    /**
     * @throws IOException various reason
     */
    public void closeLogger() throws IOException {
        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
        }
        if (DEBUG)
            Log.d(TAG, "Logger closed");
    }
}
