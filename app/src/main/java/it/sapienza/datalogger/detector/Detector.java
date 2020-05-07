package it.sapienza.datalogger.detector;
import android.util.Log;

import java.lang.Math;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Observable;

public class Detector extends Observable {
    private static Detector instance;
    private DynamicSignal prevSignal;
    // Deque for recent reading buffering
    private Deque<Double> readingBuf;
    // Deque maximum size (from 3 to n)
    private int readingBufSize;

    // Thresholds (parameters)
    private double daccThreshold;
    private double daccFallThreshold;

    // Confidence section
    private Deque<Integer> confidenceBuf;
    private int confidenceBufSize;

    private Detector() {
        readingBuf = new ArrayDeque<Double>();
        confidenceBuf = new ArrayDeque<Integer>();
    }

    public static synchronized Detector getInstance() {
        if (instance == null) {
            instance = new Detector();
        }
        return instance;
    }

    private double[] differentiate(double[] data) {
        double[] output = new double[data.length - 1];
        for (int i = 0; i < data.length - 1; i++) {
            output[i] = data[i + 1] - data[i];
        }
        return output;
    }

    private double avg(double[] data) {
        double output = 0.0f;
        for (Double d : data) {
            output += d;
        }
        output /= data.length;
        return output;
    }

    private int argmax(double[] data) {
        int maxIdx = 0;
        double maxVal = 0.0;
        for(int i = 0; i < DynamicSignal.values().length; ++i) {
            if (data[i] > maxVal) {
                maxVal = data[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private int argmax(int[] data) {
        int maxIdx = 0;
        int maxVal = 0;
        for(int i = 0; i < DynamicSignal.values().length; ++i) {
            if (data[i] > maxVal) {
                maxVal = data[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private int mostFrequent(Integer[] data) {
        // Assuming data only contains values ranging from 0 to 4
        int[] bucket = new int[DynamicSignal.values().length];
        for (Integer i : data) {
            bucket[i] += 1;
        }
        return argmax(bucket);
    }

    private DynamicSignal signalFromInteger(int idx) {
        switch(idx) {
        case 0:
            return DynamicSignal.Idle;
        case 1:
            return DynamicSignal.Moving;
        case 2:
            return DynamicSignal.Falling;
        case 3:
            return DynamicSignal.WarmStop;
        case 4:
            return DynamicSignal.ColdStop;
        default:
            return null;
        }
    }

    /**
     * Analyze the data currently stored in reading_buff
     * and computes the most appropriate dynamic signal.
     *
     * @return Dynamic Signal which represents the current state of the reading buffer.
     */
    private DynamicSignal computeSignal(/*TODO*/) {
        // acc array
        double[] acc = new double[this.readingBufSize];
        int idx = 0;
        for (Double elem : this.readingBuf) {
            acc[idx] = elem;
            idx++;
        }
        // acc derivative
        double[] dacc = differentiate(acc);
        // dacc derivative
        double[] ddacc = differentiate(dacc);
        // At this point we've got acc, dacc, ddacc that we can use to
        // study the current state of the user.

        double[] estimatedSignalProb = new double[DynamicSignal.values().length];

        // Use dacc to observe walking patterns. Can be computed by averaging
        // the dacc vector and comparing it with a threshold
        // TODO: Parametrize the confidence levels for each action
        double meanDacc = avg(dacc);
        if (meanDacc >= this.daccThreshold) {
            // Give large confidence to Moving signal
            estimatedSignalProb[DynamicSignal.Moving.getId()] += 1.0;
            // Small confidence to Falling signal
            estimatedSignalProb[DynamicSignal.Falling.getId()] += 0.5;
            // If meanDacc goes over daccFallThreshold, weight the falling signal
            if (meanDacc >= this.daccFallThreshold) {
                estimatedSignalProb[DynamicSignal.Falling.getId()] += 2.0;
            }
        } else {
            estimatedSignalProb[DynamicSignal.Idle.getId()] += 0.5;
        }
        // Confidence section
        int mlIdx = argmax(estimatedSignalProb); // Maximum-Likelihood index
        if(this.confidenceBuf.size() >= this.confidenceBufSize) {
            this.confidenceBuf.removeFirst();
        }
        this.confidenceBuf.addLast(mlIdx);

        // Extract the most probable (frequent) signal index from the confidenceBuf
        mlIdx = mostFrequent((Integer[])this.confidenceBuf.toArray(new Integer[this.confidenceBuf.size()]));
        return signalFromInteger(mlIdx);
    }

    public void readSample(double acc_x, double acc_y, double acc_z) {
        // pre-process the new reading (as we only use the norm of the acceleration vector
        double accNorm = Math.sqrt(Math.pow(acc_x, 2) + Math.pow(acc_y, 2) + Math.pow(acc_z, 2));
        if (this.readingBuf.size() >= this.readingBufSize) {
            // when deque is full, remove first item before
            // inserting the new one. (maintain size n)
            this.readingBuf.removeFirst();
        }
        this.readingBuf.addLast(accNorm);
        // Evaluate signals only when enough samples are stored.
        DynamicSignal evalSignal = computeSignal();
        if(evalSignal != prevSignal) {
            setChanged();
            notifyObservers(evalSignal);
        }
        this.prevSignal = evalSignal;
    }

    public void init(int bufSize, int confidenceBufSize, double daccThreshold, double daccFallThreshold) {
        if (bufSize > 0) {
            this.readingBufSize = bufSize;
        }
        if (confidenceBufSize > 0) {
            this.confidenceBufSize = confidenceBufSize;
        }
        this.daccThreshold = daccThreshold;
        this.daccFallThreshold = daccFallThreshold;
    }
}
