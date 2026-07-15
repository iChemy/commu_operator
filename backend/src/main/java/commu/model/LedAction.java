package commu.model;

import java.awt.Color;

public class LedAction implements RobotAction {
    private final int durationMs;
    private final Color bodyLed;
    private final Color powerButtonLed;
    private final int leftCheek;
    private final int rightCheek;

    public LedAction(int durationMs, Color bodyLed, Color powerButtonLed, int leftCheek, int rightCheek) {
        this.durationMs = durationMs;
        this.bodyLed = bodyLed;
        this.powerButtonLed = powerButtonLed;
        this.leftCheek = leftCheek;
        this.rightCheek = rightCheek;
    }

    @Override
    public String getType() {
        return "led";
    }

    public int getDurationMs() {
        return durationMs;
    }

    public Color getBodyLed() {
        return bodyLed;
    }

    public Color getPowerButtonLed() {
        return powerButtonLed;
    }

    public int getLeftCheek() {
        return leftCheek;
    }

    public int getRightCheek() {
        return rightCheek;
    }
}
