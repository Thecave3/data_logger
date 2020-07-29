package it.sapienza.datalogger.detector;

public interface SignalInterface {
    DynamicState onEventIdle();
    DynamicState onEventMoving();
    DynamicState onEventFalling();
    DynamicState onEventWarmStop();
    DynamicState onEventColdStop();
}
