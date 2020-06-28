package it.sapienza.datalogger.detector;

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

        if (sampleAvg > this.thresholdWalk) estimatedSig = DynamicSignal.Moving;
        if (sampleAvg > this.thresholdFall) estimatedSig = DynamicSignal.Falling;
        this.confidence = estimatedConf;
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
            output += d;
        }
        output /= data.length;
        return output;
    }
}
