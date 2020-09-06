package it.sapienza.datalogger.detector;

import android.util.Log;

public class StandardDeviationEvaluator extends SignalEvaluator {
    private boolean isReady;
    private double confidence;
    // parameters
    private double thresholdWalk;
    private double thresholdFall;

    StandardDeviationEvaluator(double tWalk, double tFall) {
        this.isReady = true;
        this.confidence = 0.0;
        this.thresholdWalk = tWalk;
        this.thresholdFall = tFall;
    }

    @Override
    DynamicSignal predict(double[] sample) {
        double sampleStddev = stddev(sample);
        DynamicSignal estimatedSig;
        double estimatedConf;

        estimatedSig = DynamicSignal.Idle;
        estimatedConf = 1.0;

        if (sampleStddev > this.thresholdWalk) estimatedSig = DynamicSignal.Moving;
        if (sampleStddev > this.thresholdFall) estimatedSig = DynamicSignal.Falling;
        if (estimatedSig != DynamicSignal.Idle) estimatedConf = 1.0;
        this.confidence = estimatedConf;
        Log.d("StandardDeviationEvaluator", "Predicted: " + estimatedSig + " (" + this.confidence + ") : val = " + sampleStddev);
        return estimatedSig;
    }

    @Override
    double getConfidence() {
        return this.confidence;
    }

    @Override
    boolean ready() {
        return this.isReady;
    }

    private double stddev(double[] data) {
        double sum = 0, sd = 0.0;
        int lenght = data.length;
        for (double n : data) {
            sum += n;
        }
        double mean = sum / lenght;

        for (double n : data) {
            sd += Math.pow(n - mean, 2);
        }

        return Math.sqrt(sd / lenght);
    }

    public double getThresholdWalk() {
        return thresholdWalk;
    }
    // TODO(Andrea): Rendere modificabile tramite GUI
    public void setThresholdWalk(double thresholdWalk) {
        this.thresholdWalk = thresholdWalk;
    }

    public double getThresholdFall() {
        return thresholdFall;
    }
    // TODO(Andrea): Rendere modificabile tramite GUI
    public void setThresholdFall(double thresholdFall) {
        this.thresholdFall = thresholdFall;
    }
}
