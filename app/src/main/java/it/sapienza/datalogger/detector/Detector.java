package it.sapienza.datalogger.detector;
import java.lang.Math;
import java.util.Deque;

public class Detector {
    private static Detector instance;
    // current state of the DFA
    private DynamicState state;
    // Deque for recent reading buffering
    private Deque<Double> reading_buf;
    // Deque maximum size (from 3 to n)
    private int reading_buf_size;

    private Detector() {
        this.state = DynamicState.Initial;
    }

    public static synchronized Detector getInstance() {
        if (instance == null) {
            instance = new Detector();
        }
        return instance;
    }

    private double[] differentiate(double[] data) {
        double[] output = new double[data.length-1];
        for(int i = 0; i < data.length-1; i++) {
            output[i] = data[i+1] - data[i];
        }
        return output;
    }

    /**
     * Analyze the data currently stored in reading_buff
     * and computes the most appropriate dynamic signal.
     * @return Dynamic Signal which represents the current state of the reading buffer.
     */
    private DynamicSignal computeSignal(/*TODO*/) {
        // acc array
        double[] acc = new double[this.reading_buf_size];
        int idx = 0;
        for(Double elem : this.reading_buf) {
            acc[idx] = elem;
            idx++;
        }
        // acc derivative
        double[] dacc = differentiate(acc);
        // dacc derivative
        double[] ddacc = differentiate(dacc);
        // At this point we've got acc, dacc, ddacc that we can use to
        // study the current state of the user.

        return DynamicSignal.Idle;
    }

    public int readSample(double acc_x, double acc_y, double acc_z) {
        // pre-process the new reading (as we only use the norm of the acceleration vector
        double accNorm = Math.sqrt(Math.pow(acc_x, 2) + Math.pow(acc_y, 2) + Math.pow(acc_z, 2));
        if(this.reading_buf.size() >= this.reading_buf_size){
            // when deque is full, remove first item before
            // inserting the new one. (maintain size n)
            this.reading_buf.removeFirst();
        }
        this.reading_buf.addLast(accNorm);
        // Evaluate signals only when enough samples are stored.
        DynamicSignal evalSignal = computeSignal();
        //TODO
        return 0;
    }

}
