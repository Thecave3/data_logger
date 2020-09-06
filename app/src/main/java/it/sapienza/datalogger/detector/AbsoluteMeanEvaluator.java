package it.sapienza.datalogger.detector;

import android.util.Log;

public class AbsoluteMeanEvaluator extends SignalEvaluator{
    private boolean isReady;
    private double confidence;

    AbsoluteMeanEvaluator(double tWalk, double tFall) {
        super(tWalk,tFall);
        this.isReady = true;
        this.confidence = 0.0;
    }

    @Override
    DynamicSignal predict(double[] sample) {
        double sampleAvg = avg(sample);
        DynamicSignal estimatedSig;
        double estimatedConf;

        estimatedSig = DynamicSignal.Idle;
        estimatedConf = 1.0;

        if (sampleAvg > this.getThresholdWalk()) {
            estimatedSig = DynamicSignal.Moving;
            estimatedConf = 1.0;
        }
        if (sampleAvg > this.getThresholdFall()) {
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
}
