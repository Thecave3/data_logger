package it.sapienza.datalogger.detector;

public enum DynamicSignal {
    Idle(0),
    Moving(1),
    Falling(2),
    WarmStop(3),
    ColdStop(4);

    private final int id;
    private DynamicSignal(int id) { this.id = id;}

    public int getId() {return this.id;}
}
