package commu.model;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PoseAction implements RobotAction {
    private static final Color OFF_COLOR = Color.BLACK;
    private static final int OFF_BRIGHTNESS = 0;

    private final int durationMs;
    private final Map<Byte, Short> pose;
    private final boolean hasLed;
    private final Color bodyLed;
    private final Color powerButtonLed;
    private final int leftCheek;
    private final int rightCheek;

    public PoseAction(
            int durationMs,
            Map<Byte, Short> pose,
            Color bodyLed,
            Color powerButtonLed,
            int leftCheek,
            int rightCheek) {
        this.durationMs = durationMs;
        this.pose = Collections.unmodifiableMap(new HashMap<>(pose));
        this.hasLed = bodyLed != null || powerButtonLed != null || leftCheek >= 0 || rightCheek >= 0;
        this.bodyLed = defaultColor(bodyLed);
        this.powerButtonLed = defaultColor(powerButtonLed);
        this.leftCheek = defaultBrightness(leftCheek);
        this.rightCheek = defaultBrightness(rightCheek);
    }

    @Override
    public String getType() {
        return "pose";
    }

    public int getDurationMs() {
        return durationMs;
    }

    public Map<Byte, Short> getPose() {
        return pose;
    }

    public boolean hasLed() {
        return hasLed;
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
