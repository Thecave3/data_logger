package it.sapienza.datalogger.detector;

public abstract class SignalEvaluator {
    abstract DynamicSignal predict(double[] sample);
    //abstract double[] predict(double[] sample);
    abstract double getConfidence();
    abstract boolean ready();
}
