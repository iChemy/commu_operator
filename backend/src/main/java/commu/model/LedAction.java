package commu.model;

import java.awt.Color;

public class LedAction implements RobotAction {
    private static final Color OFF_COLOR = Color.BLACK;
    private static final int OFF_BRIGHTNESS = 0;

    private final int durationMs;
    private final Color bodyLed;
    private final Color powerButtonLed;
    private final int leftCheek;
    private final int rightCheek;

    public LedAction(int durationMs, Color bodyLed, Color powerButtonLed, int leftCheek, int rightCheek) {
        this.durationMs = durationMs;
        this.bodyLed = defaultColor(bodyLed);
        this.powerButtonLed = defaultColor(powerButtonLed);
        this.leftCheek = defaultBrightness(leftCheek);
        this.rightCheek = defaultBrightness(rightCheek);
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

    private Color defaultColor(Color color) {
        return color == null ? OFF_COLOR : color;
    }

    private int defaultBrightness(int brightness) {
        return brightness < 0 ? OFF_BRIGHTNESS : brightness;
    }
}
