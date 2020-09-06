package it.sapienza.datalogger.detector;

import android.util.Log;

public class AbsoluteMeanEvaluator extends SignalEvaluator{
    private boolean isReady;
    private double confidence;
    // parameters
    private double thresholdWalk;
    private double thresholdFall;

    AbsoluteMeanEvaluator(double tWalk, double tFall) {
        this.isReady = true;
        this.confidence = 0.0;
        this.thresholdWalk = tWalk;
        this.thresholdFall = tFall;
    }

    @Override
    DynamicSignal predict(double[] sample) {
        double sampleAvg = avg(sample);
        DynamicSignal estimatedSig;
        double estimatedConf;

        estimatedSig = DynamicSignal.Idle;
        estimatedConf = 1.0;

        if (sampleAvg > this.thresholdWalk) {
            estimatedSig = DynamicSignal.Moving;
            estimatedConf = 1.0;
        }
        if (sampleAvg > this.thresholdFall) {
            estimatedSig = DynamicSignal.Falling;
            estimatedConf = 15.0;
        }
        if (estimatedSig != DynamicSignal.Idle) estimatedConf = 1.0;
        this.confidence = estimatedConf;
        Log.d("AbsoluteMeanEvaluator", "Predicted: " + estimatedSig + " (" + this.confidence + ") : val = " + sampleAvg);
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

    private double avg(double[] data) {
        double output = 0.0f;
        for (Double d : data) {
            output += Math.abs(d);
        }
        output /= data.length;
        return output;
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
