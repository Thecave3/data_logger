package it.sapienza.datalogger.detector;

import java.lang.Math;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
    private int windowSize = 30;
    double maxConfidence = 15;
    double decayRate = 0.5;

    // Evaluators-related parameters
    //double absMeanWalkThresh = 9.5;
    double absMeanWalkThresh = 1.7;
    double absMeanFallThresh = 15.0;
    double stddevWalkThresh = 0.3;
    double stddevFallThresh = 4.0;

    private Detector() {
        readingBuf = new ArrayDeque<>();
        confidenceTable = new double[DynamicSignal.values().length];

        // Install evaluators
        evaluators = new ArrayList<>();
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
        for (int i = 0; i < DynamicSignal.values().length; ++i) {
            if (data[i] > maxVal) {
                maxVal = data[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private DynamicSignal signalFromInteger(int idx) {
        switch (idx) {
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
        //Log.d("detector", Arrays.toString(this.confidenceTable) + " maxConfidenceIdx: " + maxConfidenceIdx);
        //Log.d("detector", "Current output signal is: " + signalFromInteger(maxConfidenceIdx));
        return signalFromInteger(maxConfidenceIdx);
    }

    public double getAbsMeanWalkThresh() {
        return absMeanWalkThresh;
    }

    public void setAbsMeanWalkThresh(double absMeanWalkThresh) {
        this.absMeanWalkThresh = absMeanWalkThresh;
        for (SignalEvaluator evaluator : evaluators) {
            if (evaluator instanceof AbsoluteMeanEvaluator)
                evaluator.setThresholdWalk(absMeanWalkThresh);
        }
    }

    public double getAbsMeanFallThresh() {
        return absMeanFallThresh;
    }

    public void setAbsMeanFallThresh(double absMeanFallThresh) {
        this.absMeanFallThresh = absMeanFallThresh;
        for (SignalEvaluator evaluator : evaluators) {
            if (evaluator instanceof AbsoluteMeanEvaluator)
                evaluator.setThresholdFall(absMeanFallThresh);
        }
    }

    public double getStddevWalkThresh() {
        return stddevWalkThresh;
    }

    public void setStddevWalkThresh(double stddevWalkThresh) {
        this.stddevWalkThresh = stddevWalkThresh;
        for (SignalEvaluator evaluator : evaluators) {
            if (evaluator instanceof StandardDeviationEvaluator)
                evaluator.setThresholdWalk(stddevWalkThresh);
        }
    }

    public double getStddevFallThresh() {
        return stddevFallThresh;
    }

    public void setStddevFallThresh(double stddevFallThresh) {
        this.stddevFallThresh = stddevFallThresh;
        for (SignalEvaluator evaluator : evaluators) {
            if (evaluator instanceof StandardDeviationEvaluator)
                evaluator.setThresholdFall(stddevFallThresh);
        }
    }
}
