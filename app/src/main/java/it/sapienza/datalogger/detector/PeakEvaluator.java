package it.sapienza.datalogger.detector;

public class PeakEvaluator extends SignalEvaluator {
    private boolean isReady;
    private double confidence;
    // parameters
    private int lag;
    private double threshold;
    private double influence;

    PeakEvaluator(int lag, double threshold, double influence) {
        this.lag = lag;
        this.threshold = threshold;
        this.influence = influence;

        this.isReady = true;
        this.confidence = 0.0;
    }

    @Override
    double[] predict(double[] sample) {
        return new double[0];
    }

    @Override
    double getConfidence() {
        return this.confidence;
    }

    @Override
    boolean ready() {
        return this.isReady;
    }
}
