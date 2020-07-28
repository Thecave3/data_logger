package it.sapienza.datalogger.detector;

import java.lang.Math;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Observable;
import android.util.Log;

public class Detector extends Observable {
    private static Detector instance;
    // Deque for recent reading buffering
    private Deque<Double> readingBuf;
    private DynamicSignal prevSignal; // used to avoid redundant notifies to Detector


    private ArrayList<SignalEvaluator> evaluators;
    private double[] confidenceTable;

    // Parameters
    private int windowSize = 20;
    double maxConfidence = 10;
    double decayRate = 1;

    // Evaluators-related parameters
    double absMeanWalkThresh = 9.5;
    double absMeanFallThresh = 15.0;
    double stddevWalkThresh = 1.0;
    double stddevFallThresh = 4.0;

    private Detector() {
        readingBuf = new ArrayDeque<Double>();
        confidenceTable = new double[DynamicSignal.values().length];

        // Install evaluators
        evaluators = new ArrayList<SignalEvaluator>();
        evaluators.add(new AbsoluteMeanEvaluator(absMeanWalkThresh, absMeanFallThresh));
        evaluators.add(new StandardDeviationEvaluator(stddevWalkThresh, stddevFallThresh));
    }

    public static synchronized Detector getInstance() {
        if (instance == null) {
            instance = new Detector();
        }
        return instance;
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
     *
     * @param acc_x
     * @param acc_y
     * @param acc_z
     */
    public void readSample(double acc_x, double acc_y, double acc_z) {
        double accNorm = Math.sqrt(Math.pow(acc_x, 2) + Math.pow(acc_y, 2) + Math.pow(acc_z, 2));
        // IF readingBuf is still not full, then skip evaluation phase
        if (this.readingBuf.size() < this.windowSize) {
            this.readingBuf.addLast(accNorm);
            return;
        }
         // When deque is full, remove first item before inserting the new one
         this.readingBuf.removeFirst();
        this.readingBuf.addLast(accNorm);
        DynamicSignal evalSignal = computeSignal();
        if (evalSignal != prevSignal) {
            setChanged();
            notifyObservers(evalSignal);
        }
        this.prevSignal = evalSignal;
    }

    private DynamicSignal computeSignal() {
        // Extract data from deque
        double[] acc = new double[this.windowSize];
        int idx = 0;
        for (double val : this.readingBuf) {
            acc[idx] = val;
            idx++;
        }
        // Apply decay rate
        for (int i = 0; i < DynamicSignal.values().length; ++i) {
            this.confidenceTable[i] = Math.max(
                    this.confidenceTable[i] - this.decayRate,
                    0.0);
        }
        // Query the estimators
        for (SignalEvaluator evaluator : this.evaluators) {
            if (evaluator.ready()) {
                DynamicSignal estimatedSignal = evaluator.predict(acc);
                double estimatedConfidence = evaluator.getConfidence();
                // Update confidenceTable
                confidenceTable[estimatedSignal.getId()] = Math.min(
                        this.confidenceTable[estimatedSignal.getId()] + estimatedConfidence,
                        this.maxConfidence);
            }
        }
        // Search and return highest confidence signal.
        int maxConfidenceIdx = argmax(this.confidenceTable);
        Log.d("detector", "Current output signal is: " + signalFromInteger(maxConfidenceIdx));
        return signalFromInteger(maxConfidenceIdx);
    }

    public void init() {

    }

}
