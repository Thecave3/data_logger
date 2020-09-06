package it.sapienza.datalogger.detector;

import android.util.Log;

public class StandardDeviationEvaluator extends SignalEvaluator {
    private boolean isReady;
    private double confidence;

    StandardDeviationEvaluator(double tWalk, double tFall) {
        super(tWalk,tFall);
        this.isReady = true;
        this.confidence = 0.0;
    }

    @Override
    DynamicSignal predict(double[] sample) {
        double sampleStddev = stddev(sample);
        DynamicSignal estimatedSig;
        double estimatedConf;

        estimatedSig = DynamicSignal.Idle;
        estimatedConf = 1.0;

        if (sampleStddev > this.getThresholdWalk()) estimatedSig = DynamicSignal.Moving;
        if (sampleStddev > this.getThresholdFall()) estimatedSig = DynamicSignal.Falling;
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
}
