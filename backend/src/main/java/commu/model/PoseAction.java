package commu.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PoseAction implements RobotAction {
    private final int durationMs;
    private final Map<Byte, Short> pose;

    public PoseAction(int durationMs, Map<Byte, Short> pose) {
        this.durationMs = durationMs;
        this.pose = Collections.unmodifiableMap(new HashMap<>(pose));
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
}
