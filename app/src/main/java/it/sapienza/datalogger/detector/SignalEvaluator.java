package it.sapienza.datalogger.detector;

public abstract class SignalEvaluator {
    abstract DynamicSignal predict(double[] sample);
    //abstract double[] predict(double[] sample);
    abstract double getConfidence();
    abstract boolean ready();

    private double thresholdWalk;
    private double thresholdFall;

    public SignalEvaluator(double thresholdWalk, double thresholdFall) {
        this.thresholdWalk = thresholdWalk;
        this.thresholdFall = thresholdFall;
    }

    public double getThresholdWalk() {
        return thresholdWalk;
    }

    public void setThresholdWalk(double thresholdWalk) {
        this.thresholdWalk = thresholdWalk;
    }

    public double getThresholdFall() {
        return thresholdFall;
    }

    public void setThresholdFall(double thresholdFall) {
        this.thresholdFall = thresholdFall;
    }
}
