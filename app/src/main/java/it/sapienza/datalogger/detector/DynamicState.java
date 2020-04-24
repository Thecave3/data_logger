package it.sapienza.datalogger.detector;

public enum DynamicState {
    STEADY(), WALKING(), FALLING(), PATTACK();

    DynamicState idle;
    DynamicState moving;
    DynamicState falling;
    DynamicState warmStop;
    DynamicState coldStop;

    static {
        // STEADY transitions
        STEADY.idle = STEADY; STEADY.moving = WALKING; STEADY.falling = FALLING;
        STEADY.warmStop = STEADY; STEADY.coldStop = STEADY;
        // WALKING transitions
        WALKING.idle = WALKING; WALKING.moving = WALKING; WALKING.falling = FALLING;
        WALKING.warmStop = STEADY; WALKING.coldStop = PATTACK;
        // FALLING transitions
        FALLING.idle = FALLING; FALLING.moving = FALLING; FALLING.falling = FALLING;
        FALLING.warmStop = FALLING; FALLING.coldStop = FALLING;
        // PATTACK transitions
        PATTACK.idle = PATTACK; PATTACK.moving = PATTACK; PATTACK.falling = PATTACK;
        PATTACK.warmStop = PATTACK; PATTACK.coldStop = PATTACK;

    }
    DynamicState() {};

    public DynamicState transition(DynamicSignal s) {
        switch(s) {
            case Idle:
                return this.idle;
            case Moving:
                return this.moving;
            case Falling:
                return this.falling;
            case WarmStop:
                return this.warmStop;
            case ColdStop:
                return this.coldStop;
            default:
                return null;
        }
    }
}
