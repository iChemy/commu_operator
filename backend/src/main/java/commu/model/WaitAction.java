package commu.model;

public class WaitAction implements RobotAction {
    private final int durationMs;

    public WaitAction(int durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String getType() {
        return "wait";
    }

    public int getDurationMs() {
        return durationMs;
    }
}
